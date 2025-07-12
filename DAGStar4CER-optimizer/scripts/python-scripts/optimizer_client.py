import json
import os

import requests

# Setup URL and credentials
OPTIMIZER_URL = "http://{0}".format(os.environ['OPTIMIZER_SERVER_URL'])
CREDENTIALS = ('infore_user', 'infore_pass')
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "*/*"
}

# Resource paths
dictionary_path = '../../input/topologies/topology1/dictionary.json'
network_path = '../../input/topologies/topology1/network.json'
workflow_path = '../../input/workflows/custom1.json'
request_path = '../../input/old/request.json'


def get_file_contents(file):
    with open(file) as f:
        return json.load(f)


if __name__ == '__main__':
    # Session open
    s = requests.Session()
    s.auth = CREDENTIALS
    s.headers.update(HEADERS)

    # Load JSON contents
    dictionary_data = get_file_contents(dictionary_path)
    network_data = get_file_contents(network_path)
    workflow_data = get_file_contents(workflow_path)
    request_data = get_file_contents(request_path)

    # PUT the resources in the optimizer
    dictionary_resp = s.put(OPTIMIZER_URL + '/resource/dictionary', json=dictionary_data)
    if dictionary_resp.status_code != 201:
        print('Dictionary error [{0}]'.format(dictionary_resp.status_code))
        print(dictionary_resp.content)
        exit(1)
    else:
        print('Dictionary uploaded.')

    network_resp = s.put(OPTIMIZER_URL + '/resource/network', json=network_data)
    if network_resp.status_code != 201:
        print('Network error [{0}]'.format(network_resp.status_code))
        print(network_resp.content)
        exit(1)
    else:
        print('Network uploaded.')

    workflow_resp = s.put(OPTIMIZER_URL + '/resource/workflow', json=workflow_data)
    if workflow_resp.status_code != 201:
        print('Workflow error [{0}]'.format(workflow_resp.status_code))
        print(workflow_resp.content)
        exit(1)
    else:
        print('Workflow uploaded.')
