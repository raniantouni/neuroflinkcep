import json
import logging as log
import os
from pathlib import Path

from flask import Flask, request, Response
from skopt import dump, load

from BayesianOptimization import set_cep_space
from model_operations import build_model


def DictionaryManager(json_external_input):
    objectives = ["staticCost", "migrationCosts"]
    models = '/tmp/models'
    dictionary = json_external_input['dictionaryName']

    for operator in json_external_input['operators']:
        classKey = operator['classKey']

        platforms = []
        for platform in operator['platforms']:
            platforms.append(platform)

        Folder_path = (models + classKey + "_" + dictionary).replace(":", ".")
        Path(Folder_path).mkdir(parents=True, exist_ok=True)

        for objective in objectives:
            model_path = (classKey + "_" + dictionary + "_" + objective + ".pkl").replace(":", ".")
            my_file = Path(Folder_path + "/" + model_path)
            if my_file.is_file():
                model = load(my_file)
            else:
                model = build_model(set_cep_space())
                dump(model, Folder_path + "/" + model_path)

            # SITES
            sites = []
            for site in operator['sites']:
                sites.append(site)
            for site_1 in sites:
                operator['sites'][site]['staticCost'] = 0
                for site in sites:
                    operator['sites'][site_1]['migrationCosts'][site] = 0

    return json_external_input


def is_json(content):
    try:
        json.loads(content)
        return True
    except BaseException:
        return False


if __name__ == '__main__':
    """
    Module entrypoint. 
    """

    # Setup logger
    log.basicConfig(filename=f"{os.getenv('BO_LOG_DIR')}/bo.log",
                    format='%(levelname)s %(asctime)s %(message)s',
                    filemode='a',
                    level=log.INFO,
                    datefmt='%Y-%m-%d,%H:%M:%S')
    log.info('***START***')

    # Setup ELK stack connections (two clients are provided, chose one that works)
    # es = Elasticsearch([os.getenv('ES_URL')], maxsize=4, http_auth=(os.getenv('ES_USERNAME'), os.getenv('ES_PASSWORD')))
    # es = Elasticsearch(['http://{0}:{1}@{2}'.format(os.getenv('ES_USERNAME'), os.getenv('ES_PASSWORD'), os.getenv('ES_URL'))], maxsize=4)

    # res = es.index(index="test-index", id=1, timeout=1, body={
    #     'author': 'kimchy',
    #     'text': 'Elasticsearch: cool. bonsai cool.',
    #     'timestamp': datetime.now(),
    # })
    # print(res['result'])
    #
    # res = es.get(index="test-index", id=1)
    # print(res['_source'])

    log.info('Setup completed, waiting for messages.')
    app = Flask(__name__)


    @app.route("/dictionary", methods=["POST"])
    def update_dictionary():
        log.debug('Processing: {}'.format(str(request)))

        # Parse to JSON
        input_json = json.loads(request.form['dictionary'])
        log.info('Received JSON:  {}'.format(input_json))

        # Transform JSON
        output_json = DictionaryManager(input_json)
        log.info('Transformed JSON: {}'.format(output_json))
        return Response(json.dumps(output_json), 200, {'ContentType': 'application/json'})


    @app.route("/dictionary", methods=["GET"])
    def health_check():
        return Response("{}", 200, {'ContentType': 'application/json'})


    # Use the same IP as the Optimizer service, after all docker containers are always connected.
    app.run(host="0.0.0.0", port=20004, threaded=True, debug=True)
