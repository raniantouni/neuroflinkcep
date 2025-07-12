import matplotlib.pyplot as plt
import numpy as np
import glob
import csv
import random
import sobol_seq as sb


def send_to_elastic(es, e, operator_name, identifier):
    data_body = {
        "JobMainClass": operator_name,
        "stream_type_1": e[0],
        "stream_type_2": e[1],
        "stream_out_type": e[2],
        "usage": "estimated",
        "parallelism": e[3],
        "sourceRate1": e[4],
        "sourceRate2": e[5],
        "throughput": str(abs(int(e[6])))
    }
    es.index(index='bo-estimations_2', doc_type='_doc', id=identifier, body=data_body)


def update_model_elastic(es, model_name, l1_sum, r2_score):
    es.update(index='bo-models', id=model_name,
              body={"doc": {"Coefficient of determination": r2_score}})
    es.update(index='bo-models', id=model_name,
              body={"doc": {"AVG_L1": l1_sum}})


def send_to_model_elastic(es, model_name, l1_sum, r2_score):
    data_body = {
        "Model Name": model_name,
        "AVG_L1": l1_sum,
        "coeffici "
        "ent of determination": r2_score
    }
    es.index(index='bo-models', doc_type='_doc', id=model_name, body=data_body)


def get_data_from_cs(PATH, output_col, data):
    objective_maximize = True
    flag = False
    for files in glob.glob(PATH + "*.csv"):
        print(files)
        with open(files, 'r') as file:
            reader = csv.reader(file, delimiter=',')
            for row in reader:
                if flag and len(row) != 0:
                    data.append(row[1:len(row)])
                    print(row)
                else:
                    flag = True

    if objective_maximize:
        for row in data:
            row[output_col - 1] = str(-float(row[output_col - 1]))

    print("data dimensions=", np.shape(data))


def get_cep_data_from_cs(PATH, output_col, data):
    objective_maximize = True
    flag = False
    for files in glob.glob(PATH + "*.csv"):
        print(files)
        with open(files, 'r') as file:
            reader = csv.reader(file, delimiter=',')
            for row in reader:
                if flag and len(row) != 0:
                    data.append(row[0:len(row)])
                    print(row)
                else:
                    flag = True

    if objective_maximize:
        for row in data:
            row[output_col - 1] = str(-float(row[output_col - 1]))

    print("data dimensions=", np.shape(data))


def get_data_from_elastic(es, operator, data_state, output_col, size, usage):
    query_body = {
        "query": {
            "bool": {
                "must": [
                    {
                        "match": {
                            "usage": data_state
                        }
                    },
                    {
                        "match": {
                            "JobMainClass": operator
                        }
                    }
                ]
            }
        }
    }

    res = es.search(index="bo-estimations_2", body=query_body, size=size)
    data = []
    for hit in res['hits']['hits']:
        if data_state != "training":
            print("update")
            print(hit)
            es.update(index='bo-estimations_2', id=hit['_id'], body={"doc": {"usage": usage}})
        del hit['_source']["usage"]
        del hit['_source']["JobMainClass"]
        temp = list(hit['_source'].values())
        temp[output_col - 1] = str(-int(temp[output_col - 1]))

        data.append(temp)

    return data


def data_vs_parallelism_plot(PATH, output_col, data):
    data2 = []
    objective_maximize = True
    for row in data:
        if row[0] == "CustomSource" and row[1] == "CustomSource" and int(row[2]) == 1000000 and int(
                row[3]) == 1000000 and \
                row[4] == "CustomSource":
            if objective_maximize:
                data2.append([int(row[5]), -float(row[output_col - 1])])
            else:
                data2.append([int(row[5]), float(row[output_col - 1])])

    data2.sort(key=lambda x: int(x[0]))
    parallelism = [exp[0] for exp in data2]
    throughput = [exp[1] for exp in data2]
    print("Parallelism=", parallelism)
    print("Throughput=", throughput)

    plt.plot(parallelism, throughput, "b--", label="throughput/parallelism")
    plt.legend()
    plt.grid()
    plt.show()


# # 2) Set space for Bayesian Optimization

# In[3]:


from skopt.space import Integer, Categorical


# Remember the feature vector:
# feature 1(csv column 2): Stream 1 source type, source1 = "KafkaTopic", "CustomSource"
# feature 2(csv column 3): Stream 2 source type, source2 = "KafkaTopic", "CustomSource"
# feature 3(csv column 4): Stream 1 rate(t/s), source1Rate = 1000, 10000, 100000, 1000000
# feature 4(csv column 5): Stream 2 rate(t/s), source2Rate = 1000, 10000, 100000, 1000000
# feature 5(csv column 6): Stream output type, outputSource = "KafkaTopic", "CustomSource"
# feature 6(csv column 7): Job Parallelism = 1, 4, 8, 12

def set_general_space():
    SPACE_CAT = [
        Categorical(['KafkaTopic', 'CustomSource'], name='source1'),
        Categorical(['KafkaTopic', 'CustomSource'], name='source2'),
        Categorical(['1000', '10000', '100000', '1000000', ], name='source1Rate'),
        Categorical(['1000', '10000', '100000', '1000000', ], name='source2Rate'),
        #         Categorical(['KafkaTopic', 'CustomSource'], name='outputSource'),
        Categorical(['CustomSource'], name='outputSource'),
        Categorical(['1', '4', '8', '12'], name='parallelism')
    ]

    return SPACE_CAT


def set_cep_space():
    SPACE_CAT = [
        Categorical(['1', '2', '3'], name='Contiquity'),
        Categorical(['1', '2', '3'], name='Consumption'),
        Categorical(['1', '2', '4', '8'], name='parallelism')
    ]

    return SPACE_CAT


# convert list data to python dictionary with composite key=tuple(features)
def initialize_data_map(data, output_col):
    data_map = {}
    for row in data:
        data_map[tuple(row[0:3])] = float(row[output_col - 1])

    return data_map


def choose_x_random_experiments(x, data_map, opt_seed, seed):
    if opt_seed == True:
        random.seed(seed)
    else:
        random.seed(None)

    exp_index = random.sample(range(0, len(data_map)), x - 1)
    # print("total experiments = ", len(data_map))
    # print("number of chosen experiments = ", len(exp_index))

    exp_features = []
    for index in exp_index:
        exp_features.append(list(list(data_map)[index]))

    exp_values = []
    for key in exp_features:
        exp_values.append(data_map.pop(tuple(key)))

    return [exp_features, exp_values]


def choose_x_random_experiments_2(x, data_map, opt_seed, seed):
    res = {}
    if opt_seed == True:
        random.seed(seed)
    else:
        random.seed(None)

    exp_index = random.sample(range(0, len(data_map)), x)
    # print("total experiments = ", len(data_map))
    # print("number of chosen experiments = ", len(exp_index))

    exp_features = []
    for index in exp_index:
        key = (list(data_map)[index])
        tmp = {key: data_map.pop(tuple(key))}
        res.update(tmp)

    return dict(res)


def generate_sobol_sequence(x, data_map):
    exp_features = []
    sob_seq_array = sb.i4_sobol_generate(5, x)
    print("sob_seq_array=", sob_seq_array, "\n")

    for sob_seq in sob_seq_array:

        feature = []
        if sob_seq[0] <= 0.5:
            source1_type = 'KafkaTopic'
        else:
            source1_type = 'CustomSource'
        feature.append(source1_type)

        if sob_seq[1] <= 0.5:
            source2_type = 'KafkaTopic'
        else:
            source2_type = 'CustomSource'
        feature.append(source2_type)

        if sob_seq[2] <= 0.25:
            source1_rate = '1000'
        elif sob_seq[2] <= 0.5:
            source1_rate = '10000'
        elif sob_seq[2] <= 0.75:
            source1_rate = '100000'
        else:
            source1_rate = '1000000'
        feature.append(source1_rate)

        if sob_seq[3] <= 0.25:
            source2_rate = '1000'
        elif sob_seq[3] <= 0.5:
            source2_rate = '10000'
        elif sob_seq[3] <= 0.75:
            source2_rate = '100000'
        else:
            source2_rate = '1000000'
        feature.append(source2_rate)

        output_type = 'CustomSource'
        feature.append(output_type)

        if sob_seq[4] <= 0.25:
            parallelism = '1'
        elif sob_seq[4] <= 0.5:
            parallelism = '4'
        elif sob_seq[4] <= 0.75:
            parallelism = '8'
        else:
            parallelism = '12'

        feature.append(parallelism)
        exp_features.append(feature)

    exp_values = []
    for key in exp_features:
        exp_values.append(data_map.pop(tuple(key)))

    print("exp_features=", exp_features, "\n")
    print("exp_values=", exp_values, "\n")

    return [exp_features, exp_values]


def update_model(model, init_data_map, data_map, initial_fit, model_res_init):
    exp_discovered = False

    # update model
    if initial_fit == False:
        model_features = model.ask()

        features_key = tuple(model_features)
        print("feature used for update ", features_key)
        throughput = init_data_map.get(tuple(features_key))
        model_res = model.tell(model_features, throughput, fit=True)

        if features_key in init_data_map:
            init_data_map.pop(features_key)
            exp_discovered = True
    if initial_fit == True:
        exp_discovered = True
        model_res = model_res_init

    if exp_discovered == True:
        # calculate metrics
        l1_sum = 0
        y_true_list = []
        y_pred_list = []

        gp = model_res.models[-1]
        print("evaluation on data")
        for index in range(len(init_data_map)):
            feature = list(list(init_data_map)[index])

            feature_gp = model.space.transform([feature])

            y_pred, sigma = gp.predict(feature_gp, return_std=True)
            y_true = list(init_data_map.values())[index]
            # calculate l1 distance
            l1 = np.abs(y_pred[0] - y_true)
            l1_sum += l1
            # calculate R2 score
            y_true_list.append(y_true)
            y_pred_list.append(y_pred[0])

        y_true_list = np.array(y_true_list).reshape(-1, 1)
        y_pred_list = np.array(y_pred_list).reshape(-1, 1)
        u = ((y_true_list - y_pred_list) ** 2).sum()
        v = ((y_true_list - y_pred_list.mean()) ** 2).sum()
        r2_score = (1 - u / v)
        return [l1_sum, model_res, r2_score]
    else:
        return [None, model_res, None]


# ##### The following script calls Bayesian Optimization for a fixed number of iterations.

# In[9]:

###########################
###########################

def CallBo(model, model_res, data_map, init_data_map, iterations, initial_fit):
    exp_iter = []
    exp_iter_l1 = []
    exp_iter_r2 = []
    model_res_fun = []
    total_calls = 0
    objective_maximize = 'true';
    number_of_discovered_exp = 0
    for index in range(iterations):
        total_calls += 1
        ##print("loop=", index)
        l1_sum, model_res, r2_score = update_model(model, init_data_map, data_map, initial_fit, model_res)
        if l1_sum is not None and r2_score is not None:
            number_of_discovered_exp += 1
            ##print("number_of_discovered_exp=", number_of_discovered_exp, "\n")
            exp_iter.append(number_of_discovered_exp)
            exp_iter_l1.append(l1_sum)
            exp_iter_r2.append(r2_score)
            if objective_maximize:
                model_res_fun.append(-model_res.fun)
            else:
                model_res_fun.append(model_res.fun)

    print("Size of space=", len(init_data_map), "\n")
    print("Number of chosen experiments=", len(init_data_map) - len(data_map), "\n")
    print("Number of unchosen experiments=", len(data_map), "\n")
    print("Total iterations of Bayesian Optimization=", total_calls, "\n")
    print("r2_score=", r2_score, "\n")
    print("l1_sum score=", l1_sum, "\n")
    return [l1_sum, model_res, r2_score]


# # 6) Print execution metrics and figures

# In[10]:


from skopt.plots import plot_convergence


def PrintResult(data_map, total_calls, acq_func, initial_exp, exp_iter_l1, exp_iter_r2, model_res, data, exp_iter,
                acq_func_ei_xi, acq_func_lcb_kappa, rand_state, model_res_fun, model):
    global init_data_map
    print("Size of space=", len(init_data_map), "\n")
    print("Number of chosen experiments=", len(init_data_map) - len(data_map), "\n")
    print("Number of unchosen experiments=", len(data_map), "\n")
    print("Total iterations of Bayesian Optimization=", total_calls, "\n")
    # useful prints
    # print("exp_iter=",exp_iter,"len=",len(exp_iter),"\n")
    # print("exp_iter_l1=",exp_iter_l1,"len=",len(exp_iter_l1),"\n")
    # print("exp_iter_r2=",exp_iter_r2,"len=",len(exp_iter_r2),"\n")
    # print("model_res_fun=",model_res_fun,"len=",len(model_res_fun),"\n")
    # print("len exp_iter=",len(exp_iter),"\n")
    # print("len exp_iter_l1=",len(exp_iter_l1),"\n")
    # print("len exp_iter_r2=",len(exp_iter_r2),"\n")
    # print("len model_res_fun=",len(model_res_fun),"\n")
    objective_maximize = 'true';
    if objective_maximize == True:
        print("Best maximum found = ", -model_res.fun, " at location x=", model_res.x, "\n")
    else:
        print("Best minimum found = ", model_res.fun, " at location x=", model_res.x, "\n")
    exp_iter_percentage = [exp_iter_i * 100 / len(data) for exp_iter_i in exp_iter]
    # print("exp_iter_percentage=",exp_iter_percentage,"len=",len(exp_iter_percentage),"\n")
    # print("exp_iter_percentage=",len(exp_iter_percentage),"\n")
    print("Percentage of explored space=", exp_iter_percentage[len(exp_iter_percentage) - 1], "%\n")
    fig = plt.figure()
    fig.set_figheight(12)
    fig.set_figwidth(14)
    if acq_func == "EI":
        acq_func_param_type = " ,xi="
        acq_func_param_value = acq_func_ei_xi
    elif acq_func == "LCB":
        acq_func_param_type = " ,kappa="
        acq_func_param_value = acq_func_lcb_kappa
    elif acq_func == "PI":
        acq_func_param_type = " ,xi="
        acq_func_param_value = acq_func_ei_xi
    top_title = "Bayesian Optimization\nacq_func=" + acq_func + acq_func_param_type + str(
        acq_func_param_value) + " ,BO_iterations=" + str(total_calls) + " ,rand_state=" + str(
        rand_state) + "\n,initial_fit_exp=" + str(initial_exp) + ",size_of_space=" + str(
        len(init_data_map)) + " ,chosen_exp=" + str((len(init_data_map) - len(data_map))) + " ,unchosen_exp=" + str(
        len(data_map))
    fig.suptitle(top_title, fontsize=16)
    plt.subplot(2, 1, 1)
    plt.plot(exp_iter_percentage, exp_iter_l1, "r--", label="L1-distance")
    scale_r2_score = [r * 10000000 for r in exp_iter_r2]
    plt.plot(exp_iter_percentage, scale_r2_score, "b--", label="R2-score")
    plt.ylim(0.0, 60000000)
    plt.xlim(0, 101)
    plt.legend(loc="upper right", prop={'size': 12})
    plt.grid()
    plt.title("L1 distance (predicted vs true throughput) & R2 score")
    plt.xlabel("Percentage of explored space")
    plt.subplot(2, 1, 2)
    plt.plot(exp_iter_percentage, model_res_fun, linestyle='--', marker='o', color='b')
    # plt.ylim(0, 1000000)
    # plt.xlim(0, 101)
    plt.grid()
    if objective_maximize == True:
        plt.ylabel("max f(x)")
        title2 = "Convergence plot: max_fx=" + str(-model_res.fun) + " ,x=" + str(model_res.x)
    else:
        plt.ylabel("min f(x)")
        title2 = "Convergence plot: min_fx=" + str(model_res.fun) + " ,x=" + str(model_res.x)
    plt.title(title2)
    plt.xlabel("Percentage of explored space")
    plt.legend()
    plt.show()
    fig_title = "acq_func=" + acq_func + acq_func_param_type + str(acq_func_param_value) + ",BO_iters=" + str(
        total_calls) + ",rand_state=" + str(rand_state) + ",initial_fit_sb_exp=" + str(initial_exp)
    fig_path = "figures\\"
    fig_total_path = fig_path + fig_title
    fig.savefig(fig_total_path + '.pdf', format='pdf', dpi=1000)
    fig.savefig(fig_total_path + '.eps', format='eps', dpi=1000)
    # ##### The following script prints performance metrics about a fixed model returned by Bayesian Optimization.
    # In[11]:
    check_index = 20
    print("L1 distance=", exp_iter_l1[check_index], "after", exp_iter[check_index] - 1, "B.O calls\n")
    print("R2 score=", exp_iter_r2[check_index], "after", exp_iter[check_index] - 1, "B.O calls\n")
    print("Optimum throughput=", model_res_fun[check_index], "after", exp_iter[check_index] - 1, "B.O calls\n")
    print("Percentage of explored space=", exp_iter_percentage[check_index], "% after", exp_iter[check_index] - 1,
          "B.O calls\n")
    # # 7) Print for every point of the space the true throughput vs. estimation
    # In[12]:
    # print(model_res)
    print("Performance Cost Estimation after", len(model.models) - 1, "B.O calls\n")
    init_data_map = initialize_data_map()
    gp = model.models[len(model.models) - 1]
    for index in range(len(init_data_map)):
        feature = list(list(init_data_map)[index])
        feature_gp = model.space.transform([feature])
        y_pred, sigma = gp.predict(feature_gp, return_std=True)
        y_true = list(init_data_map.values())[index]
        print("\nTrue Experiment: throughput=", -y_true, ", with features=", feature)
        CI_95 = [(y_pred - 1.9600 * sigma)[0], (y_pred + 1.9600 * sigma)[0]]
        print("GP Predicted: throughput=", -y_pred[0], ",sigma=", sigma)
        print("GP Predicted: 0.95 confidence interval=", [-float(i) for i in CI_95])
    print(model_res)
