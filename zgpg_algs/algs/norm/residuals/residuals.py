import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.interpolate import UnivariateSpline, interp1d  # 导入 scipy 中的样条插值工具

def algsMain(*args, **kwargs):
# def residual(*args, **kwargs):
    datas = args[0]
    config = kwargs["config"]
    base_data = config["base_data"]
    indicator_processing_result = []
    indicator_processing_base_result = []

    flag = True
    for i in range(len(datas)):
        if len(datas[i]) != 0:
            flag = False
            break

    if flag:
        chara =["null" for _ in range(len(datas))]
        indi = [1 for _ in range(len(datas))]
        chara.extend(indi)
        return chara


    # 计算基准段的残差
    for i in range(len(base_data)):
        if len(base_data[i][0]) == 0:
            indicator_processing_base_result.append(np.nan)
        else:
            indicator_processing_base_result.append(indicator_processing(base_data[i]))

    # 计算数据段的残差
    for i in range(len(datas)):
        if len(datas[i][0]) == 0:
            indicator_processing_result.append(np.nan)
        else:
            indicator_processing_result.append(indicator_processing(datas[i]))
        # print(indicator_processing_result)
        # print(datas[i])
    # 计算基准段的最大值和平均距离。
    max_v,  data_ideal = get_standard_value(indicator_processing_base_result)


    if np.isnan(max_v) or np.isnan(data_ideal):
        chara = ["null" for _ in range(len(datas))]
        indi = [1 for _ in range(len(datas))]
        chara.extend(indi)
        return chara
    # print(data_ideal)
    for i in range(len(indicator_processing_result)):
        if np.isnan(indicator_processing_result[i]) or abs(indicator_processing_result[i])<=abs(data_ideal):

            indicator_processing_result.append(1.0)
        else:
            indicator_processing_result_i = (11*abs(data_ideal)-abs(indicator_processing_result[i]))/(10*abs(data_ideal))

            if indicator_processing_result_i < 0:
                indicator_processing_result_i=0
            indicator_processing_result.append(indicator_processing_result_i)


    for i in range(len(indicator_processing_result)):
        if np.isnan(indicator_processing_result[i]):
            indicator_processing_result[i] = "null"


    return True, indicator_processing_result
    # except Exception as e:
    #     return False, str(e)


def get_standard_value(list):
    # indicator_processing返回的是标量值，不需要再调用np.nanmean
    list_l2 = []
    for i in range(len(list)):
        # 如果list[i]是数组，使用np.nanmean；如果是标量，直接使用
        if np.isscalar(list[i]) or (isinstance(list[i], (int, float))):
            list_l2.append(list[i])
        else:
            list_l2.append(np.nanmean(list[i]))
    max_v = np.nanmax(list_l2)
    data_ideal = np.nanmean(list_l2)
    return max_v, data_ideal



def cal_mean(*args, **kwargs):
    '''
    均值
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        # print("###############################")

        rst = []

        datas = args[0]
        # print(datas)
        # print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        all_days_datas = [np.asarray(item[1:]) for item in datas]
        # print(datas)
        for idx, every_day_data in enumerate(all_days_datas):
            # todaytimes=datas[idx][0]
            tmpdata = every_day_data[0]
            for i in range(1, len(every_day_data)):
                tmpdata += every_day_data[i]
                # print(tmpdata)
            if len(tmpdata) != 0:
                rst.append(np.nanmean(tmpdata) / len(every_day_data))
            else:
                rst.append("null")

        return rst
    except Exception as e:
        return e.args


def indicator_processing(listA):
    # print("listA:", listA)
    df_time = listA[0]
    if len(df_time) == 0:
        return 0
    data = []
    data_bio = listA[1:]
    data_bio = list(map(list, zip(*data_bio)))
    for k in range(len(data_bio)):
        data.append(np.nanmean(data_bio[k]))


    df_time = np.asarray(df_time)
    # print("111111111111111", df_time)
    # print("1111111111111111111111111111111111")
    df = pd.DataFrame({'datetime': df_time, 'data': data})

    df['datetime'] = pd.to_datetime(df['datetime'])
    df = df.sort_values('datetime').reset_index(drop=True)
    
    # 优化：针对稀疏数据点（如每天一个数据点）的场景进行优化
    # 计算数据点的平均时间间隔
    time_diffs = df['datetime'].diff().dropna()
    if len(time_diffs) > 0:
        avg_interval_seconds = time_diffs.mean().total_seconds()
        time_span_seconds = (df['datetime'].max() - df['datetime'].min()).total_seconds()
        num_data_points = len(df)
        
        # 如果数据点很稀疏（平均间隔 > 1小时），直接对原始数据点进行插值，不生成密集时间网格
        # 这样可以避免几个月的数据生成数百万个时间点
        if avg_interval_seconds > 3600:  # 平均间隔超过1小时（如每天一个点）
            # 直接对原始数据点进行插值和平滑，不进行重采样
            d = df.copy()
            y = d['data'][d['data'].notnull()].values.tolist()
            x = d['data'][d['data'].notnull()].index.tolist()
            
            if len(x) < 2:
                d['data_new'] = d['data']
            else:
                # 对原始数据点进行插值
                if len(x) < 4 or len(y) < 4:
                    fx = interp1d(x, y, kind='linear', fill_value='extrapolate')
                else:
                    fx = interp1d(x, y, kind='cubic', fill_value='extrapolate')
                # 对所有数据点（包括NaN）进行插值
                x1 = range(len(d))
                y1 = fx(x1)
                d['data_new'] = y1
        else:
            # 数据点较密集，根据时间跨度选择重采样频率
            max_allowed_points = 100000  # 限制最大数据点数
            
            if time_span_seconds > max_allowed_points:
                # 时间跨度太大，直接使用原始数据点
                d = df.copy()
                y = d['data'][d['data'].notnull()].values.tolist()
                x = d['data'][d['data'].notnull()].index.tolist()
                if len(x) < 2:
                    d['data_new'] = d['data']
                else:
                    if len(x) < 4 or len(y) < 4:
                        fx = interp1d(x, y, kind='linear', fill_value='extrapolate')
                    else:
                        fx = interp1d(x, y, kind='cubic', fill_value='extrapolate')
                    x1 = range(len(d))
                    y1 = fx(x1)
                    d['data_new'] = y1
            elif time_span_seconds > 86400:  # 超过1天，使用每小时重采样
                freq = "1h"
                helper = pd.DataFrame({'datetime': pd.date_range(start=df['datetime'].min(), 
                                                                  end=df['datetime'].max(), 
                                                                  freq=freq)})
                helper = helper.set_index('datetime')
                d = pd.merge(df, helper, on='datetime', how='outer').sort_values('datetime')
                d.index = range(len(d['datetime']))
                
                y = d['data'][d['data'].notnull()].values.tolist()
                x = d['data'][d['data'].notnull()].index.tolist()
                
                if len(x) < 2:
                    d['data_new'] = d['data']
                else:
                    if len(x) < 4 or len(y) < 4:
                        fx = interp1d(x, y, kind='linear', fill_value='extrapolate')
                    else:
                        fx = interp1d(x, y, kind='cubic', fill_value='extrapolate')
                    x1 = range(len(d))
                    y1 = fx(x1)
                    d['data_new'] = y1
            elif time_span_seconds > 3600:  # 超过1小时，使用每10秒重采样
                freq = "10s"
                helper = pd.DataFrame({'datetime': pd.date_range(start=df['datetime'].min(), 
                                                                  end=df['datetime'].max(), 
                                                                  freq=freq)})
                helper = helper.set_index('datetime')
                d = pd.merge(df, helper, on='datetime', how='outer').sort_values('datetime')
                d.index = range(len(d['datetime']))
                
                y = d['data'][d['data'].notnull()].values.tolist()
                x = d['data'][d['data'].notnull()].index.tolist()
                
                if len(x) < 2:
                    d['data_new'] = d['data']
                else:
                    if len(x) < 4 or len(y) < 4:
                        fx = interp1d(x, y, kind='linear', fill_value='extrapolate')
                    else:
                        fx = interp1d(x, y, kind='cubic', fill_value='extrapolate')
                    x1 = range(len(d))
                    y1 = fx(x1)
                    d['data_new'] = y1
            else:
                # 时间跨度较小，使用每秒重采样（原始逻辑）
                helper = pd.DataFrame({'datetime': pd.date_range(start=df['datetime'].min(), 
                                                                  end=df['datetime'].max(), 
                                                                  freq="1s")})
                helper = helper.set_index('datetime')
                d = pd.merge(df, helper, on='datetime', how='outer').sort_values('datetime')
                d.index = range(len(d['datetime']))
                
                y = d['data'][d['data'].notnull()].values.tolist()
                x = d['data'][d['data'].notnull()].index.tolist()
                
                if len(x) < 2:
                    d['data_new'] = d['data']
                else:
                    if len(x) < 4 or len(y) < 4:
                        fx = interp1d(x, y, kind='linear', fill_value='extrapolate')
                    else:
                        fx = interp1d(x, y, kind='cubic', fill_value='extrapolate')
                    x1 = range(len(d))
                    y1 = fx(x1)
                    d['data_new'] = y1
    else:
        # 只有一个数据点或没有数据点
        d = df.copy()
        d['data_new'] = d['data']
    # plt.plot(d['datetime'][d['data'].notnull()], y, color='b')
    # # plt.plot(x1, y1, color='b')
    # plt.show()
    yObs = d['data_new'].values

    x = range(1, len(yObs) + 1, 1)
    # UnivariateSpline 至少需要3个数据点
    if len(yObs) < 3:
        # 如果数据点太少，直接使用原始数据，不进行平滑
        yS = yObs
        coeffs = pd.Series([])  # 空系数序列
    else:
        # 优化：如果数据量太大，先进行降采样或使用更高效的平滑方法
        if len(yObs) > 50000:
            # 数据量太大，使用pandas的rolling平滑代替UnivariateSpline，性能更好
            yObs_series = pd.Series(yObs)
            # 使用滚动窗口平滑，窗口大小根据数据量自适应
            window_size = max(3, int(len(yObs) / 1000))
            yS = yObs_series.rolling(window=window_size, center=True, min_periods=1).mean().values
            coeffs = pd.Series([])  # rolling平滑不产生系数
        else:
            # 数据量适中，使用原始UnivariateSpline方法
            fSpl = UnivariateSpline(x, yObs)  # 三次样条插值，默认 s= len(w)
            coeffs = fSpl.get_coeffs()  # Return spline coefficients
            # 由拟合函数 fitfunc 计算拟合曲线在数据点的函数值
            yFit = fSpl(x)  # 由插值函数 fSpl1 计算插值点的函数值 yFit
            # print("_______________", yFit)
            # 对拟合函数 fitfunc 进行平滑处理
            fSpl.set_smoothing_factor(20)  # 设置光滑因子 sf
            yS = fSpl(x)  # 由插值函数 fSpl(sf=10) 计算插值点的函数值 ys
            coeffs = fSpl.get_coeffs()  # 平滑处理后的参数
            # print("coeffs of 3rd spline function (sf=10):\n  ", coeffs)
            coeffs = pd.Series(coeffs)
    # coeffs.to_csv('111.csv')

    d['data_interpolate'] = yS
    # d['TMK112_help'] = d['TMK112'].interpolate(method='polynomial', order=5, limit =5)
    # d['TMK112_help'] = d['TMK112'].interpolate(method='polynomial', order=3)
    # d['datetime'] = pd.to_datetime(d['datetime'] )
    # plt.plot(d['datetime'], d['data'], 'r', label='data')
    # plt.plot(d['datetime'], yS, 'b', label='data_interpolate')
    # plt.show()

    data_new = d[['datetime', 'data', 'data_interpolate']]
    data_new = data_new[data_new['data'].notnull()]
    data_new['data_diff'] = data_new['data'] - data_new['data_interpolate']
    x2 = range(1, len(data_new['data_diff']) + 1, 1)
    # plt.plot(x2, data_new['data_diff'], 'b', label='data_interpolate')
    # plt.show()
    diff = np.abs(data_new['data_diff'].tolist())
    # print(diff)
    diff_result = np.nanmean(diff)
    return diff_result


if __name__ == "__main__":

    # data = [1, 2, 3, 4, 5]
    # data = [165, 170, 175]
    # # data = [165, "null", 165]
    # data = ["null","null",0,2,1,3,60]
    # args = [data]
    # args = [[[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405,
    #            1678118406, 1678118407,
    #            1678118420, 1678118430], [2, 2, 4, 2, 1, 2, 2, 4, 2, 2]
    #
    #           ],
    #          [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405,
    #            1678118406, 1678118420,
    #            1678118430, 1678118440],
    #           [2, 2, 2, 2, 2, 2, 2, 1, 2, 2]
    #           ]]]
    # kwargs = {"config": {"indicatorType": 1,
    #                      "k": 1,
    #                      "roundTFlag": 1,
    #                      "min": 20,
    #                      "max": 30,
    #                      # "base_data": [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405,
    #                      #                 1678118406, 1678118407,
    #                      #                 1678118410, 1678118412], [2, 2, 2, 1, 2, 2, 2, 2, 2, 2],
    #                      #                [2, 2, 2, 2, 2, 2, 2, 2, 2, 2],
    #                      #                [2, 2, 2, 2, 2, 2, 2, 2, 2, 2]],
    #                      #               [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405,
    #                      #                 1678118406, 1678118407,
    #                      #                 1678118408, 1678204799], [2, 2, 2, 2, 2, 2, 2, 2, 2, 2],
    #                      #                [2, 2, 2, 2, 2, 2, 2, 2, 2, 2],
    #                      #                [2, 2, 2, 2, 2, 2, 2, 2, 2, 2]]],
    #                      "base_data": [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405,
    #                                      1678118406, 1678118407,
    #                                      1678118410, 1678118412], [2, 2, 2, 2, 2, 2, 1, 2, 2, 2]],
    #                                    [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405,
    #                                      1678118406, 1678118407,
    #                                      1678118408, 1678204799],
    #                                     [2, 2, 2, 2, 2, 1, 2, 1, 2, 2]]],
    #                      "basePath": "C:\\Data",
    #                      'a': 0, 'b': 1}}
    # # 测试区间化处理
    # result, output = algsMain(*args, **kwargs)
    # if result:
    #     print("区间化处理结果：", output)
    # else:
    #     print("区间化处理出错，错误信息：", output)

    # except Exception as e:
    #     print("执行出错，错误信息：", str(e))
# if __name__ == "__main__":
#     list = [[1,2,3,4,5],[2,3,4,5,6],[3,4,5]]
#     min,max,data_ideal = get_standard_value(list)
#     print(min,max,data_ideal)


# if __name__ == "__main__":
#     list1 = [1,2,3,4]
#     list = np.asarray(list1)
#
#     list = 10*list
#     printssss
    data = [np.nan, np.nan]
    if data[0] == np.nan or abs(data[0]) < 1:
        print("1111111111")
    print(abs(data[0]) <1)