file_name = "all.log"

PATTERN1 = "OptimizationRequestStatisticsBundle"
outfile = "all.csv"

scenario = ['custom0', 'custom1', 'custom2', 'custom3', 'custom4']
cores_list = [1, 2, 3, 4, 5, 6]
atrs = ["created_plans",
        "explored_plans",
        "explored_dims",
        "set_collisions",
        "graph_collisions",
        "pruned_plans",
        "algorithm_name",
        "fileName",
        "total_threads",
        "elapsed_time",
        "method_time",
        "cost"]

if __name__ == '__main__':
    store = {}
    out_rows = {}
    for sc in scenario:
        store[sc] = {}
        out_rows[sc] = []
        for core in cores_list:
            store[sc][core] = {}
            for atr in atrs:
                store[sc][core][atr] = -1

    with open(file_name) as f:
        file = ''
        mode = ''
        cores = -1
        for line in f:
            if PATTERN1 in line:
                tokens = line[36:-2].split(" ")
                store[file][cores]["created_plans"] = int(tokens[0])
                store[file][cores]["explored_plans"] = int(tokens[1])
                store[file][cores]["explored_dims"] = int(tokens[2])
                store[file][cores]["set_collisions"] = int(tokens[3])
                store[file][cores]["graph_collisions"] = int(tokens[4])
                store[file][cores]["pruned_plans"] = int(tokens[5])
                store[file][cores]["algorithm_name"] = tokens[6]
                store[file][cores]["fileName"] = tokens[7]
                store[file][cores]["total_threads"] = int(tokens[8])
                store[file][cores]["elapsed_time"] = int(tokens[9])
                store[file][cores]["method_time"] = int(tokens[10])
                store[file][cores]["cost"] = int(tokens[11])

    print(store)

    for scenario in store:
        for mode in store[scenario]:
            for core in store[scenario][mode]:
                out_rows[scenario].append(store[scenario][core]["created_plans"])
                out_rows[scenario].append(store[scenario][core]["explored_plans"])
                out_rows[scenario].append(store[scenario][core]["explored_dims"])
                out_rows[scenario].append(store[scenario][core]["set_collisions"])
                out_rows[scenario].append(store[scenario][core]["graph_collisions"])
                out_rows[scenario].append(store[scenario][core]["pruned_plans"])
                out_rows[scenario].append(store[scenario][core]["algorithm_name"])
                out_rows[scenario].append(store[scenario][core]["fileName"])
                out_rows[scenario].append(store[scenario][core]["total_threads"])
                out_rows[scenario].append(store[scenario][core]["elapsed_time"])
                out_rows[scenario].append(store[scenario][core]["method_time"])
                out_rows[scenario].append(store[scenario][core]["cost"])


    headers = []
    for mode in modes:
        for core in cores_list:
            for atr in atrs:
                headers.append(str(mode) + '-' + str(core) + '-' + str(atr))

    with open(outfile, 'w+') as f:
        f.write('scenario' + ''.join(',' + str(x) for x in headers) + '\n')
        for row in out_rows:
            if sum(out_rows[row]) < 0:
                continue
            line_out = row + ''.join(',' + str(x) for x in out_rows[row]) + '\n'
            f.write(line_out)
