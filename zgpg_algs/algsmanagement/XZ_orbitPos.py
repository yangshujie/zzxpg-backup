import time

import requests
import datetime
import numpy as np
import uuid
import random
import math


def getOrbitPic(data):
    '''
    星座轨位保持状态
    :param args:
    :param kwargs:
    :return:
    '''
    GEO_JSON = {'BD3G01': 140, 'BD3G02': 80, 'BD3G03': 110.5, 'BD3G04': 160}
    IGSO_JSON = {'BD3I01': 140, 'BD3I02': 80, 'BD3I03': 110.5}
    MEO_JSON = {'BD3M01': 0, 'BD3M02': 45, 'BD3M03': 270, 'BD3M04': 315, 'BD3M05': 15, 'BD3M06': 105,
                'BD3M07': 210, 'BD3M08': 255, 'BD3M09': 120, 'BD3M10': 165, 'BD3M11': 60, 'BD3M12': 150,
                'BD3M13': 90, 'BD3M14': 180, 'BD3M15': 345, 'BD3M16': 75, 'BD3M17': 240, 'BD3M18': 330,
                'BD3M19': 135, 'BD3M20': 225, 'BD3M21': 300, 'BD3M22': 30, 'BD3M23': 195, 'BD3M24': 285}
    # series = data["series"]
    # sat_type = data["satType"]
    orbit_element_url = "http://5.30.73.79:8085/xwsb/sync/orbitelement/getHistortyOrbitelement"
    if "orbitElementUrl" in data.keys():
        if data["orbitElementUrl"] != "":
            orbit_element_url = data["orbitElementUrl"]
    start_time = data["startTime"]
    end_time = data["endTime"]
    end_time = stampToTime(timeToStamp(end_time) + 1)
    orbit_propagator_url = "http://5.30.73.87:8083/algorithmOut/input"
    if "orbitPropagatorUrl" in data.keys():
        if data["orbitPropagatorUrl"] != "":
            orbit_propagator_url = data["orbitPropagatorUrl"]

    pic1_value = []
    pic2_value = []
    meo_color = "RGB(" + str(math.floor(random.random() * 255)) + ", " + str(math.floor(random.random() * 255)) + ", " + str(math.floor(random.random() * 255)) + ")"
    geo_color = "RGB(" + str(math.floor(random.random() * 255)) + ", " + str(math.floor(random.random() * 255)) + ", " + str(math.floor(random.random() * 255)) + ")"
    igso_color = "RGB(" + str(math.floor(random.random() * 255)) + ", " + str(math.floor(random.random() * 255)) + ", " + str(math.floor(random.random() * 255)) + ")"





    # 取六根数
    try:
        # input1 = {"taskCode": sat_type, "endTime": start_time}  # 取距离开始时间最近的六根数
        start_time_temp = stampToTime(timeToStamp(start_time) - 86400 * 30)
        for sat_type in MEO_JSON.keys():
            input1 = {"satelliteCodes": sat_type, "endTime": start_time, "startTime": start_time_temp, "count": 1,
                      "type": "mean"}
            rsp1 = requests.get(url=orbit_element_url, params=input1)
            orbit_element = eval(rsp1.text)["data"][sat_type][0]
            base_date = "1949-12-31 00:00:00"
            epoch_time = stampToTime(
                timeToStamp(str(str_to_stamp(base_date) + datetime.timedelta(days=int(orbit_element["meanJd"])))) + float(
                    orbit_element["meanJs"]))

            # 轨道外推
            end_time_stamp_temp = timeToStamp(start_time) + 86400
            print(sat_type + "星当前历元时刻：" + epoch_time)
            end_time_temp = stampToTime(end_time_stamp_temp)
            epoch_time_stamp = timeToStamp(epoch_time)
            start_time_stamp = timeToStamp(start_time)
            input_list = []
            orbitalElements = [orbit_element["meanA"], orbit_element["meanE"], orbit_element[
                "meanI"], orbit_element["meanO"], orbit_element["meanW"], orbit_element["meanM"]]
            start_time_new = start_time.split(" ")[0] + " 08:00:00"
            input2 = {"id": 1, "satId": sat_type, "satName": sat_type, "ephdurationStart": start_time_new,
                      "ephdurationEnd": start_time_new,
                      "elements": orbitalElements, "epoch": epoch_time, "stepSub": 60, "step": 60, "mass": 1000,
                      "atoModel": 1, "optModel": 1, "forceNsp": [30, 30], "forceSrp": [1.44, 20], "forceDrg": [2.2, 20],
                      "forceThbd": [10, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11], "pulse": []}
            input_list.append(str(input2))
            input_json = {"data": input_list}
            count = 0
            while count < 30:
                current_time = datetime.datetime.now()
                time_slice = str(current_time.date()).split("-")
                request_id_head = time_slice[0] + time_slice[1] + time_slice[2]
                request_id = request_id_head + str(int(timeToStamp(str(current_time)[:-7])))
                rsp2 = requests.post(url=orbit_propagator_url,
                                     json={"input": input_json, "requestId": request_id,
                                           # 异步id：202401171458312510  同步id：202311140916236818
                                           "totalNum": len(input_list),
                                           "batchNum": 1, "algorithmName": "hpop", "programId": 1,
                                           "output": {"callBackUrl": "", "outputType": 12}})
                if eval(rsp2.text)["data"][0] != "":
                    break
                count += 1
                time.sleep(1)
            if eval(rsp2.text)["data"][0] == "":
                print("调用30次接口都没算出来的" + sat_type)
                continue
            orbit_element_List = eval(eval(rsp2.text)["data"][0])["content"].split("\n")[:-1]
            # ["satId", "SatName","time","a","e", "i", "o","w","m", "lon", "lat", "h", "x", "y", "z", "vx", "vy", "vz", "toe", "drift","perigeeDis", "apogeeDis","perigeeTime","apogeeTime"]
            phi_list = []
            o_list = []
            for i, item in enumerate(orbit_element_List):
                data = item.split(",")
                phi = float(data[7]) + float(data[8])
                if phi > 360:
                    phi -= 360
                phi_list.append(phi)
                o_list.append(float(data[6]))
            pic1_value.append({"x":np.mean(o_list), "y":np.mean(phi_list), "label":sat_type, "color":meo_color})


        for sat_type in GEO_JSON.keys():
            input1 = {"satelliteCodes": sat_type, "endTime": start_time, "startTime": start_time_temp, "count": 1,
                      "type": "mean"}
            rsp1 = requests.get(url=orbit_element_url, params=input1)
            orbit_element = eval(rsp1.text)["data"][sat_type][0]
            base_date = "1949-12-31 00:00:00"
            epoch_time = stampToTime(
                timeToStamp(str(str_to_stamp(base_date) + datetime.timedelta(days=int(orbit_element["meanJd"])))) + float(
                    orbit_element["meanJs"]))

            # 轨道外推
            end_time_stamp_temp = timeToStamp(start_time) + 86400
            print(sat_type + "星当前历元时刻：" + epoch_time)
            end_time_temp = stampToTime(end_time_stamp_temp)
            epoch_time_stamp = timeToStamp(epoch_time)
            start_time_stamp = timeToStamp(start_time)
            input_list = []
            start_time_new = start_time.split(" ")[0] + " 08:00:00"
            orbitalElements = [orbit_element["meanA"], orbit_element["meanE"], orbit_element[
                "meanI"], orbit_element["meanO"], orbit_element["meanW"], orbit_element["meanM"]]
            input2 = {"id": 1, "satId": sat_type, "satName": sat_type, "ephdurationStart": start_time_new,
                      "ephdurationEnd": start_time_new,
                      "elements": orbitalElements, "epoch": epoch_time, "stepSub": 60, "step": 60, "mass": 1000,
                      "atoModel": 1, "optModel": 1, "forceNsp": [30, 30], "forceSrp": [1.44, 20], "forceDrg": [2.2, 20],
                      "forceThbd": [10, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11], "pulse": []}
            input_list.append(str(input2))
            # if series == "MEO":
            #     orbitalElements_meo = [orbit_element_meo["oscuA"], orbit_element_meo["oscuE"], orbit_element_meo[
            #     "oscuI"], orbit_element_meo["oscuO"], orbit_element_meo["oscuW"], orbit_element_meo["oscuM"]]
            #     input_meo = {"id": 2, "satId": "BD3M01", "satName": "BD3M01", "ephdurationStart": start_time, "ephdurationEnd":end_time_temp,
            #           "elements": orbitalElements_meo, "epoch": epoch_time_meo, "stepSub":60, "step": 60, "mass": 1000,
            #           "atoModel":1, "optModel":1, "forceNsp":[30,30], "forceSrp":[1.44,20],"forceDrg":[2.2,20],"forceThbd":[10,1,2,4,5,6,7,8,9,10,11],"pulse": []}
            #     input_list.append(str(input_meo))
            input_json = {"data": input_list}
            count = 0
            while count < 30:
                current_time = datetime.datetime.now()
                time_slice = str(current_time.date()).split("-")
                request_id_head = time_slice[0] + time_slice[1] + time_slice[2]
                request_id = request_id_head + str(int(timeToStamp(str(current_time)[:-7])))
                rsp2 = requests.post(url=orbit_propagator_url,
                                 json={"input": input_json, "requestId": request_id,
                                       "totalNum": len(input_list),
                                       "batchNum": 1, "algorithmName": "hpop", "programId": 1, "output": {"callBackUrl": "", "outputType": 12}})
                if eval(rsp2.text)["data"][0] != "":
                    break
                count += 1
                time.sleep(1)
            if eval(rsp2.text)["data"][0] == "":
                print("调用30次接口都没算出来的" + sat_type)
                continue
            orbit_element_List = eval(eval(rsp2.text)["data"][0])["content"].split("\n")[:-1]
            # ["satId", "SatName","time","a","e", "i", "o","w","m", "lon", "lat", "h", "x", "y", "z", "vx", "vy", "vz", "toe", "drift","perigeeDis", "apogeeDis","perigeeTime","apogeeTime"]
            lon_list = []
            for i, item in enumerate(orbit_element_List):
                data = item.split(",")
                lon_list.append(float(data[9]))
            pic2_value.append({"x":np.mean(lon_list), "y":0, "label":sat_type, "color":geo_color})


        for sat_type in IGSO_JSON.keys():
            input1 = {"satelliteCodes": sat_type, "endTime": start_time, "startTime": start_time_temp, "count": 1,
                      "type": "mean"}
            rsp1 = requests.get(url=orbit_element_url, params=input1)
            orbit_element = eval(rsp1.text)["data"][sat_type][0]
            base_date = "1949-12-31 00:00:00"
            epoch_time = stampToTime(
                timeToStamp(str(str_to_stamp(base_date) + datetime.timedelta(days=int(orbit_element["meanJd"])))) + float(
                    orbit_element["meanJs"]))

            # 轨道外推
            end_time_stamp_temp = timeToStamp(start_time) + 86400
            print(sat_type + "星当前历元时刻：" + epoch_time)
            end_time_temp = stampToTime(end_time_stamp_temp)
            epoch_time_stamp = timeToStamp(epoch_time)
            start_time_stamp = timeToStamp(start_time)
            input_list = []
            orbitalElements = [orbit_element["meanA"], orbit_element["meanE"], orbit_element[
                "meanI"], orbit_element["meanO"], orbit_element["meanW"], orbit_element["meanM"]]
            input2 = {"id": 1, "satId": sat_type, "satName": sat_type, "ephdurationStart": start_time,
                      "ephdurationEnd": end_time_temp,
                      "elements": orbitalElements, "epoch": epoch_time, "stepSub": 60, "step": 60, "mass": 1000,
                      "atoModel": 1, "optModel": 1, "forceNsp": [30, 30], "forceSrp": [1.44, 20], "forceDrg": [2.2, 20],
                      "forceThbd": [10, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11], "pulse": []}
            input_list.append(str(input2))
            # if series == "MEO":
            #     orbitalElements_meo = [orbit_element_meo["oscuA"], orbit_element_meo["oscuE"], orbit_element_meo[
            #     "oscuI"], orbit_element_meo["oscuO"], orbit_element_meo["oscuW"], orbit_element_meo["oscuM"]]
            #     input_meo = {"id": 2, "satId": "BD3M01", "satName": "BD3M01", "ephdurationStart": start_time, "ephdurationEnd":end_time_temp,
            #           "elements": orbitalElements_meo, "epoch": epoch_time_meo, "stepSub":60, "step": 60, "mass": 1000,
            #           "atoModel":1, "optModel":1, "forceNsp":[30,30], "forceSrp":[1.44,20],"forceDrg":[2.2,20],"forceThbd":[10,1,2,4,5,6,7,8,9,10,11],"pulse": []}
            #     input_list.append(str(input_meo))
            input_json = {"data": input_list}
            count = 0
            while count < 30:
                current_time = datetime.datetime.now()
                time_slice = str(current_time.date()).split("-")
                request_id_head = time_slice[0] + time_slice[1] + time_slice[2]
                request_id = request_id_head + str(int(timeToStamp(str(current_time)[:-7])))
                rsp2 = requests.post(url=orbit_propagator_url,
                                     json={"input": input_json, "requestId": request_id,
                                           "totalNum": len(input_list),
                                           "batchNum": 1, "algorithmName": "hpop", "programId": 1, "output": {"callBackUrl": "", "outputType": 12}})
                if eval(rsp2.text)["data"][0] != "":
                    break
                count += 1
                time.sleep(1)
            if eval(rsp2.text)["data"][0] == "":
                print("调用30次接口都没算出来的" + sat_type)
                continue
            orbit_element_List = eval(eval(rsp2.text)["data"][0])["content"].split("\n")[:-1]
            # ["satId", "SatName","time","a","e", "i", "o","w","m", "lon", "lat", "h", "x", "y", "z", "vx", "vy", "vz", "toe", "drift","perigeeDis", "apogeeDis","perigeeTime","apogeeTime"]
            lon_list = []
            lat_list = []
            for i, item in enumerate(orbit_element_List):
                data = item.split(",")
                lon_list.append(float(data[9]))
                lat_list.append(float(data[10]))
            lon_ave = get_lon(lon_list, lat_list)
            pic2_value.append({"x":lon_ave, "y":0, "label":sat_type, "color":igso_color})


        res = {"pic1": pic1_value,
               "pic2": pic2_value}

        return True, res
    except Exception as e:
        return False, {"msg": e.args}


def str_to_stamp(time_str):
    return datetime.datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S")


def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime


def get_lon(lon_list, lat_list):
    '''
    根据纬度变化计算赤经
    :param lon_list:
    :param lat_list:
    :return:
    '''
    lon_res_list = []
    # 当纬度列表里有零值时,采用numpy
    if 0 in lat_list:
        lat_np = np.array(lat_list)
        index = np.where(lat_np == 0)
        for i in index[0]:
            if i != 0:
                if lat_np[i - 1] < 0:
                    lon_res_list.append(lon_list[i])
            else:
                if lat_np[i + 1] > 0:
                    lon_res_list.append(lon_list[i])
    else:
        for i in range(len(lat_list) - 1):
            if lat_list[i] < (lat_list[i] + lat_list[i + 1]) < lat_list[i + 1]:
                percent = (0 - lat_list[i]) / (lat_list[i + 1] - lat_list[i])
                lon_res_list.append((lon_list[i + 1] - lon_list[i]) * percent + lon_list[i])
    if len(lon_res_list) != 0:
        return sum(lon_res_list) / len(lon_res_list)
    else:
        return 0


if __name__ == "__main__":
    #     input = {
    #   "input": {
    #     "data": ["{\"id\": 1,\"orbitalElements\": \"7244535.885311,0.001472,99.138264,181.256571,357.271467,0.684692\",\"sateCode\": \"FUN1A\",\"sceneId\": \"1102\",\"span\": 86400,\"startTime\": 0,\"step\": 60,\"toe\": \"2021-06-20 20:19:36\"}","{\"id\": 1,\"orbitalElements\": \"7244535.885311,0.001472,99.138264,181.256571,357.271467,0.684692\",\"sateCode\": \"BD3G01\",\"sceneId\": \"1102\",\"span\": 86400,\"startTime\": 0,\"step\": 60,\"toe\": \"2021-06-20 20:19:36\"}"]
    #   },
    #   "requestId": "112",
    #   "totalNum": 2,
    #   "batchNum": 1,
    #   "algorithmName": "J2"
    # }
    #     rsp2 = requests.post(url="http://5.30.73.87:8083/algorithmOut/input",
    #                         json=input)
    #     orbit_element_List = eval(eval(rsp2.text)["data"][0])["orbitals"]
    #     print(float(orbit_element_List[0].split(",")[1]))

    # data = ["\"2019-01-01 00:00:00, 6878000, 0, 0, 0\", \"2019-01-01 00:00:00, 6878000, 0, 0, 0\", \"2019-01-01 00:00:00, 6878000, 0, 0, 0\""]
    # print(data[0].split("\"")[lambda x:x/2==0])

    # lon_list = [120,135,140,144,150,151]
    # lat_list = [10,7,1,-3,-5,-9]
    # print(get_lon(lon_list, lat_list))

    # current_time = datetime.datetime.now()
    # print(current_time.date())
    # time = str(current_time.date()).split("-")
    # print((time[0] + time[1] + time[2]))
    # print(current_time.year)
    # print(current_time.month)
    # print(current_time.day)

    config = {"series": "MEO",
              "satType": "BD3M03",
              "startTime": "2023-07-01 00:00:00",
              "endTime": "2023-07-02 00:00:00",
              "orbitElementUrl": "http://5.30.73.79:8085/xwsb/sync/orbitelement/getHistortyOrbitelement",
              "orbitPropagatorUrl": "http://5.30.73.87:8083/algorithmOut/input"}
    print(getOrbitPic(config))

    # print(math.floor(random.random() * 255))

    # print(timeToStamp("1950-01-01 00:00:00"))

    # date = str_to_stamp("1950-01-01 00:00:00")
    # delta = datetime.timedelta(days=10)
    # newdata = date + delta
    # print(newdata)
