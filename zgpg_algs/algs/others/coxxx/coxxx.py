from scipy import stats
import sys
import statistics

def algsMain(*args,**kwargs):
    try:


        # print('1111111111111111111111111111111111111')
        list_c = args[0]
        if len(list_c) == 1:
            return True, [[0], [0]]
        trends = []
        lst = list_c.copy()
        raw_len = len(lst)
        sum_per_list = []
        # 时间窗建议最小为6`
        window_len = 8
        if window_len > len(lst):
            window_len = len(lst)
        c = raw_len // window_len

        n_pos = n_neg = 0

    # 先判断第一个时间窗的趋势和百分数
        list_0 = lst[0:window_len]
        list_0_median = statistics.median(list_0)
        if list_0[0] > list_0_median + 0.01:
            trends.append("-1")
        elif list_0[0] < list_0_median - 0.01:
            trends.append("1")
        else:trends.append("0")


        #后续判断拐点，然后和前一拐点的
        if c >= 1:
            for i in range(c - 1):
                for k in range(window_len):
                    diff = lst[(i + 1) * window_len + k] - lst[i * window_len + k]
                    if diff > 0.01:
                        n_pos += 1
                    elif diff < -0.01:
                        n_neg += 1
                    else:
                        continue


                num = n_neg + n_pos
                k = min(n_pos, n_neg)

                # 这里用来计算 '趋势不存在的' 概率有多大
                p_value = 2 * stats.binom.cdf(k, num, 0.5)

                if n_pos > n_neg and p_value < 0.05:
                    # if n_pos > n_neg:
                    trends.append("1")
                    n_pos = n_neg = 0
                elif n_neg > n_pos and p_value < 0.05:
                    # elif n_neg > n_pos:
                    trends.append("-1")
                    n_pos = n_neg = 0
                else:
                    trends.append("0")
                    n_pos = n_neg = 0

        if c * window_len < raw_len:
            for i in range(window_len):
                diff = lst[raw_len - i-1] - lst[raw_len - i - window_len-1]

                if diff > 0.01:
                    n_pos += 1
                elif diff < -0.01:
                    n_neg += 1
                else:
                    continue
            num = n_neg + n_pos
            k = min(n_pos, n_neg)

            # 这里用来计算 '趋势不存在的' 概率有多大
            p_value = 2 * stats.binom.cdf(k, num, 0.5)

            if n_pos > n_neg and p_value < 0.05:
                trends.append("1")
            elif n_neg > n_pos and p_value < 0.05:
                trends.append("-1")
            else:
                # pass
                trends.append("0")


        if len(trends) == 0:
            return True, []
        merged_trend = [trends[0]]
        turn_spot = [0]
        percent = []
        for i in range(1, len(trends)):
            if trends[i] != trends[i - 1]:
                merged_trend.append(trends[i])
                turn_spot.append(int(((i) * window_len) // 1))
        sum_per_last = 0
        if int(len(lst)-1) not in turn_spot:
            turn_spot.append(len(lst)-1)
        for i in range(len(merged_trend)):
            if merged_trend[i] == "0":
                sum_per_list.append(0)


            else:
                if i == 0:

                    list_0 = lst[:int(turn_spot[1])]
                    if list_0[0] != 0:
                        sum_per_list.append(round( (statistics.median(list_0)/list_0[0] -1 ) * 100,3 ))
                    else:
                        if statistics.median(list_0)>0:
                            sum_per_list.append(100)
                            print(1)
                        elif statistics.median(list_0)==0:
                            sum_per_list.append(0)
                        else:
                            sum_per_list.append(-100)

                elif i == len(merged_trend)-1:
                    for k in range(window_len):
                        if lst[turn_spot[len(turn_spot)-2]-1-k] != 0 :


                            if (lst[len(lst)-1-k]/lst[turn_spot[len(turn_spot)-2]-1-k] -1) * int(merged_trend[i])>0:
                                sum_per_last=round((lst[len(lst)-1-k]/lst[turn_spot[len(turn_spot)-2]-1-k] -1)*100,3)
                                break
                            else:
                                continue
                        else:
                            if lst[len(lst)-1-k]>0:
                                sum_per_last =100

                            elif lst[len(lst)-1-k] == 0:
                                sum_per_last = 0
                            else:
                                sum_per_last = -100

                else:
                    for k in range(window_len):
                        if lst[turn_spot[i - 1] + k] != 0:
                            # print(i)
                            if (lst[turn_spot[i]+k] / lst[turn_spot[i-1]+k]-1) * int(merged_trend[i]) >0:
                                sum_per_list.append(round((lst[turn_spot[i]+k] / lst[turn_spot[i-1]+k]-1)*100,3))
                                break
                            else:
                                if k == window_len - 1:
                                    if lst[turn_spot[i] + k] > 0:
                                        sum_per_list.append(100)
                                        print(3)
                                    elif lst[turn_spot[i] + k] == 0:
                                        sum_per_list.append(0)
                                    else:
                                        sum_per_list.append(-100)
                                    # sum_per_list.append(0)
                                    break
                                else:
                                    continue



        if sum_per_last != None and sum_per_last != sum_per_list[-1]:
        # if sum_per_last != None:
            sum_per_list.append(sum_per_last)
            # print(sum_per_list)
        if len(sum_per_list) != len(turn_spot)-1:
            return True, [sum_per_list[:-1], turn_spot[:-1]]
        return True,[sum_per_list, turn_spot[:-1]]

    except Exception as e:
        return False, e.args



def trends_count(a):
    if len(a[0]) == 1:
        return ("整体呈" + a[0][0] + "趋势")
    elif len(a[0]) == 2:
        return ("数据趋势先" + a[0][0] + "然后在" + str(a[1][1]) + "附近呈" + a[0][1] + "趋势")
    elif len(a[0]) >= 3:
        char = "数据趋势先" + a[0][0] + "然后在"
        if len(a[0]) - 2 > 2:
            for k in range(1, len(a[0]) - 2):
                char += str(a[1][k]) + "附近呈" + a[0][k] + "趋势,后在"
        char += str(a[1][len(a[0]) - 2]) + "附近呈" + a[0][len(a[0]) - 2] + "趋势,最后在" + str(a[1][len(a[0]) - 1]) + "附近呈" + \
                a[0][len(a[0]) - 1] + "趋势"
        return (char)


# list = [1, 1, 1, 1, 2, 3, 4, 5, 6, 1, 2, 3, 5, 3, 3, 12, 31, 23, 12, 321, 3, 12, 321, 31, 23, 4, 1]
# # list = [1, 2, 3, 4,5,6,7,8,9,10,11,12,13,14,15,16]
# # list = [1, 2, 3, 4,5,6,0,1,2,3,4,5,1, 2, 3, 4,5,6,0,1,2,3,4,5,1, 2, 3, 4,5,6,0,1,2,3,4,5,1, 2, 3, 4,5,6]
# # list = [1, 2, 3, 4,5,6,0,1,2,3,4,5,1, 2, 3, 4,5,6]
# list = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 00, 0, 0, 0, 0, 0, 0, 1]
# list = [1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6, 2, 3, 4, 5, 6, 7]
# list = [1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6, 2, 3, 4, 5, 6, 7, 2, 3, 4, 5, 6, 7]
if __name__ == '__main__':

    testList = [
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        1.0,
        0.9453134797557626,
        0.6776308906899409,
        0.6901121261911269,
        0.6786015431453322,
        0.6925032534480775,
        0.682314073494217,
        0.6816662303434535,
        0.6823066734376849,
        0.6853693055856178,
        0.6922761367447374,
        0.6812436916473549,
        0.6845142350656932,
        0.6827748628715852,
        0.6763710736480879,
        0.6851898357233484,
        0.6760100541970955,
        0.6842274055606632,
        0.683290073554286,
        0.6792563267064636,
        0.6877153164473119,
        0.6817923854376738,
        0.6736920712145071,
        0.6742568735650579,
        0.6710942767892496,
        0.685879931599056,
        0.6732524906856392,
        0.6807472188108581,
        0.6749826905119024,
        0.6761538482537881,
        0.6737661571488586,
        0.6793759219478459,
        0.6862289681287027,
        0.6697344299857898,
        0.6806001111386992,
        0.6778059103404207,
        0.6823243106723071,
        0.6844297482130106,
        0.6738877559274921,
        0.6859104215469018,
        0.6756683853091188,
        0.6883955906703799,
        0.6632209509867114,
        0.6820555788638062,
        0.6907939716823728,
        0.6721466909495669,
        0.6843577978523119,
        0.6797023902223048,
        0.6839541097992351,
        0.682436361219007,
        0.6826407667344335,
        0.671129726123252,
        0.6809646573035866,
        0.6771540991535075,
        0.6775035964190689,
        0.6837173262242868,
        0.676912899706824,
        0.6798588074423239,
        0.6812966396556234,
        0.6699685456342029,
        0.674189056493978,
        0.6841258158937699,
        0.6833550790064864,
        0.6679659124333005,
        0.6810920350263817,
        0.6794462139215238,
        0.6860456617900343,
        0.6793099095131682,
        0.6732450450505092,
        0.679343093629719,
        0.6783066203582275,
        0.6800327753158492,
        0.6792903556694252,
        0.6825397338441974,
        0.6798290388538447,
        0.6862845024480704,
        0.6877363722630225
    ]
    # testList = [1,1,1]
    testList = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25]
    a=algsMain(testList)
    print(a[1])
    # print(len(list))