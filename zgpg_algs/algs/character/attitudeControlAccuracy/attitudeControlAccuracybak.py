# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math, re

# [[[time],[data1],[data2]],[]]

'''
未完成
'''


def algsMainOld(*args, **kwargs):
    '''
    稳态时段姿态控制精度、稳态时段姿态控制精度  φi、θi、ψi

    控制精度阈值是写死的
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        config = kwargs["config"]
        tel_id = config["cal_object"]

        rst = []
        rst_test = []

        datas = args[0]

        all_days_datas = [np.asarray(item[2:]) for item in datas]

        for idx, every_day_data in enumerate(all_days_datas):
            today_times_angel = datas[idx][0]
            today_times_flag = datas[idx][1]
            # y_fi=abs(every_day_data[0])
            # y_theta=abs(every_day_data[1])
            # y_psi=abs(every_day_data[2])
            # y_control = abs(every_day_data[3])
            #
            # ############## zl修改
            # if tel_id == "滚动角":
            #     y_posible = y_fi
            # elif tel_id == "俯仰角":
            #     y_posible = y_theta
            # elif tel_id == "偏航角":
            #     y_posible = y_psi
            # elif tel_id == "陀螺角速度" and len(every_day_data) > 4:
            #     y_posible = every_day_data[4]
            # else:
            #     y_posible = every_day_data[0]

            # y_fi_idx = np.where(abs(np.diff(y_fi))<=100)
            # y_theta_idx = np.where(abs(np.diff(y_theta)) <= 100)
            # y_psi_idx = np.where(abs(np.diff(y_psi)) <= 100)
            # y_control_idx = np.where(abs(y_control-6) <= 0.1)
            #
            # tmp1_idx = np.intersect1d(y_fi_idx,y_theta_idx)
            # tmp2_idx=np.intersect1d(y_psi_idx,y_control_idx)
            # tmp_idx = np.intersect1d(tmp1_idx,tmp2_idx)

            y_posible = abs(np.array(every_day_data[0]))
            y_control = abs(np.array(every_day_data[1]))
            y_control_idx = np.where(abs(y_control - 6) <= 0.1)
            y_posible_idx = np.where(abs(np.diff(y_posible)) <= 0.01)
            # tmp_idx = y_control_idx[0]
            tmp_idx = np.intersect1d(y_posible_idx, y_control_idx)

            idxs = np.array([(i in tmp_idx) for i in range(len(today_times_flag))])
            idxs = np.where(idxs == True, 1, 0)
            #
            #
            # 稳态时段
            # 把 1 置为 1，0置为0
            norm = idxs
            # 找连续5次触发1的置为c，其他不替换
            norm_continous = re.sub(r'1{10,}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
            # 找出连续为c的索引
            norm_continous_idx = np.array(list(norm_continous)) == 'c'
            norm_continous_idx = np.where(norm_continous_idx == True)
            if len(norm_continous_idx[0]) != 0:
                # 找出索引差分>1的索引，即每一段连续的开始的索引
                diffidxofidx = np.where((np.diff(norm_continous_idx) > 1)[0])
                diffidxofidx = diffidxofidx[0]

                list_norm_continous_idx = list(norm_continous_idx)[0]
                valid_idx = []
                for i in range(-1, len(diffidxofidx)):
                    if len(diffidxofidx) == 0:
                        valid_idx.append(list_norm_continous_idx[:])
                    else:
                        if i == -1:
                            valid_idx.append(list_norm_continous_idx[:diffidxofidx[i + 1] + 1])
                        elif i == len(diffidxofidx) - 1:
                            valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:])
                        else:
                            valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:diffidxofidx[i + 1] + 1])

                final_data = []
                final_data_test = []
                final_data_test1 = []
                final_data_test2 = []
                # valid_idx[2] = valid_idx[2][2500:]
                for valid_idx_single in valid_idx:
                    for data_index in valid_idx_single:
                        if y_posible[data_index] <= 1:
                            final_data.append(y_posible[data_index])
                        else:
                            print(str(data_index) + ":" + str(y_posible[data_index]))
                        final_data_test.append(y_posible[data_index])
                        # final_data_test1.append(y_posible[today_times_angel.index(today_times_flag[data_index])])
                        # final_data_test2.append(y_posible[findLastIndex(today_times_angel,today_times_flag[data_index])])
                    print(
                        "[" + str(valid_idx_single[0]) + ":" + str(today_times_angel[valid_idx_single[0]]) + "," + str(
                            valid_idx_single[-1]) + ":" + str(today_times_angel[valid_idx_single[-1]]) + "]")
                    print("[" + stampToTime(today_times_angel[valid_idx_single[0]]) + "," + stampToTime(
                        today_times_angel[valid_idx_single[-1]]) + "]")
                print(np.std(final_data, dtype=np.float64, ddof=1))
                print("min:" + str(min(final_data)) + "," + "max:" + str(max(final_data)))
                print("方式1----------------------------------------------")
                rst.append(np.std(final_data, dtype=np.float64, ddof=1))

                # remove_index = three_sigma(np.array(final_data_test))
                # for j in reversed(remove_index):
                #     final_data_test.pop(j)
                print(np.std(final_data_test, dtype=np.float64, ddof=1))
                print("min:" + str(min(final_data_test)) + "," + "max:" + str(max(final_data_test)))
                print("方式2----------------------------------------------")
                rst_test.append(np.std(final_data_test, dtype=np.float64, ddof=1))

                # remove_index1 = three_sigma(np.array(final_data_test1))
                # for j in reversed(remove_index1):
                #     final_data_test1.pop(j)
                # print(np.std(final_data_test1, dtype=np.float64, ddof=1))
                # print("min:" + str(min(final_data_test1)) + "," + "max:" + str(max(final_data_test1)))
                # print("方式3----------------------------------------------")
                #
                # # remove_index2 = three_sigma(np.array(final_data_test2))
                # # for j in reversed(remove_index2):
                # #     final_data_test2.pop(j)
                # print(np.std(final_data_test2, dtype=np.float64, ddof=1))
                # print("min:" + str(min(final_data_test2)) + "," + "max:" + str(max(final_data_test2)))
                # print("方式4----------------------------------------------")


            else:
                rst.append(1)  # 如果没有光照时间，暂定默认结果为1

        return True, rst_test
    except Exception as e:
        return False, e.args


def findLastIndex(data, value):
    datacopy = data.copy()
    datacopy.reverse()
    return len(datacopy) - datacopy.index(value) - 1


def algsMain(*args, **kwargs):
    '''
    稳态时段姿态控制精度、稳态时段姿态控制精度  φi、θi、ψi

    控制精度阈值是写死的
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        start_time = time.time()
        print("稳态时段姿态控制精度、稳态时段姿态控制精度算法已开始计算")
        
        datas = args[0]
        config = kwargs["config"]
        
        data_prep_time = time.time()
        print(f"数据准备耗时: {data_prep_time - start_time:.4f} 秒")
        
        result_list = calMain(datas, config)
        
        calc_time = time.time()
        print(f"核心计算耗时: {calc_time - data_prep_time:.4f} 秒")

        print("稳态时段姿态控制精度、稳态时段姿态控制精度算法计算完成")
        total_time = time.time() - start_time
        print(f"算法总耗时: {total_time:.4f} 秒")
        
        return True, result_list
    except Exception as e:
        print("稳态控制精度算法:" + str(e.args))
        return False, e.args


def get_y_ang_idx(y_ang, today_times, diffValue, count):
    func_start_time = time.time()
    
    y_ang_idx = list(np.where(abs(np.diff(y_ang)) < diffValue)[0])
    delete_idx = set([])
    y_time_idx = np.where(np.diff(today_times) <= 10)
    splitTime_continous_idx = split_continous_idx(y_time_idx[0], today_times, count)
    for splitTime_continous_idx_single in splitTime_continous_idx:
        splitTime_continous_idx_single = list(np.intersect1d(y_ang_idx, splitTime_continous_idx_single))
        # for delete_count in range(2, 10):
        #     for index_ang, item_ang in enumerate(splitTime_continous_idx_single[delete_count:]):
        #         if abs(y_ang[item_ang] - y_ang[splitTime_continous_idx_single[index_ang - delete_count]]) >= 0.1 and item_ang in y_ang_idx:
        #             y_ang_idx.remove(item_ang)
        for index_ang, item_ang in enumerate(splitTime_continous_idx_single):
            if index_ang >= 5 and abs(y_ang[item_ang] - y_ang[
                splitTime_continous_idx_single[index_ang - 5]]) >= diffValue and item_ang in y_ang_idx:
                for delete_item in splitTime_continous_idx_single[index_ang - 5: index_ang + 1]:
                    delete_idx.add(delete_item)
            if index_ang >= 10 and abs(y_ang[item_ang] - y_ang[
                splitTime_continous_idx_single[index_ang - 10]]) >= diffValue and item_ang in y_ang_idx:
                for delete_item in splitTime_continous_idx_single[index_ang - 10: index_ang + 1]:
                    delete_idx.add(delete_item)
            if index_ang >= 20 and abs(y_ang[item_ang] - y_ang[
                splitTime_continous_idx_single[index_ang - 20]]) >= diffValue and item_ang in y_ang_idx:
                for delete_item in splitTime_continous_idx_single[index_ang - 20: index_ang + 1]:
                    delete_idx.add(delete_item)
            if index_ang >= 30 and abs(y_ang[item_ang] - y_ang[
                splitTime_continous_idx_single[index_ang - 30]]) >= diffValue and item_ang in y_ang_idx:
                for delete_item in splitTime_continous_idx_single[index_ang - 30: index_ang + 1]:
                    delete_idx.add(delete_item)
    for delete_item_idx in delete_idx:
        y_ang_idx.remove(delete_item_idx)

    func_duration = time.time() - func_start_time
    if func_duration > 0.01:  # 只打印超过10毫秒的调用
        print(f"    [get_y_ang_idx] 耗时: {func_duration:.4f} 秒, 处理点数: {len(y_ang)}")
    
    return y_ang_idx


def calMain(datas, config):
    start_time = time.time()
    timing_info = {}
    
    if "diffValue" in config.keys():
        diffValue = float(config["diffValue"])
    else:
        diffValue = 0.1
    count = eval(config['count'])
    calType = eval(config["calType"])

    index = config["attitudeMode"]
    rst = []
    
    config_parse_time = time.time()
    timing_info['配置解析'] = config_parse_time - start_time
    print(f"[calMain] 配置解析耗时: {timing_info['配置解析']:.4f} 秒")
    
    all_days_datas = [np.asarray(item[1:]) for item in datas]
    
    data_convert_time = time.time()
    timing_info['数据转换'] = data_convert_time - config_parse_time
    print(f"[calMain] 数据转换耗时: {timing_info['数据转换']:.4f} 秒")
    print(f"[calMain] 处理数据集数量: {len(all_days_datas)}")
    
    day_timings = []
    for idx, every_day_data in enumerate(all_days_datas):
        day_start_time = time.time()
        y_control = np.array(every_day_data[index])
        if len(y_control) == 0:
            rst.append("null")
            continue
        today_times = datas[idx][0]
        y_posible = []
        y_ang = []
        ang_tel_id = []
        for ang_id_index in range(1, 4):
            if config["ang_append" + str(ang_id_index)] != -1:
                ang_tel_id.append(config["ang_append" + str(ang_id_index)])
                y_ang.append(every_day_data[config["ang_append" + str(ang_id_index)]])

        for i in range(len(every_day_data)):
            if i not in ang_tel_id and i != index:
                y_posible.extend(every_day_data[i])


        if calType == 0:
            y_posible_idx = []
            if len(y_ang) <= 2:
                y_ang.append(y_posible)
            else:
                y_posible_idx = list(np.where(abs(np.diff(y_posible)) < diffValue)[0])
            y_ang_idx = get_y_ang_idx(y_ang[0], today_times, diffValue, count)
            for y_ang_item in y_ang[1:]:
                y_ang_idx = list(np.intersect1d(y_ang_idx, get_y_ang_idx(y_ang_item, today_times, diffValue, count)))
            if len(y_posible_idx) > 0:
                y_ang_idx = list(np.intersect1d(y_ang_idx, y_posible_idx))
            if config["flagValue"] is not '':
                flag_value_str_list = config["flagValue"].split(',')
                y_control_idx = list(np.where(abs(np.array(y_control) - eval(flag_value_str_list[0])) <= 0.01))
                for flag_value_str in flag_value_str_list[1:]:
                    y_control_idx = list(np.union1d(y_control_idx, list(
                        np.where(abs(np.array(y_control) - eval(flag_value_str)) <= 0.01))))
                tmp_idx = np.intersect1d(y_ang_idx, y_control_idx)
            else:
                rst.append(1)
                continue
        elif calType == 1:
            if config["flagValue"] is not '':
                flag_value_str_list = config["flagValue"].split(',')
                y_control_idx = list(np.where(abs(np.array(y_control) - eval(flag_value_str_list[0])) <= 0.01))
                for flag_value_str in flag_value_str_list[1:]:
                    y_control_idx = list(np.union1d(y_control_idx, list(
                        np.where(abs(np.array(y_control) - eval(flag_value_str)) <= 0.01))))
                tmp_idx = y_control_idx
            else:
                rst.append(1)
                continue
        else:
            y_posible_idx = []
            if len(y_ang) <= 2:
                y_ang.append(y_posible)
            else:
                y_posible_idx = list(np.where(abs(np.diff(y_posible)) < diffValue)[0])
            y_ang_idx = get_y_ang_idx(y_ang[0], today_times, diffValue, count)
            for y_ang_item in y_ang[1:]:
                y_ang_idx = list(np.intersect1d(y_ang_idx, get_y_ang_idx(y_ang_item, today_times, diffValue, count)))
            if len(y_posible_idx) > 0:
                y_ang_idx = list(np.intersect1d(y_ang_idx, y_posible_idx))
            tmp_idx = y_ang_idx

        idxs = np.array([(i in tmp_idx) for i in range(len(today_times))])
        idxs = np.where(idxs == True, 1, 0)

        # 稳态时段
        # 把 1 置为 1，0置为0
        norm = idxs
        # 找连续5次触发1的置为c，其他不替换
        norm_continous = re.sub(r'1{' + str(count) + ',}', lambda x: 'c' * len(x.group()),
                                ''.join(map(str, norm)))
        # 找出连续为c的索引
        norm_continous_idx = np.array(list(norm_continous)) == 'c'
        norm_continous_idx = np.where(norm_continous_idx == True)
        if len(norm_continous_idx[0]) != 0:
            # 找出索引差分>1的索引，即每一段连续的开始的索引
            diffidxofidx = np.where((np.diff(norm_continous_idx) > 1)[0])
            diffidxofidx = diffidxofidx[0]

            list_norm_continous_idx = list(norm_continous_idx)[0]
            valid_idx = []
            for i in range(-1, len(diffidxofidx)):
                if len(diffidxofidx) == 0:
                    valid_idx.append(list_norm_continous_idx[:])
                else:
                    if i == -1:
                        valid_idx.append(list_norm_continous_idx[:diffidxofidx[i + 1] + 1])
                    elif i == len(diffidxofidx) - 1:
                        valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:])
                    else:
                        valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:diffidxofidx[i + 1] + 1])

            final_data = []
            for valid_idx_single in valid_idx:
                for data_index in valid_idx_single:
                    final_data.append(abs(y_posible[data_index]))
            final_data = np.array(final_data)
            # count = 2
            # while count > 0:
            #     data_pre_index = three_sigma(np.array(final_data))
            #     for pre_index in reversed(data_pre_index):
            #         final_data.pop(pre_index)
            #     count -= 1
            rst.append(np.std(final_data, dtype=np.float64, ddof=1))
            print("rst: ", rst)
        else:
            rst.append(1)
        
        day_end_time = time.time()
        day_duration = day_end_time - day_start_time
        day_timings.append(day_duration)
        print(f"[calMain] 第 {idx + 1}/{len(all_days_datas)} 天数据处理耗时: {day_duration:.4f} 秒")

    total_time = time.time() - start_time
    timing_info['总耗时'] = total_time
    timing_info['平均每天耗时'] = np.mean(day_timings) if day_timings else 0
    timing_info['最长处理耗时'] = max(day_timings) if day_timings else 0
    timing_info['最短处理耗时'] = min(day_timings) if day_timings else 0
    
    print("\n" + "="*60)
    print("[calMain] 性能统计摘要:")
    print(f"  配置解析耗时: {timing_info['配置解析']:.4f} 秒")
    print(f"  数据转换耗时: {timing_info['数据转换']:.4f} 秒")
    print(f"  平均每天处理耗时: {timing_info['平均每天耗时']:.4f} 秒")
    print(f"  最长处理耗时: {timing_info['最长处理耗时']:.4f} 秒")
    print(f"  最短处理耗时: {timing_info['最短处理耗时']:.4f} 秒")
    print(f"  总耗时: {timing_info['总耗时']:.4f} 秒")
    print("="*60 + "\n")

        # if config["flagValue"] is not '':
        #     flag_value_str_list = config["flagValue"].split(',')
        #     rst_tmp = []
        #     final_data = []
        #     for flag_value_str in flag_value_str_list:
        #         flag_value = eval(flag_value_str)
        #         y_control_idx = np.where(abs(np.array(y_control) - flag_value) <= 0.01)
        #         if len(y_control_idx[0]) == 0:
        #             break
        #
        #         # tmp_idx1 = np.intersect1d(y_ang_idx, y_control_idx)
        #         # tmp_idx = np.intersect1d(y_ang_idx1, tmp_idx1)
        #         tmp_idx = np.intersect1d(y_ang_idx, y_control_idx)
        #
        #         idxs = np.array([(i in tmp_idx) for i in range(len(today_times))])
        #         idxs = np.where(idxs == True, 1, 0)
        #
        #         # 稳态时段
        #         # 把 1 置为 1，0置为0
        #         norm = idxs
        #         # 找连续5次触发1的置为c，其他不替换
        #         norm_continous = re.sub(r'1{10,}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
        #         # 找出连续为c的索引
        #         norm_continous_idx = np.array(list(norm_continous)) == 'c'
        #         norm_continous_idx = np.where(norm_continous_idx == True)
        #         if len(norm_continous_idx[0]) != 0:
        #             # 找出索引差分>1的索引，即每一段连续的开始的索引
        #             diffidxofidx = np.where((np.diff(norm_continous_idx) > 1)[0])
        #             diffidxofidx = diffidxofidx[0]
        #
        #             list_norm_continous_idx = list(norm_continous_idx)[0]
        #             valid_idx = []
        #             for i in range(-1, len(diffidxofidx)):
        #                 if len(diffidxofidx) == 0:
        #                     valid_idx.append(list_norm_continous_idx[:])
        #                 else:
        #                     if i == -1:
        #                         valid_idx.append(list_norm_continous_idx[:diffidxofidx[i + 1] + 1])
        #                     elif i == len(diffidxofidx) - 1:
        #                         valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:])
        #                     else:
        #                         valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:diffidxofidx[i + 1] + 1])
        #
        #             for valid_idx_single in valid_idx:
        #                 for data_index in valid_idx_single:
        #                     final_data.append(abs(y_posible[data_index]))
        #
        #     rst.append(np.std(final_data, dtype=np.float64, ddof=1))
        # else:
        #     rst.append(1)

    return rst


def calSigmoid(x, indicatorType, min_x, max_x):
    result = 0
    # min_x = floor_min(min_x)
    # max_x = floor_max(max_x)

    if min_x == max_x:
        result = 1
    else:
        b = (math.log(99) * max_x + 2 * math.log(10) * min_x) / (2 * math.log(10) + math.log(99))
        a = 2 * math.log(10) / (max_x - b)

        if indicatorType == 0:
            result = 1 - (1 / (1 + math.exp(-a * (x - b))))
        elif indicatorType == 1:
            result = (1 / (1 + math.exp(-a * (x - b))))
        elif indicatorType == 2:
            result = (1 / (1 + math.exp(-a * (x - b))))

    return result


def floor_min(min_x):
    if min_x <= 0.0001:
        return 0
    if abs(min_x) >= 1:
        output = math.floor(min_x)
    else:
        output = min_x
        count = 0
        while abs(output) < 1:
            output = output * 10
            count += 1
        output = math.floor(output)
        while count > 0:
            output = output / 10.0
            count -= 1
    return output


def floor_max(max_x):
    if max_x <= 0.0001:
        return 0
    if abs(max_x) >= 1:
        output = math.floor(max_x + 1)
    else:
        output = max_x
        count = 0
        while abs(output) < 1:
            output = output * 10
            count += 1
        output = math.floor(output + 1)
        while count > 0:
            output = output / 10.0
            count -= 1
    return output


def calGuassian(x, ideal_x, indicatorType, k, roundTFlag, min_x, max_x):
    if x == "null":
        return 1
    result = 0
    if roundTFlag == 1:
        if min_x <= x <= max_x:
            result = 1
            return result
        elif x < min_x:
            ideal_x = min_x
        elif x > max_x:
            ideal_x = max_x

    if ideal_x > 0.0001:
        min_x = floor_min(min_x)
        if k == 0:
            # k = 1.0
            k = pow((min_x - ideal_x), 2) / (pow(ideal_x, 2) * math.log(10, math.e))
        if k <= 0.0001:
            k = 1
        fx = 0
        if abs(x - ideal_x) > 0.0005:
            fx = math.exp(-pow((x - ideal_x) / (ideal_x), 2) / k)
        else:
            fx = 1

        if indicatorType == 0:
            result = 1 - fx
        elif indicatorType == 1:
            result = fx
        elif indicatorType == 2:
            result = fx
    else:
        result = 1

    return result


def split_continous_idx(tmp_idx, today_times, count):
    idxs = np.array([(i in tmp_idx) for i in range(len(today_times))])
    idxs = np.where(idxs == True, 1, 0)

    # 稳态时段
    # 把 1 置为 1，0置为0
    norm = idxs
    # 找连续5次触发1的置为c，其他不替换
    norm_continous = re.sub(r'1{' + str(count) + ',}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
    # 找出连续为c的索引
    norm_continous_idx = np.array(list(norm_continous)) == 'c'
    norm_continous_idx = np.where(norm_continous_idx == True)
    valid_idx = []
    if len(norm_continous_idx[0]) != 0:
        # 找出索引差分>1的索引，即每一段连续的开始的索引
        diffidxofidx = np.where((np.diff(norm_continous_idx) > 1)[0])
        diffidxofidx = diffidxofidx[0]
        list_norm_continous_idx = list(norm_continous_idx)[0]
        for i in range(-1, len(diffidxofidx)):
            if len(diffidxofidx) == 0:
                valid_idx.append(list_norm_continous_idx[:])
            else:
                if i == -1:
                    valid_idx.append(list_norm_continous_idx[:diffidxofidx[i + 1] + 1])
                elif i == len(diffidxofidx) - 1:
                    valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:])
                else:
                    valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:diffidxofidx[i + 1] + 1])
    return valid_idx


def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime


def three_sigma(df_col):
    '''
    剔野
    :param df_col:
    :return:
    '''
    rule = (df_col.mean() - 2 * df_col.std() > df_col) | (df_col.mean() + 2 * df_col.std() < df_col)
    index = np.arange(df_col.shape[0])[rule]
    return index


# 1678204799
if __name__ == '__main__':
    stampToTime(1550159970)
    print(timeToStamp("2023-04-22 00:00:00"))
    args = [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118410, 1678118412], [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7],
             [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7]],
            [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7],
             [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7]]]
    kwargs = {}

    status, rst = algsMain(args, **kwargs)
    print(status, rst)
