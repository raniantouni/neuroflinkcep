# HTTP API
#
#   /                           GET             check server status
#   /files                      GET             get list of files
#   /files/:file_name           GET             get file by file name
#   /files/exists/:file_name    GET             checks if file exists by file name
#   /files/:file_name           DELETE          delete file by file name
#   /files                      POST            upload file
import json
import logging as log
import os
from functools import wraps

from flask import Flask, Response, request, send_from_directory, jsonify

from client import WSClient

app = Flask(__name__)
file_root = os.getenv('FS_ROOT')
if not os.path.exists(file_root):
    raise RuntimeError('File directory not found.')

# WS client, use it to communicate with the Optimizer
wsclient = None

# Set to True to disable the optimizer connection. No notification will be sent to the optimizer. For debugging
# purposes. Default is True
IGNORE_WS = os.getenv('IGNORE_WS', True) == True


# Authorization methods
def check_auth(username, password):
    """Check authentication data"""
    return username == os.getenv('OPTIMIZER_USER') and password == os.getenv('OPTIMIZER_PASS')


def authenticate():
    """Sends a 401 response that enables basic auth"""
    return Response(json.dumps({'error': 'Authentication failure'}), 401,
                    {'WWW-Authenticate': 'Basic realm="Login Required"', 'ContentType': 'application/json'})


def requires_auth(f):
    """Handle authentication"""

    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not check_auth(auth.username, auth.password):
            return authenticate()
        return f(*args, **kwargs)

    return decorated


# API
@app.route("/")
@requires_auth
def get_server_status():
    """Returns a string if server is up and running"""
    return Response(json.dumps({'message': 'Server OK'}), 200, {'ContentType': 'application/json'})


@app.route("/files")
@requires_auth
def get_files_list():
    """Returns a list of files in the server"""
    try:
        files = os.listdir(file_root)
        if '.gitkeep' in files:
            files.remove('.gitkeep')
        return jsonify(files)
    except ValueError as e:
        return Response(json.dumps({'error': str(e)}), 500, {'ContentType': 'application/json'})


@app.route("/files/<name>", methods=["GET"])
@requires_auth
def get_file_by_file_name(name):
    """Check for file in resources directory and return it"""
    if name in os.listdir(file_root):
        return send_from_directory(directory=os.path.abspath(file_root), path=name)
    else:
        return Response(json.dumps({'error': name + " not found in the server"}), 404,
                        {'ContentType': 'application/json'})


@app.route("/files/exists/<name>", methods=["GET"])
@requires_auth
def check_if_file_by_file_name(name):
    """Check if file in resources directory and return if exists"""
    if name in os.listdir(file_root):
        return Response(json.dumps({"message": "true"}), 200, {'ContentType': 'application/json'})
    else:
        return Response(json.dumps({"message": "false"}), 200, {'ContentType': 'application/json'})


@app.route("/files/<name>", methods=["DELETE"])
@requires_auth
def remove_file_by_file_name(name):
    """Find file in resources directory and delete if"""
    if name in os.listdir(file_root):
        os.remove(file_root + "/" + name)
        if wsclient:  # Notify server about the new deletion
            wsclient.send_to_fs_topic(json.dumps({'file': name, 'status': 'deleted'}))
        return Response(json.dumps({'message': name + " deleted successfully"}), 200,
                        {'ContentType': 'application/json'})
    else:
        return Response(json.dumps({'error': name + " not found."}), 404, {'ContentType': 'application/json'})


@app.route("/files", methods=["POST"])
@requires_auth
def save_new_file():
    """Handle file uploading, save files in resources directory"""
    file = request.files["file"]

    if file:
        file_name = file.filename
        file.save(os.path.join(file_root, file_name))
        if wsclient:  # Notify server about the new upload
            wsclient.send_to_fs_topic(json.dumps({'file': file_name, 'status': 'uploaded'}))
        return Response(json.dumps({'message': file_name + " uploaded successfully"}), 200,
                        {'ContentType': 'application/json'})
    else:
        return Response(json.dumps({'message': 'Invalid type'}), 400, {'ContentType': 'application/json'})


# Run flask server
if __name__ == '__main__':
    # Logger
    log.basicConfig(filename=f"{os.getenv('FS_LOG_DIR')}/fs.log",
                    format='%(levelname)s %(asctime)s %(message)s',
                    filemode='a',
                    level=log.DEBUG,
                    datefmt='%Y-%m-%d,%H:%M:%S')
    log.info('***START***')
    log.info('IGNORE_WS: {}'.format(IGNORE_WS))

    # WSClient
    if not IGNORE_WS:
        try:
            wsclient = WSClient(client_id='fs', queue_size=100, logger=log)
            wsclient.start()
        except:
            raise RuntimeError('Failed to initialize the STOMP websocket. Is the optimizer service running?')

    # Use the same IP as the Optimizer service, after all docker containers are always connected.
    app.run(host="0.0.0.0", port=20003, threaded=True, debug=True)
