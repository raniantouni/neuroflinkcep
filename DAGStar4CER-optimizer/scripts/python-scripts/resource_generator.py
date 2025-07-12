import simplejson as json
import sys


all_operators = ['streaming:aggregate', 'streaming:connect', 'streaming:duplicate', 'streaming:filter',
                 'streaming:join', 'streaming:kafka_sink', 'streaming:kafka_source', 'streaming:map',
                 'streaming:sde', 'multiply', 'retrieve']

# Coefficients are [c,log,n,n^2,...,n^k]
cost_coefficients = {
    'streaming:aggregate': [0, 1, 0, 0],
    'streaming:connect': [0, 0, 1, 0],
    'streaming:duplicate': [0, 0, 0, 1],
    'streaming:filter': [0, 0, 1, 0],
    'streaming:join': [0, 0, 0, 1],
    'streaming:kafka_sink': [0, 0, 1, 0],
    'streaming:kafka_source': [0, 0, 1, 0],
    'streaming:map': [0, 0, 1, 0],
    'multiply': [0, 0, 1, 0],
    'retrieve': [0, 0, 1, 0],
    'streaming:sde': [0, 0, 1, 0],
}

site_static_cost_counter = 0
site_static_cost_switcher = {
    0: 80,
    1: 90,
    2: 100,
    3: 110,
    4: 120
}

platform_static_cost_counter = 0
platform_static_cost_switcher = {
    0: 5,
    1: 10,
    2: 15,
    3: 20,
    4: 25
}


def get_operator_input_rate(operator):
    return 100


def get_platform_static_cost(platform):
    global platform_static_cost_counter
    platform_static_cost_counter += 1
    platform_static_cost_counter %= len(platform_static_cost_switcher)
    return platform_static_cost_switcher[platform_static_cost_counter]


def get_platform_migration_cost(current, next, total):
    if current == next:
        return 0
    elif abs(current - next) <= total * 0.5:
        return int((abs(current - next) / total) * 1000)
    else:
        return 1000


def get_site_static_cost(site):
    global site_static_cost_counter
    site_static_cost_counter += 1
    site_static_cost_counter %= len(site_static_cost_switcher)
    return site_static_cost_switcher[site_static_cost_counter]


def get_site_migration_cost(current, next, total):
    if current == next:
        return 0
    elif abs(current - next) <= total * 0.3:
        return int((abs(current - next) / total) * 1000)
    else:
        return 1000


if __name__ == '__main__':
    sites = int(sys.argv[1])
    platforms = int(sys.argv[2])
    unique_platforms = True if sys.argv[3].lower() == 'true' else False
    print('Using {} sites and {} platforms per site.'.format(sites, platforms))
    content_root = sys.argv[4]
    out_network_name = content_root + '/network.json'
    out_dictionary_name = content_root + '/dictionary.json'

    if unique_platforms:
        print('Ignoring number of platforms and placing 1 unique platform on each site')

    # Generate sites/platforms
    all_sites = {}
    for i in range(sites):
        all_sites[i] = 'site_' + str(i)

    all_platforms = {}
    if unique_platforms:
        for i in all_sites:
            all_platforms[i] = 'platform_' + str(i)
    else:
        for i in range(platforms):
            all_platforms[i] = 'platform_' + str(i)

    # Build dictionary
    operator_array = []
    for operator in all_operators:
        operator_entry = {}

        # Add class key
        operator_entry['classKey'] = operator

        # Add the cost coefficients
        operator_entry['costCoefficients'] = cost_coefficients[operator]

        # Operator input rate (could be an API call in the future)
        operator_entry['inputRate'] = get_operator_input_rate(operator)

        # Add all platforms and costs
        platform_entries = {}
        for platform_id in sorted(all_platforms):
            platform_name = all_platforms[platform_id]
            platform_entry = {}
            platform_entry['operatorName'] = platform_name + '_' + operator
            platform_entry['staticCost'] = get_platform_static_cost(platform_name)
            migration_entries = {}
            for migration_platform_id in sorted(all_platforms):
                migration_platform_name = all_platforms[migration_platform_id]
                migration_entries[migration_platform_name] = get_platform_migration_cost(platform_id,
                                                                                         migration_platform_id,
                                                                                         platforms)
            platform_entry['migrationCosts'] = migration_entries
            platform_entries[platform_name] = platform_entry
        operator_entry['platforms'] = platform_entries

        # Add site costs
        site_entries = {}
        for site_id in sorted(all_sites):
            site_name = all_sites[site_id]
            site_entry = {}
            site_entry['staticCost'] = get_site_static_cost(site_name)
            migration_entries = {}
            for migration_site_id in sorted(all_sites):
                migration_site_name = all_sites[migration_site_id]
                migration_entries[migration_site_name] = get_site_migration_cost(site_id, migration_site_id, sites)

            site_entry['migrationCosts'] = migration_entries
            site_entries[site_name] = site_entry
        operator_entry['sites'] = site_entries

        operator_array.append(operator_entry)

    with open(out_dictionary_name, 'w') as f:
        f.write(json.dumps({'dictionaryName': 'dict_{}_{}'.format(sites, platforms), 'operators': operator_array}))

    # Build network
    out_network_contents = {'network': 'network_{}_{}'.format(sites, 1 if unique_platforms else platforms)}
    site_array = []
    for site_id in sorted(all_sites):
        site_name = all_sites[site_id]
        site_entry = {'siteName': site_name}
        avail_platform_array = []
        if unique_platforms:
            avail_platform_array.append({'platformName': str(all_platforms[site_id])})
        else:
            for platform_id in sorted(all_platforms):
                platform_name = all_platforms[platform_id]
                platform_entry = {'platformName': platform_name}
                # More details can be added here
                avail_platform_array.append(platform_entry)
        site_entry['availablePlatforms'] = avail_platform_array
        site_array.append(site_entry)
    out_network_contents['sites'] = site_array

    with open(out_network_name, 'w') as f:
        f.write(json.dumps(out_network_contents))
