import numpy as np

from skopt.learning import GaussianProcessRegressor
from skopt.learning.gaussian_process.kernels import HammingKernel
from BayesianOptimization import choose_x_random_experiments, initialize_data_map
from skopt import Optimizer


def build_model(space):
    gpr = GaussianProcessRegressor(alpha=1e-10, copy_X_train=True,
                                   kernel=1 ** 2 * HammingKernel(length_scale=[1.0, 1.0, 1.0, 1.0, 1.0, 1.0]),
                                   n_restarts_optimizer=2, noise=1e-08, normalize_y=True,
                                   optimizer='fmin_l_bfgs_b', random_state=10)

    acq_func = "LCB"  # "EI","PI", "LCB"
    acq_func_ei_xi = 100  # default=0.01
    acq_func_lcb_kappa = 100000  # default=1.96
    thisdict = {'xi': acq_func_ei_xi, 'kappa': acq_func_lcb_kappa}

    model = Optimizer(
        dimensions=space,
        acq_func=acq_func,
        base_estimator=gpr,
        n_initial_points=0,
        n_random_starts=0,
        random_state=10,
        acq_optimizer='auto',
        acq_func_kwargs=thisdict
    )
    return model


def build_init_model(initial_exp, rand_state, data, output_col, objective_maximize, space):
    data_map = initialize_data_map(data, output_col)

    exp_features, exp_values = choose_x_random_experiments(int(initial_exp), data_map, True, rand_state)

    gpr = GaussianProcessRegressor(alpha=1e-10, copy_X_train=True,
                                   kernel=1 ** 2 * HammingKernel(length_scale=[1.0, 1.0, 1.0, 1.0, 1.0, 1.0]),
                                   n_restarts_optimizer=2, noise=1e-08, normalize_y=True,
                                   optimizer='fmin_l_bfgs_b', random_state=rand_state)

    acq_func = "LCB"  # "EI","PI", "LCB"
    acq_func_ei_xi = 100  # default=0.01
    acq_func_lcb_kappa = 100000  # default=1.96
    thisdict = {'xi': acq_func_ei_xi, 'kappa': acq_func_lcb_kappa}

    model = Optimizer(
        dimensions=space,
        acq_func=acq_func,
        base_estimator=gpr,
        n_initial_points=initial_exp,
        n_random_starts=0,
        random_state=rand_state,
        acq_optimizer='auto',
        acq_func_kwargs=thisdict
    )
    model.tell(exp_features, exp_values, fit=True)
    return model


def fit_model(model, initial_exp, rand_state, data, output_col):
    data_map = initialize_data_map(data, output_col)
    exp_features, exp_values = choose_x_random_experiments(int(initial_exp), data_map, True, rand_state)
    model.tell(exp_features, exp_values, fit=True)

    return model


def estimate_model(model, data, output_col, rand_state, initial_exp):
    data_map = initialize_data_map(data, output_col)
    print("estimate")
    # data_map = choose_x_random_experiments_2(int(initial_exp), data_map, True, rand_state)
    print(data_map)
    gp = model.models[-1]
    res = []
    for index in range(len(data_map)):
        feature = list(list(data_map)[index])

        feature_gp = model.space.transform([feature])
        y_pred, sigma = gp.predict(feature_gp, return_std=True)
        feature.append(str(int(y_pred[0])))
        print(feature)
        res.append(feature)
    return res


def evaluation_model(model, data, output_col):
    data_map = initialize_data_map(data, output_col)
    l1_sum = 0
    y_true_list = []
    y_pred_list = []
    gp = model.models[-1]
    res = []
    for index in range(len(data_map)):
        feature = list(list(data_map)[index])
        feature_gp = model.space.transform([feature])
        print(type(feature_gp))
        print(feature_gp)
        y_pred, sigma = gp.predict(feature_gp, return_std=True)
        y_true = list(data_map.values())[index]
        # calculate l1 distance
        print("true ", y_true)
        print("predicted ", y_pred[0])
        l1 = np.abs(y_pred[0] - y_true)
        l1_sum += l1
        # calculate R2 score
        y_true_list.append(y_true)
        y_pred_list.append(y_pred[0])
    print("true ", y_true_list)
    print("predicted ", y_pred_list)
    y_true_list = np.array(y_true_list).reshape(-1, 1)
    y_pred_list = np.array(y_pred_list).reshape(-1, 1)
    u = ((y_true_list - y_pred_list) ** 2).sum()
    v = ((y_true_list - y_true_list.mean()) ** 2).sum()
    r2_score = (1 - u / v)
    l1_avg = l1_sum / y_true_list.size
    print("size ", y_true_list.size)
    return [l1_avg, r2_score]


def dist_toCategorical(p_s):
    from skopt.space import Categorical
    S_C = []
    for key, value in p_s.items():
        S_C.append(Categorical(value, name=key))
    return S_C
