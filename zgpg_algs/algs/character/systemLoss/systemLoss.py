#coding: utf-8

import pandas as pd
import numpy as np
from scipy import stats
import requests
import json,time,math,re

# [[[time],[data1],[data2]],[]]



def algsMain(*args,**kwargs):
    '''
    计算系统不可用率
    :param args:
    :param kwargs:
    :return:
    '''
    try:


        rst = []
        chara_list = []

        datas = args[0]
        all_days_datas = [np.asarray(item[1:]) for item in datas]
        for idx,every_day_data in enumerate(all_days_datas):
            # today_times=datas[idx][0]
            tmpdata = every_day_data[0]
            if len(tmpdata) == 0:
                chara_list.append(0)
                rst.append(1)
            else:
                chara_list.append(np.average(tmpdata))
                # zhongshu=stats.mode(tmpdata)[0][0]
                # ouofranges = np.where(abs(tmpdata - zhongshu)>0.5)
                # excepnums = len(ouofranges[0])
                # if 1-excepnums / 24.0 <0:
                #     rst.append(0)
                # else:
                #     rst.append(1-excepnums / 24.0)
                diff_idx=np.where(np.diff(tmpdata)>0.1,1,0)
                excepnums = len(np.where(diff_idx==1)[0])
                if 1-excepnums / 24.0 <0:
                    rst.append(0)
                else:
                    rst.append(1-excepnums / 24.0)
        chara_list.extend(rst)
        # chara_list.extend(chara_list)
        return True,chara_list
    except Exception as e:
        return False,e.args

# 1678204799
if __name__ == '__main__':
    args=[[[1678118400,1678118401,1678118402,1678118403,1678118404,1678118405,1678118406,1678118407,1678118410,1678118412],[0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0
                                                        ]]]
    kwargs={}
    status,rst=algsMain(args,**kwargs)
    print(rst)

