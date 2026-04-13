#coding:utf-8
import csv
import os
import math
import numpy as np
import numpy.polynomial as p
from matplotlib import pyplot as plt
from sklearn.mixture import GaussianMixture
from numpy import unique
import json,time
import torch as torch
from torch import nn
import pandas as pd
import numpy as np
from sklearn import preprocessing
from torch.nn import functional as F
from torch.utils.data import DataLoader
from torch.utils.data import Dataset
from torch.nn.utils import rnn
import os

def timeToStamp(time_in):
    timearray = time.strptime(time_in,"%Y-%m-%d %H:%M:%S")
    return int(time.mktime(timearray))

def stampToTime(stamp):

    strtime=time.strftime("%Y-%m-%d %H:%M:%S",time.localtime(stamp))
    return strtime

#--------------------------------------------------------退化----------------------------------------------------
class Tuihua(object):
    def __init__(self,spit_time,spit_range,targetFields,able_out_dl,able_in_dl,able_all_dy,hlRule,hyRule,fdRule):
        self.spit_time = spit_time  # 中午12点
        self.spit_range = spit_range  # 24小时

        # 相关遥测
        self.targetFields = targetFields
        self.able_out_dl = able_out_dl
        self.able_in_dl = able_in_dl
        self.able_all_dy = able_all_dy

        self.hlRule = hlRule  # 恒流充电规则
        self.hyRule = hyRule  # 恒压充电规则
        self.fdRule = fdRule  # 放电规则
        # self.spit_time = 12 #中午12点
        # self.spit_range = 24  #24小时
        # # 相关遥测
        # self.targetFields = {"out_dl_field":"1026", "in_dl_field":"1028","all_dy_field":"1030"}  #放电电流，充电电流，整租电压
        # self.able_out_dl = [0.5,40]
        # self.able_in_dl = [-1.5,4.5]
        # self.able_all_dy = [75,90]
        #
        # self.hlRule = "4.099062 <= {1028} <= 4.4068188  and {1030} < 88.5 and {1026} < 0.6"  #恒流充电规则
        # self.hyRule = "-0.003453200000000006 <{1028} < 4.099062  and {1030} >= 88.5 and {1026} < 0.6"  #恒压充电规则
        # self.fdRule = " {1030} < 89 and {1026} > 0.7 and {1028} < -0.003453200000000006"  #放电规则

        self.cai_yang = 16  # 采样间隔
        self.dataPath = ""
        self.dayData = []
        self.starttime = 0
        self.endtime = 0

        #遥测原始数据
        self.outDl = {"x":[],"y":[]}
        self.inDl = {"x":[],"y":[]}
        self.allDy = {"x":[],"y":[]}

        self.resData = []
        pass

    def _read_file(self, csv_file):
        times = []
        datas = []
        ccount = self.cai_yang
        with open(csv_file, 'r', encoding="utf-8") as file:
            reader = csv.reader(file)
            for row in reader:
                if ccount > 0:
                    ccount = ccount - 1
                    continue

                if ccount <= 0:
                    ccount = self.cai_yang
                    times.append(int(row[0]))
                    datas.append(float(row[1]))

        return times, datas

    def _load_data(self):
        dayDirs = os.listdir(self.dataPath)
        # 遍历文件计算每天的去野值后平均值

        if self.targetFields.__contains__("out_dl_field"):
            for dayDir in dayDirs:
                target = self.targetFields["out_dl_field"]
                targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
                times, datas = self._read_file(targetFilePath)
                self.outDl["x"].extend(times)
                self.outDl["y"].extend(datas)

            self._clean_data(self.outDl, self.able_out_dl[0], self.able_out_dl[1])

        if self.targetFields.__contains__("in_dl_field"):
            for dayDir in dayDirs:
                target = self.targetFields["in_dl_field"]
                targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
                times, datas = self._read_file(targetFilePath)
                self.inDl["x"].extend(times)
                self.inDl["y"].extend(datas)
            self._clean_data(self.inDl, self.able_in_dl[0], self.able_in_dl[1])

        if self.targetFields.__contains__("all_dy_field"):
            for dayDir in dayDirs:
                target = self.targetFields["all_dy_field"]
                targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
                times, datas = self._read_file(targetFilePath)
                self.allDy["x"].extend(times)
                self.allDy["y"].extend(datas)
            self._clean_data(self.allDy, self.able_all_dy[0], self.able_all_dy[1])

        minarr = []
        maxarr = []
        if len(self.outDl["x"]) > 0:
            minarr.append(self.outDl["x"][0])
            maxarr.append(self.outDl["x"][-1:][0])

        if len(self.inDl["x"]) > 0:
            minarr.append(self.inDl["x"][0])
            maxarr.append(self.inDl["x"][-1:][0])

        if len(self.allDy["x"]) > 0:
            minarr.append(self.allDy["x"][0])
            maxarr.append(self.allDy["x"][-1:][0])

        self.starttime = min(minarr)
        self.endtime = max(maxarr)
        startT = time.localtime(self.starttime)
        self.starttime = time.mktime(
            (startT.tm_year, startT.tm_mon, startT.tm_mday, self.spit_time, 0, 0, 0, startT.tm_yday, 0))

        endT = time.localtime(self.endtime)
        self.endtime = time.mktime((endT.tm_year, endT.tm_mon, endT.tm_mday, self.spit_time, 0, 0, 0, endT.tm_yday, 0))

        etime = self.starttime
        while etime < self.endtime:
            tTime = etime + self.spit_range * 3600
            obj = {"start": etime, "end": tTime, "data": {}}
            self.dayData.append(obj)
            etime = tTime
            pass
        pass

    def _split_data(self):
        fd_start_pos = 0
        cd_start_pos = 0
        dy_start_pos = 0
        for i in range(len(self.dayData)):
            progress = int(50 * i / len(self.dayData))
            percentage = 100 * i / len(self.dayData)
            progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
            print(f"\r正在处理数据... {progress_bar}", end="")
            nowDay = self.dayData[i]
            nowData = nowDay["data"]
            if self.targetFields.__contains__("out_dl_field"):
                nowData[self.targetFields["out_dl_field"]] = {"x": [], "y": []}
            if self.targetFields.__contains__("in_dl_field"):
                nowData[self.targetFields["in_dl_field"]] = {"x": [], "y": []}
            if self.targetFields.__contains__("all_dy_field"):
                nowData[self.targetFields["all_dy_field"]] = {"x": [], "y": []}

            for i in range(fd_start_pos, len(self.outDl["x"])):
                x = self.outDl["x"][i]
                y = self.outDl["y"][i]
                if x < nowDay["start"]:
                    continue
                if nowDay["start"] <= x < nowDay["end"]:
                    nowData[self.targetFields["out_dl_field"]]["x"].append(x)
                    nowData[self.targetFields["out_dl_field"]]["y"].append(y)
                    continue
                    pass
                if x >= nowDay["end"]:
                    fd_start_pos = i
                    break
                pass

            for i in range(cd_start_pos, len(self.inDl["x"])):
                x = self.inDl["x"][i]
                y = self.inDl["y"][i]
                if x < nowDay["start"]:
                    continue
                if nowDay["start"] <= x < nowDay["end"]:
                    nowData[self.targetFields["in_dl_field"]]["x"].append(x)
                    nowData[self.targetFields["in_dl_field"]]["y"].append(y)
                    continue
                    pass
                if x >= nowDay["end"]:
                    cd_start_pos = i
                    break
                pass

            for i in range(dy_start_pos, len(self.allDy["x"])):
                x = self.allDy["x"][i]
                y = self.allDy["y"][i]
                if x < nowDay["start"]:
                    continue
                if nowDay["start"] <= x < nowDay["end"]:
                    nowData[self.targetFields["all_dy_field"]]["x"].append(x)
                    nowData[self.targetFields["all_dy_field"]]["y"].append(y)
                    continue
                    pass
                if x >= nowDay["end"]:
                    dy_start_pos = i
                    break
                pass
            dataLen = []
            if len(nowData[self.targetFields["out_dl_field"]]["x"]) > 0:
                dataLen.append(len(nowData[self.targetFields["out_dl_field"]]["x"]))

            if len(nowData[self.targetFields["in_dl_field"]]["x"]) > 0:
                dataLen.append(len(nowData[self.targetFields["in_dl_field"]]["x"]))

            if len(nowData[self.targetFields["all_dy_field"]]["x"]) > 0:
                dataLen.append(len(nowData[self.targetFields["all_dy_field"]]["x"]))

            if len(dataLen) > 0:
                minlen = min(dataLen)
                if len(nowData[self.targetFields["out_dl_field"]]["x"]) > minlen:
                    nowData[self.targetFields["out_dl_field"]]["x"] = nowData[self.targetFields["out_dl_field"]]["x"][
                                                                      0:minlen]
                    nowData[self.targetFields["out_dl_field"]]["y"] = nowData[self.targetFields["out_dl_field"]]["y"][
                                                                      0:minlen]

                if len(nowData[self.targetFields["in_dl_field"]]["x"]) > minlen:
                    nowData[self.targetFields["in_dl_field"]]["x"] = nowData[self.targetFields["in_dl_field"]]["x"][
                                                                     0:minlen]
                    nowData[self.targetFields["in_dl_field"]]["y"] = nowData[self.targetFields["in_dl_field"]]["y"][
                                                                     0:minlen]

                if len(nowData[self.targetFields["all_dy_field"]]["x"]) > minlen:
                    nowData[self.targetFields["all_dy_field"]]["x"] = nowData[self.targetFields["all_dy_field"]]["x"][
                                                                      0:minlen]
                    nowData[self.targetFields["all_dy_field"]]["y"] = nowData[self.targetFields["all_dy_field"]]["y"][
                                                                      0:minlen]
            pass
        print()
        pass

    def _clean_data(self, rangeData, minSize, maxSize):
        rangeLen = len(rangeData["y"])
        for i in range(rangeLen):
            rI = i
            if rI == 0:
                rangeData["y"][rI] = minSize
            else:
                if rangeData["y"][rI] < minSize or rangeData["y"][rI] > maxSize:
                    rangeData["y"][rI] = rangeData["y"][rI - 1]
            pass
        pass

    def _getValue(self, target, field_name, pos):
        if field_name == "":
            return ""

        return target[field_name]["y"][pos]
        pass

    def dealData(self):
        posDict = {}
        if self.targetFields.__contains__("out_dl_field"):
            posDict[self.targetFields["out_dl_field"]] = 0
        if self.targetFields.__contains__("in_dl_field"):
            posDict[self.targetFields["in_dl_field"]] = 0
        if self.targetFields.__contains__("all_dy_field"):
            posDict[self.targetFields["all_dy_field"]] = 0

        for i in range(len(self.dayData)):
            progress = int(50 * i / len(self.dayData))
            percentage = 100 * i / len(self.dayData)
            progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
            print(f"\r正在分析数据... {progress_bar}", end="")
            currData = self.dayData[i]["data"]
            nowRes = {"hlRange":[],"hyRange":[],"fdRange":[],"time":0}

            lastState = ""
            nowState = []

            for p in range(len(currData[list(currData.keys())[0]]["x"])):
                x = currData[list(currData.keys())[0]]["x"][p]
                outdlVal = self._getValue(currData,self.targetFields["out_dl_field"],p)
                indlVal = self._getValue(currData, self.targetFields["in_dl_field"],p)
                alldyVal = self._getValue(currData, self.targetFields["all_dy_field"],p)

                hlRunRule = self.hlRule.strip().replace("{"+self.targetFields["out_dl_field"]+"}",str(outdlVal))
                hlRunRule = hlRunRule.replace("{" + self.targetFields["in_dl_field"] + "}", str(indlVal))
                hlRunRule = hlRunRule.replace("{" + self.targetFields["all_dy_field"] + "}", str(alldyVal))

                hyRunRule = self.hyRule.strip().replace("{"+self.targetFields["out_dl_field"]+"}",str(outdlVal))
                hyRunRule = hyRunRule.replace("{" + self.targetFields["in_dl_field"] + "}", str(indlVal))
                hyRunRule = hyRunRule.replace("{" + self.targetFields["all_dy_field"] + "}", str(alldyVal))

                fdRunRule = self.fdRule.strip().replace("{"+self.targetFields["out_dl_field"]+"}",str(outdlVal))
                fdRunRule = fdRunRule.replace("{" + self.targetFields["in_dl_field"] + "}", str(indlVal))
                fdRunRule = fdRunRule.replace("{" + self.targetFields["all_dy_field"] + "}", str(alldyVal))

                if outdlVal > 0.7:
                    dd = 00
                    pass

                if eval(hlRunRule):
                    if lastState == "":
                        nowState.append(x)
                        lastState = "hlRange"
                    else:
                        nowState.append(x)
                        nowRes[lastState].append(nowState.copy())
                        nowState = [x]
                        lastState = "hlRange"
                    pass

                elif eval(hyRunRule):
                    if lastState == "":
                        nowState.append(x)
                        lastState = "hyRange"
                    else:
                        nowState.append(x)
                        nowRes[lastState].append(nowState.copy())
                        nowState = [x]
                        lastState = "hyRange"
                    pass

                elif eval(fdRunRule):
                    if lastState == "":
                        nowState.append(x)
                        lastState = "fdRange"
                    else:
                        nowState.append(x)
                        nowRes[lastState].append(nowState.copy())
                        nowState = [x]
                        lastState = "fdRange"
                    pass
                else:
                    if lastState != "":
                        nowState.append(x)
                        nowRes[lastState].append(nowState.copy())
                        nowState = []
                        lastState = ""
                pass
            nowRes["time"] = self.dayData[i]["start"]
            self.resData.append(nowRes)
            pass
        print()
        pass


    def _sum_stat(self,target):
        totle = 0
        for i in target:
            sub = i[1] - i[0]
            totle = totle + sub
        return totle
        pass

    def _stat_data(self):
        x = []
        hl = []
        hy = []
        fd = []
        kx = []
        for r in self.resData:
            # item = {"hl":0,"hy":0,"fd":0,"kx":0}
            tmp_hl=self._sum_stat(r["hlRange"]) / (self.spit_range*3600)*100
            tmp_hy=self._sum_stat(r["hyRange"]) / (self.spit_range*3600)*100
            tmp_fd=self._sum_stat(r["fdRange"]) / (self.spit_range*3600)*100
            hl.append(tmp_hl)
            hy.append(tmp_hy)
            fd.append(tmp_fd)
            kx.append(100 - tmp_hl - tmp_hy -tmp_fd)
            x.append(r["time"])
            # res[0].append(r["time"])
            # res[1].append(item)
            pass

        res={
            "x":[item*1000 for item in x],   #todo 注：此处前端和java处是需要时间戳*1000的
            "hl":hl,
            "hy":hy,
            "fd":fd,
            "kx":kx
        }

        # print(res)
        # plt.figure(figsize=(300, 20))
        # plt.plot(np.array(x), hl, label='hl', color='red')
        # plt.plot(np.array(x), hy, label='hy', color='blue')
        # plt.plot(np.array(x), fd, label='fd', color='green')
        # plt.plot(np.array(x), kx, label='kx', color='black')
        #
        # plt.xlabel('startTime')
        # plt.ylabel('test')
        # plt.title(f"zb")
        # plt.legend()
        # plt.savefig("./电力占比new.png")
        # plt.close()

        # print(res)
        return res

    def train(self,dataPath):
        print("开始训练")
        self.dataPath = dataPath
        print("加载遥测数据...")
        self._load_data()
        print("切分遥测数据...")
        self._split_data()
        print("分析遥测数据")
        self.dealData()
        print("生成结果数据")
        ret=self._stat_data()
        print("训练完成")
        return ret

#--------------------------------------------------------BP神经网络----------------------------------------------------
#模型类
class BPBaseDeepModel(nn.Module):
    def __init__(self,inputSize,hiddenSize,kindSize):
        super().__init__()
        self.model = nn.Sequential(
            nn.Linear(inputSize, hiddenSize),
            nn.ReLU(),
            nn.Linear(hiddenSize, int(hiddenSize/2)),
            nn.ReLU(),
            nn.Linear(int(hiddenSize/2), kindSize),
            nn.LogSoftmax(dim=1)
        )
        pass

    def forward(self, x):
        return self.model(x)
        pass

class BpModel(object):
    def __init__(self,dataPath,modelPath,stdFile):
        self.trainDataFile = dataPath
        self.modelPath = modelPath
        self.stdFile = stdFile
        self.stdObj = None
        self.model = None
        self.kind = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]

        self._data_std()
        pass

    def _data_std(self):
        data = pd.read_csv(self.stdFile)
        data = data.fillna(0)
        data = data.values
        row = data.shape[0]
        feature = data[:row, :-1]
        self.stdObj = preprocessing.StandardScaler().fit(feature)
        pass

    def readData(self, filename):
        data = pd.read_csv(filename)
        data = data.fillna(method="ffill")
        data = data.dropna(how="any")
        data = data.sample(frac=1, random_state=42)
        data = data.values
        row = data.shape[0]
        feature = data[:row, :-1]
        label = data[:row, -1]

        feature = self.stdObj.transform(feature)

        return feature, label
        pass

    def train(self, trainDataDir, epochs=2000, lr=0.01):
        trainLost = []
        self.trainDataFile = trainDataDir
        train_feature, train_labels = self.readData(self.trainDataFile)

        labels = []

        for label in train_labels:
            labels.append(self.kind[int(label) - 1])
            pass

        input_size = train_feature.shape[1]
        output_size = len(self.kind)
        hidden_size = 160
        EPOCHS = epochs

        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        self.model = BPBaseDeepModel(input_size, hidden_size, output_size)
        self.model = self.model.to(device)
        cost = torch.nn.CrossEntropyLoss()
        optimizer = torch.optim.Adam(self.model.parameters(), lr=lr)

        checkLost = 9999999
        for epochs in range(EPOCHS):
            self.model.train()
            result_loss = 0

            print("\nEpoch [{}/{}]".format(epochs + 1, EPOCHS))

            traindata = torch.tensor(train_feature).float()
            trainlabel = torch.tensor(labels).float()
            traindata = traindata.to(device)
            trainlabel = trainlabel.to(device)
            out = self.model(traindata)
            # print(out)
            result_loss = cost(out, trainlabel)
            optimizer.zero_grad()
            result_loss.backward()
            optimizer.step()

            if checkLost > result_loss.item():
                checkLost = result_loss.item()
                torch.save(self.model, self.modelPath)
            trainLost.append(result_loss.item())
            print("train lost = %2f" % (result_loss.item()))

        # plt.plot(trainLost, label='lost', color='blue')
        # plt.xlabel('epoch')
        # plt.ylabel('loss')
        # plt.title(f"zb")
        # plt.legend()
        # plt.savefig("./训练损失走势图.png")
        # plt.close()
        print("save model....")

    def test(self,testDataDir):
        test_feature, test_labels = self.readData(testDataDir)


        labels = torch.tensor(test_labels).long()

        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        model = torch.load(self.modelPath, map_location=device)

        input_features = torch.tensor(test_feature, dtype=torch.float32)
        input_features = input_features.to(device)

        model.eval()
        with torch.no_grad():
            res = model(input_features)
            _,p = torch.max(res,1)
            s = p +1
            accracy = (s==labels).sum().item()/labels.size(0)

        print('accracy:'+ str(accracy*100))

    def load_model(self, modelfile):
        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        self.model = torch.load(modelfile, map_location=device)

        pass

    def forword(self, data):
        features = pd.DataFrame(data)
        features = features.fillna(method="ffill")
        # data = data.dropna(how="any")

        input_features = self.stdObj.transform(features)

        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

        input_features = torch.tensor(input_features, dtype=torch.float32)
        input_features = input_features.to(device)
        self.model.eval()
        with torch.no_grad():
            res = self.model(input_features)
            _, p = torch.max(res, 1)
            s = p + 1

        ret=s.tolist()
        final_ret=[]
        for item in ret:
            if item == 1:
                final_ret.append(4)
            if item == 2:
                final_ret.append(3)
            if item == 3:
                final_ret.append(2)
            if item == 4:
                final_ret.append(1)
        return final_ret
        pass

    pass


#--------------------------------------------------------RNN神经网络----------------------------------------------------
class MyDataSet(Dataset):
    def __init__(self, dataset, labelset):
        self.datas = dataset
        self.labels = labelset
        self.datalen = len(self.datas)
        self.labellen = len(self.labels)
        pass

    def __len__(self):
        return len(self.datas)

    def __getitem__(self, idx):
        data = self.datas[idx]
        label = self.labels[idx]
        return data, label

class RNNBaseDeepModel(nn.Module):
    def __init__(self,inputSize):
        super().__init__()

        self.rnn1 = nn.RNN(input_size=inputSize,
                           hidden_size=96,
                           batch_first=True)

        self.line1 = nn.Linear(96, 45)

        self.rnn2 = nn.RNN(input_size=45,
                           hidden_size=20,
                           batch_first=True)

        self.line2 = nn.Linear(20, 1)
        pass

    def forward(self, x):
        Y1, state_new = self.rnn1(x)
        out1 = self.line1(Y1)
        X2 = F.tanh(out1)
        out2, state_new = self.rnn2(X2)
        X3 = self.line2(out2)
        x = torch.sigmoid(X3)
        return x
        pass

class RNNDeepNetModel(object):
    def __init__(self, dataPath, modelPath,stdFile):
        self.trainDataFile = dataPath
        self.modelPath = modelPath
        self.stdObj = None
        self.model = None
        self._data_std()
        pass

    def _data_std(self):
        data = pd.read_csv(self.trainDataFile)
        features = data.drop("res", axis=1)

        train_features = np.array(features, dtype=np.float32)[0:-1]

        self.stdObj = preprocessing.StandardScaler().fit(train_features)
        pass

    def collate_fn(self,train_data):
        datas = []
        labels = []
        for data in train_data:
            datas.append(data[0])
            labels.append(data[1])
        # datas = rnn.pad_sequence(torch.tensor(datas,dtype=float), batch_first=True, padding_value=0)
        # resData = []
        # for i in range(len(datas)):
        #    resData.append((datas[i],labels[i]))
        datas = torch.tensor(datas, dtype=torch.float32, requires_grad=True)
        datas = datas.unsqueeze(1)
        return datas, torch.tensor(labels, dtype=torch.float32,
                                                                                          requires_grad=True)

    def readData(self):
        features = pd.read_csv(self.trainDataFile)
        features = features.dropna(axis=0)

        labels = np.array(features["res"])
        features = features.drop("res", axis=1)

        train_features = np.array(features, dtype=np.float32)[0:-1]
        train_labels = np.array(labels, dtype=np.float32)[0:-1]

        check_features = np.array(features, dtype=np.float32)[-1:]
        check_labels = np.array(labels, dtype=np.float32)[-1:]

        train_features = self.stdObj.transform(train_features)
        check_features = self.stdObj.transform(check_features)

        return train_features, train_labels, check_features, check_labels
        pass

    def train(self,trainDataDir,epochs=2000,batchsize=300,lr=0.01):
        self.trainDataFile = trainDataDir
        train_feature, train_labels, check_features, check_labels = self.readData()
        input_features = preprocessing.StandardScaler().fit_transform(train_feature)
        vail_features = preprocessing.StandardScaler().fit_transform(check_features)

        input_size = input_features.shape[1]
        batch_size = epochs
        EPOCHS = batchsize

        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        self.model = RNNBaseDeepModel(input_size)
        self.model = self.model.to(device)
        cost = torch.nn.MSELoss(reduction='mean')
        optimizer = torch.optim.Adam(self.model.parameters(), lr=lr)

        trainMyDataSet = MyDataSet(input_features.tolist(), train_labels)
        traindataloader = DataLoader(dataset=trainMyDataSet, batch_size=batch_size, shuffle=True, num_workers=0,
                                     drop_last=False, collate_fn=self.collate_fn)

        testMyDataSet = MyDataSet(vail_features.tolist(), check_labels)
        test_len = len(testMyDataSet)
        testdataloader = DataLoader(dataset=testMyDataSet, batch_size=batch_size, shuffle=True, num_workers=0,
                                    drop_last=False, collate_fn=self.collate_fn)

        total_step = int(len(trainMyDataSet) / batch_size)

        accuracy_rate = 0
        checkLost = 9999999
        for epochs in range(EPOCHS):
            self.model.train()
            result_loss = 0

            print("\nEpoch [{}/{}]".format(epochs + 1, EPOCHS))
            loop = 0
            train_lost = []
            for data in traindataloader:
                traindata, trainlabel = data
                traindata = traindata.to(device)
                trainlabel = trainlabel.to(device)
                out = self.model(traindata)
                print(out.reshape(-1))
                result_loss = cost(out.reshape(-1), trainlabel)
                optimizer.zero_grad()
                result_loss.backward()
                optimizer.step()
                loop = loop + 1
                if checkLost > result_loss.item():
                    checkLost = result_loss.item()
                    torch.save(self.model, self.modelPath)
                # progress(loop / total_step, result_loss.item(), loop, total_step, accuracy_rate)
                train_lost.append(result_loss.item())
            print("train lost = " + str(np.array(train_lost, dtype=np.float32).mean()))

            # 在测试集上面的效果
            self.model.eval()  # 在验证状态

            right_number = 0
            test_lost = []
            with torch.no_grad():  # 验证的部分，不是训练所以不要带入梯度
                for test_data in testdataloader:
                    testdata, testlabel = test_data
                    testdata = testdata.to(device)
                    testlabel = testlabel.to(device)
                    outputs_ = self.model(testdata)
                    outputs_ = outputs_.reshape(-1)
                    test_result_loss = cost(outputs_, testlabel)
                    test_lost.append(test_result_loss)
            print("check lost = " + str(np.array(test_lost, dtype=np.float32).mean()))
            accuracy_rate = 1
            # progress(loop / total_step, result_loss.item(), loop, total_step, accuracy_rate)

        print("save model....")

    def test(self,testDataDir):
        features = pd.read_csv(testDataDir)
        features = features.dropna(axis=0)

        #features = features.head(180)

        labels = np.array(features["res"])
        features = features.drop("res", axis=1)

        input_features = self.stdObj.transform(features)

        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        model = torch.load(self.modelPath, map_location=device)

        input_features = torch.tensor(input_features, dtype=torch.float32)
        input_features = input_features.unsqueeze(1)
        input_features = input_features.to(device)
        res = model(input_features)
        res = res.reshape(-1)

        print(res.tolist())
        print(labels.tolist())

    def load_model(self,modelfile):
        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        self.model = torch.load(modelfile, map_location=device)
        pass

    def forword(self,data):
        features = pd.DataFrame(data)
        features = features.dropna(axis=0)

        input_features = self.stdObj.transform(features)
        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

        input_features = torch.tensor(input_features, dtype=torch.float32)
        input_features = input_features.unsqueeze(1)
        input_features = input_features.to(device)
        res = self.model(input_features)
        res = res.reshape(-1)
        print(res.tolist())
        return res.tolist(),features.index.values
        pass

    pass

#--------------------------------------------------------趋势预测----------------------------------------------------
# class Qushi(object):
#     def __init__(self,dataPath,fields,starttime):
#         self.dataPath = dataPath
#         self.targetFields = fields
#         self.starttime = starttime
#         self.qushiLineDeg = 3  # 总趋势函数指数幂
#         self.rangeLineDeg = 4  # 周期你和函数指数幂
#         pass
#
#     def _data_clean(self, data):
#         subSum = []
#         avg = np.array(data).mean()
#         for i in data:
#             subSum.append(abs(i - avg))
#         avgSub = sum(subSum) / len(subSum)
#
#         dataLen = len(data)
#         for i in range(dataLen):
#             p = dataLen - i - 1
#             val = data[p]
#             if abs(val - avg) > 1 * avgSub:
#                 del data[p]
#             pass
#         pass
#
#     def csv_to_avg(self, csv_file):
#         result = []
#         with open(csv_file, 'r', encoding="utf-8") as file:
#             reader = csv.reader(file)
#             for row in reader:
#                 result.append(float(row[1]))
#         if len(result) == 0:
#             result.append(0)
#
#         self._data_clean(result)
#         # 把大于平均值3倍以上的野值赋值为平均值，通过循环加强去重效果
#         # for fix in range(0, 2):
#         #    avg = np.array(result).mean()
#         #    for i in range(0, len(result)):
#         #        if abs(result[i]) > 3 * avg:
#         #            result[i] = avg
#         #    pass
#         # df = pd.DataFrame(result)
#         # col = df.columns.values[0]
#         # mean = df[col].mean()
#         # std = df[col].std()
#         # lower_bound = mean - 3 * std
#         # upper_bound = mean + 3 * std
#         # removed_data = df[(df[col] < lower_bound) | (df[col] > upper_bound)]
#         # df = df[(df[col] >= lower_bound) & (df[col] <= upper_bound)]
#         # result = df.values.reshape(-1).tolist()
#         return np.array(result).mean()
#
#     def getNextState(self, groupCount, nowState):
#         nowState = nowState + 1
#         if nowState >= groupCount:
#             nowState = 0
#         return nowState
#
#     def getGroupRange(self, data, groupCount, fixp):
#         targetData = data[0:]
#         targetData.sort()
#         npArray = []
#         for i in range(len(targetData)):
#             npArray.append([i, targetData[i]])
#
#         model = GaussianMixture(n_components=groupCount)
#         npArray = np.array(npArray)
#         model.fit(npArray)
#         yat = model.predict(npArray)
#         clusters = unique(yat)
#         res = []
#
#         for cluster in clusters:
#             group = {"max": -9999999999999999999999, "min": 9999999999999999999}
#             row_ix = np.where(yat == cluster)
#             row_list = row_ix[0].tolist()
#             if len(row_list) / len(targetData) < fixp:
#                 continue
#
#             for i in row_list:
#                 if npArray[i][1] > group["max"]:
#                     group["max"] = npArray[i][1]
#                 if npArray[i][1] < group["min"]:
#                     group["min"] = npArray[i][1]
#             res.append(group)
#
#         # l = len(res)
#         # for i in range(l):
#         #    ri = l - i -1
#         #    if ri ==0:
#         #        break
#         #    n = res[ri]
#         #    s = res[ri -1]
#         #    if n["max"] == s["max"] and n["min"]==s["min"]:
#         #        del res[ri]
#         return res
#
#         pass
#
#     def _fing_list_max(self, data):
#         val = -9999999999999999
#         pos = 0
#         for i in range(len(data)):
#             if data[i] > val:
#                 val = data[i]
#                 pos = i
#             pass
#         return val, pos
#         pass
#
#     def dealData(self,featureDay, groupCount, fixp,bline =False):
#         try:
#             # self.targetFields[0] = field
#             resData = {}
#
#             # plt.figure(figsize=(100, 20))
#             count = 0
#             for target in self.targetFields:
#                 resData[target] = {"total": [], "group": [], "groupdata": [], "grouprange": []}
#
#             dayDirs = os.listdir(self.dataPath)
#             # 遍历文件计算每天的去野值后平均值
#             for dayDir in dayDirs:
#                 for target in self.targetFields:
#                     targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
#                     targetAvg = self.csv_to_avg(targetFilePath)
#                     resData[target]["total"].append(targetAvg)
#
#             for target in self.targetFields:
#                 targetGroup = self.getGroupRange(resData[target]["total"], groupCount, fixp)
#                 for group in targetGroup:
#                     resData[target]["group"].append(group)
#                     resData[target]["groupdata"].append({"x": [], "y": []})
#                     resData[target]["grouprange"].append({"rangecount": [], "rangedata": []})
#
#             lastGroup = -1
#
#             # 根据每天平均值分类当前状态是属于top还是down,并保存下标和值
#             for key in resData.keys():
#                 vals = resData[key]["total"]
#                 rangedata = []
#                 currentGroup = 0
#                 for i in range(0, len(vals)):
#                     for g in range(len(resData[key]["group"])):
#                         if vals[i] >= resData[key]["group"][g]["min"] and vals[i] <= resData[key]["group"][g]["max"]:
#                             resData[key]["groupdata"][g]["x"].append(i)
#                             resData[key]["groupdata"][g]["y"].append(vals[i])
#                             currentGroup = g
#                             rangedata.append(vals[i])
#                             count = count + 1
#                             break
#                         pass
#
#                     if lastGroup != currentGroup:
#                         if lastGroup == -1:
#                             lastGroup = currentGroup
#                             continue
#                         nowval = rangedata[-1:]
#                         resData[key]["grouprange"][lastGroup]["rangecount"].append(count - 1)
#                         resData[key]["grouprange"][lastGroup]["rangedata"].append(rangedata[0:-1].copy())
#                         count = 1
#                         lastGroup = currentGroup
#                         rangedata.clear()
#                         rangedata.extend(nowval)
#
#                 if currentGroup == currentGroup:
#                     resData[key]["grouprange"][lastGroup]["rangecount"].append(count)
#                     resData[key]["grouprange"][lastGroup]["rangedata"].append(rangedata.copy())
#
#                 pass
#
#
#             ret = {}
#
#             for field in self.targetFields:
#                 # plt.figure(figsize=(100, 20))
#                 groupFun = []
#                 gid = 0
#                 groupdatalen = len(resData[field]["groupdata"])
#                 for i in range(groupdatalen):
#                     ri = groupdatalen - i - 1
#                     groupData = resData[field]["groupdata"][ri]
#                     if len(groupData["x"]) == 0:
#                         del resData[field]["groupdata"][ri]
#                         del resData[field]["group"][ri]
#                         del resData[field]["grouprange"][ri]
#
#                 for groupdata in resData[field]["groupdata"]:
#                     fun = p.polynomial.Polynomial.fit(groupdata["x"], groupdata["y"], deg=self.qushiLineDeg)
#                     pY = fun(np.array(groupdata["x"]))
#                     # plt.plot(groupdata["x"], pY, label=field + str(gid) + "_line")
#
#                     groupFun.append(fun)
#
#                     # plt.scatter(groupdata["x"], groupdata["y"], label=field + str(gid))
#
#                     gid = gid + 1
#                     pass
#
#                 groupRangeAvg = []
#                 groupRangeIndex = []
#                 rangeFun = []
#                 for groupRange in resData[field]["grouprange"]:
#                     if len(groupRange["rangecount"]) == 0:
#                         groupRange["rangecount"].append(0)
#
#                     val, pos = self._fing_list_max(groupRange["rangecount"])
#                     groupRangeAvg.append(val)
#                     groupRangeIndex.append(pos)
#                     nx = []
#                     for i in range(val):
#                         nx.append(i)
#                     fun = p.polynomial.Polynomial.fit(np.array(nx), groupRange["rangedata"][pos], deg=10)
#                     rangeFun.append(fun)
#
#                 # 取最后一个周期的数据和周期索引
#                 lastList = []
#                 lastState = 0
#                 lastX = 0
#                 for i in range(len(resData[field]["groupdata"])):
#                     if lastX < max(resData[field]["groupdata"][i]["x"]):
#                         lastState = i
#                         lastX = max(resData[field]["groupdata"][i]["x"])
#                         lastList = resData[field]["groupdata"][i]["x"]
#                     pass
#
#                 lastStateStartX = 0
#                 lastStateCount = 0  # 最后一个状态周期到周期完成的剩余天数
#                 # 取样本最有一天所在状态和所处状态相对位置，用于估算未来目标的状态
#                 for i in range(len(lastList) - 1, -1, -1):
#                     lastStateCount = lastStateCount + 1
#                     lastStateStartX = lastList[i]
#                     if lastStateStartX - 1 != lastList[i - 1]:
#                         break
#
#                 featureX = []
#                 featureY = []
#                 rangePos = lastStateCount
#                 lastX = max(lastList)  # 真是数据最后一条数据的天数
#                 featureState = lastState
#                 for i in range(featureDay):
#                     featureX.append(lastX + i + 1)  # 预测数据的天索引
#                     # 未来目标X为样本组大值+未来天数
#                     featureXVal = lastX + i + 1
#                     rangePos = rangePos + 1
#                     groupEndX = lastStateStartX + groupRangeAvg[featureState]
#                     if groupEndX < featureXVal:
#                         lastStateStartX = groupEndX
#                         featureState = self.getNextState(len(resData[field]["group"]), featureState)
#                         rangePos = 0
#
#                     if bline:
#                         featureY.append(groupFun[featureState](np.array([featureXVal]))[0])
#                     else:
#                         featureY.append(rangeFun[featureState](np.array([rangePos]))[0])
#
#                 # featureX = []
#                 # featureY = []
#                 # maxX = max(lastList)
#                 # for i in range(featureDay):
#                 #     featureX.append(len(resData[field]["total"]) + i)
#                 #     # 未来目标X为样本组大值+未来天数
#                 #     featureXVal = len(resData[field]["total"]) + i
#                 #
#                 #     featureState = lastState
#                 #     nowX = max(lastList)
#                 #
#                 #     nowX = nowX + groupRangeAvg[featureState] - lastStateCount
#                 #     if nowX > featureXVal:
#                 #         featureState = lastState
#                 #     else:
#                 #         featureState = self.getNextState(len(resData[field]["group"]), featureState)
#                 #         while nowX < featureXVal:
#                 #             nowX = nowX + groupRangeAvg[featureState]
#                 #             if nowX > featureXVal:
#                 #                 break
#                 #             else:
#                 #                 featureState = self.getNextState(len(resData[field]["group"]), featureState)
#                 #
#                 #     featureY.append(groupFun[featureState](np.array([featureXVal]))[0])
#
#                 # plt.plot(featureX, featureY, label='feature')
#                 # plt.legend()
#                 # plt.savefig(f"./{field}趋势.png")
#                 # plt.close()
#                 startstamp = timeToStamp(self.starttime)
#                 featureX = [ stampToTime(j*86400 + startstamp) for j in featureX]
#
#                 ret[field] = {
#                     "x":featureX,
#                     "y":featureY
#                 }
#
#             return ret
#         except Exception as e:
#             print(e.args)
#         pass
#     pass

class Qushi(object):
    def __init__(self,dataPath,fields,starttime):
        self.dataPath = dataPath
        self.targetFields = fields
        self.starttime = starttime
        self.qushiLineDeg = 3  # 总趋势函数指数幂
        self.rangeLineDeg = 4  # 周期你和函数指数幂
        pass

    def _data_clean(self, data):
        subSum = []
        avg = np.array(data).mean()
        for i in data:
            subSum.append(abs(i - avg))
        avgSub = sum(subSum) / len(subSum)

        dataLen = len(data)
        for i in range(dataLen):
            p = dataLen - i - 1
            val = data[p]
            if abs(val - avg) > 1 * avgSub:
                del data[p]
            pass
        pass

    def csv_to_avg(self, csv_file):
        result = []
        with open(csv_file, 'r', encoding="utf-8") as file:
            reader = csv.reader(file)
            for row in reader:
                result.append(float(row[1]))
        if len(result) == 0:
            result.append(0)

        self._data_clean(result)
        # 把大于平均值3倍以上的野值赋值为平均值，通过循环加强去重效果
        # for fix in range(0, 2):
        #    avg = np.array(result).mean()
        #    for i in range(0, len(result)):
        #        if abs(result[i]) > 3 * avg:
        #            result[i] = avg
        #    pass
        # df = pd.DataFrame(result)
        # col = df.columns.values[0]
        # mean = df[col].mean()
        # std = df[col].std()
        # lower_bound = mean - 3 * std
        # upper_bound = mean + 3 * std
        # removed_data = df[(df[col] < lower_bound) | (df[col] > upper_bound)]
        # df = df[(df[col] >= lower_bound) & (df[col] <= upper_bound)]
        # result = df.values.reshape(-1).tolist()
        # print(np.array(result).mean(),np.array(result).max(),np.array(result).min(),
        #       (0.75*np.array(result).max()+0.25*np.array(result).min()),(0.25*np.array(result).max()+0.75*np.array(result).min()))
        return np.array(result).mean(), np.array(result).max(), np.array(result).min(), (
                    0.75 * np.array(result).max() + 0.25 * np.array(result).min()), (
                           0.25 * np.array(result).max() + 0.75 * np.array(result).min())

    def getNextState(self, groupCount, nowState):
        nowState = nowState + 1
        if nowState >= groupCount:
            nowState = 0
        return nowState

    def getGroupRange(self, data, groupCount, fixp):
        targetData = data[0:]
        targetData.sort()
        npArray = []
        for i in range(len(targetData)):
            npArray.append([i, targetData[i]])

        model = GaussianMixture(n_components=groupCount)
        npArray = np.array(npArray)
        model.fit(npArray)
        yat = model.predict(npArray)
        clusters = unique(yat)
        res = []

        for cluster in clusters:
            group = {"max": -9999999999999999999999, "min": 9999999999999999999}
            row_ix = np.where(yat == cluster)
            row_list = row_ix[0].tolist()
            if len(row_list) / len(targetData) < float(fixp):
                continue

            for i in row_list:
                if npArray[i][1] > group["max"]:
                    group["max"] = npArray[i][1]
                if npArray[i][1] < group["min"]:
                    group["min"] = npArray[i][1]
            res.append(group)

        # l = len(res)
        # for i in range(l):
        #    ri = l - i -1
        #    if ri ==0:
        #        break
        #    n = res[ri]
        #    s = res[ri -1]
        #    if n["max"] == s["max"] and n["min"]==s["min"]:
        #        del res[ri]
        return res

        pass

    def _fing_list_max(self, data):
        val = -9999999999999999
        pos = 0
        for i in range(len(data)):
            if data[i] > val:
                val = data[i]
                pos = i
            pass
        return val, pos
        pass

    def cal_res(self, featureDay, groupCount, fixp, numi, bline=False):
        resData = {}

        # plt.figure(figsize=(100, 20))
        count = 0
        for target in self.targetFields:
            resData[target] = {"total": [], "group": [], "groupdata": [], "grouprange": []}

        dayDirs = os.listdir(self.dataPath)
        # 遍历文件计算每天的去野值后平均值
        for dayDir in dayDirs:
            for target in self.targetFields:
                targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
                targetAvg = self.csv_to_avg(targetFilePath)[numi]
                resData[target]["total"].append(targetAvg)

        for target in self.targetFields:
            targetGroup = self.getGroupRange(resData[target]["total"], groupCount, fixp)
            for group in targetGroup:
                resData[target]["group"].append(group)
                resData[target]["groupdata"].append({"x": [], "y": []})
                resData[target]["grouprange"].append({"rangecount": [], "rangedata": []})

        lastGroup = -1

        # 根据每天平均值分类当前状态是属于top还是down,并保存下标和值
        for key in resData.keys():
            vals = resData[key]["total"]
            rangedata = []
            currentGroup = 0
            for i in range(0, len(vals)):
                for g in range(len(resData[key]["group"])):
                    if vals[i] >= resData[key]["group"][g]["min"] and vals[i] <= resData[key]["group"][g]["max"]:
                        resData[key]["groupdata"][g]["x"].append(i)
                        resData[key]["groupdata"][g]["y"].append(vals[i])
                        currentGroup = g
                        rangedata.append(vals[i])
                        count = count + 1
                        break
                    pass

                if lastGroup != currentGroup:
                    if lastGroup == -1:
                        lastGroup = currentGroup
                        continue
                    nowval = rangedata[-1:]
                    resData[key]["grouprange"][lastGroup]["rangecount"].append(count - 1)
                    resData[key]["grouprange"][lastGroup]["rangedata"].append(rangedata[0:-1].copy())
                    count = 1
                    lastGroup = currentGroup
                    rangedata.clear()
                    rangedata.extend(nowval)

            if currentGroup == currentGroup:
                resData[key]["grouprange"][lastGroup]["rangecount"].append(count)
                resData[key]["grouprange"][lastGroup]["rangedata"].append(rangedata.copy())

            pass

        ret = {}

        for field in self.targetFields:
            # plt.figure(figsize=(100, 20))
            groupFun = []
            gid = 0
            groupdatalen = len(resData[field]["groupdata"])
            for i in range(groupdatalen):
                ri = groupdatalen - i - 1
                groupData = resData[field]["groupdata"][ri]
                if len(groupData["x"]) == 0:
                    del resData[field]["groupdata"][ri]
                    del resData[field]["group"][ri]
                    del resData[field]["grouprange"][ri]

            for groupdata in resData[field]["groupdata"]:
                fun = p.polynomial.Polynomial.fit(groupdata["x"], groupdata["y"], deg=self.qushiLineDeg)
                pY = fun(np.array(groupdata["x"]))
                # plt.plot(groupdata["x"], pY, label=field + str(gid) + "_line")

                groupFun.append(fun)

                # plt.scatter(groupdata["x"], groupdata["y"], label=field + str(gid))

                gid = gid + 1
                pass

            groupRangeAvg = []
            groupRangeIndex = []
            rangeFun = []
            for groupRange in resData[field]["grouprange"]:
                if len(groupRange["rangecount"]) == 0:
                    groupRange["rangecount"].append(0)

                val, pos = self._fing_list_max(groupRange["rangecount"])
                groupRangeAvg.append(val)
                groupRangeIndex.append(pos)
                nx = []
                for i in range(val):
                    nx.append(i)
                fun = p.polynomial.Polynomial.fit(np.array(nx), groupRange["rangedata"][pos], deg=10)
                rangeFun.append(fun)

            # 取最后一个周期的数据和周期索引
            lastList = []
            lastState = 0
            lastX = 0
            for i in range(len(resData[field]["groupdata"])):
                if lastX < max(resData[field]["groupdata"][i]["x"]):
                    lastState = i
                    lastX = max(resData[field]["groupdata"][i]["x"])
                    lastList = resData[field]["groupdata"][i]["x"]
                pass

            lastStateStartX = 0
            lastStateCount = 0  # 最后一个状态周期到周期完成的剩余天数
            # 取样本最有一天所在状态和所处状态相对位置，用于估算未来目标的状态
            for i in range(len(lastList) - 1, -1, -1):
                lastStateCount = lastStateCount + 1
                lastStateStartX = lastList[i]
                if lastStateStartX - 1 != lastList[i - 1]:
                    break

            featureX = []
            featureY = []
            rangePos = lastStateCount
            lastX = max(lastList)  # 真是数据最后一条数据的天数
            featureState = lastState
            for i in range(featureDay):
                featureX.append(lastX + i + 1)  # 预测数据的天索引
                # 未来目标X为样本组大值+未来天数
                featureXVal = lastX + i + 1
                rangePos = rangePos + 1
                groupEndX = lastStateStartX + groupRangeAvg[featureState]
                if groupEndX < featureXVal:
                    lastStateStartX = groupEndX
                    featureState = self.getNextState(len(resData[field]["group"]), featureState)
                    rangePos = 0

                if bline:
                    featureY.append(groupFun[featureState](np.array([featureXVal]))[0])
                else:
                    featureY.append(rangeFun[featureState](np.array([rangePos]))[0])

            startstamp = timeToStamp(self.starttime)
            featureX = [stampToTime(j * 86400 + startstamp) for j in featureX]
            ret[field] = {
                "x": featureX,
                "y": featureY,
            }
            return ret

    def dealData(self, featureDay, groupCount, fixp, bline=False):
        # try:
        # self.targetFields[0] = field

        retAvg = self.cal_res(featureDay, groupCount, fixp, bline=False, numi=0)
        retMax = self.cal_res(featureDay, groupCount, fixp, bline=False, numi=1)
        retMin = self.cal_res(featureDay, groupCount, fixp, bline=False, numi=2)
        retMax_075 = self.cal_res(featureDay, groupCount, fixp, bline=False, numi=3)
        retMax_025 = self.cal_res(featureDay, groupCount, fixp, bline=False, numi=4)

        ret = {}
        for field in self.targetFields:
            MaxPre = []
            MinPre = []
            for i in range(len(retMax[field]["y"])):
                MaxPre.append(
                    max(retMax[field]["y"][i], retMin[field]["y"][i],retAvg[field]["y"][i], retMax_075[field]["y"][i], retMax_025[field]["y"][i]))
                MinPre.append(
                    min(retMax[field]["y"][i], retMin[field]["y"][i],retAvg[field]["y"][i], retMax_075[field]["y"][i], retMax_025[field]["y"][i]))
            ret[field] = {
                "x": retAvg[field]["x"],
                "y": retAvg[field]["y"],
                "yMax": MaxPre,
                "yMin": MinPre
            }

        return ret
        # except Exception as e:
        #     return e.args


# class Qushi2(object):
#     def __init__(self,dataPath,modelPath,tel,rel_tel):
#         self.maxCurrent = 40
#         self.splitCurrent = 20
#         self.dataPath = dataPath
#         self.modelPath = modelPath
#         self.tel = tel,
#         self.rel_tel = rel_tel,
#         self.dealData = {}
#         [self.dealData.update({item:{"light":{"fun":None},"night":{"fun":None},"data":[],"code":item}})  for item in tel]
#         # self.dealData["1034"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1034"}
#         # self.dealData["1035"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1035"}
#         # self.dealData["1030"]= {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1030"}
#         # self.dealData["1031"]= {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1031"}
#         # self.dealData["1040"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1040"}
#         # self.dealData["1041"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1041"}
#         # self.dealData["1043"]= {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1043"}
#         # self.dealData["1162"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1162"}
#         # self.dealData["1164"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1164"}
#         # self.dealData["9216"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9216"}
#         # self.dealData["9222"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9222"}
#         # self.dealData["9226"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9226"}
#         # self.dealData["9230"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9230"}
#         # self.dealData["9565"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9565"}
#         # self.dealData["9570"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9570"}
#
#         self.currentData = {rel_tel:{"data":[],"code":rel_tel}}
#         # self.currentData["1026"] = {"data":[],"code":"1026"}
#         # self.currentData["1027"] = {"data":[],"code":"1027"}
#
#         self.bestNightRange = []
#         self.bestLightRange = []
#
#         self.nightRange = []
#         self.lightRange = []
#         pass
#
#     def _save_model(self):
#         res = {}
#         for field in self.dealData.keys():
#             deal = self.dealData[field]
#             if deal["light"]["fun"] is None or deal["night"]["fun"] is None:
#                 continue
#             saveMode = {
#                 "light": {"coef": None, "domain": None, "window": None},
#                 "night": {"coef": None, "domain": None, "window": None},
#             }
#             if not deal["light"]["fun"] is None:
#                 saveMode["light"]["coef"] = deal["light"]["fun"].coef.tolist()
#                 saveMode["light"]["domain"] = deal["light"]["fun"].domain.tolist()
#                 saveMode["light"]["window"] = deal["light"]["fun"].window.tolist()
#                 pass
#             if not deal["night"]["fun"] is None:
#                 saveMode["night"]["coef"] = deal["night"]["fun"].coef.tolist()
#                 saveMode["night"]["domain"] = deal["night"]["fun"].domain.tolist()
#                 saveMode["night"]["window"] = deal["night"]["fun"].window.tolist()
#                 pass
#             res[field] = saveMode
#         res["bestLightRange"] = self.bestLightRange
#         res["bestNightRange"] = self.bestNightRange
#         res["nightRange"] = self.nightRange
#         res["lightRange"] = self.lightRange
#         modelStr = json.dumps(res)
#         with open(self.modelPath,"w") as f:
#             f.write(modelStr)
#         pass
#
#     def load_model(self,model_file):
#         print(f"加载模型文件 {model_file}")
#         if not os.path.isfile(model_file):
#             print(f"模型文件 {model_file} 不存在")
#             return
#         with open(model_file,"r") as f:
#             modelStr = f.read()
#             models = json.loads(modelStr)
#
#             for field in models.keys():
#                 model = models[field]
#                 if field == "bestLightRange":
#                     self.bestLightRange = model
#                     continue
#                 if field == "bestNightRange":
#                     self.bestNightRange = model
#                     continue
#                 if field == "nightRange":
#                     self.nightRange = model
#                     continue
#                 if field == "lightRange":
#                     self.lightRange = model
#                     continue
#
#                 self.dealData[field]["light"]["fun"] = p.polynomial.Polynomial(model["light"]["coef"], model["light"]["domain"], model["light"]["window"])
#                 self.dealData[field]["night"]["fun"] = p.polynomial.Polynomial(model["night"]["coef"],
#                                                                                model["night"]["domain"],
#                                                                                model["night"]["window"])
#                 pass
#         pass
#
#     def _read_file(self, csv_file, b=False):
#         times = []
#         datas = []
#         with open(csv_file, 'r', encoding="utf-8") as file:
#             reader = csv.reader(file)
#             for row in reader:
#                 if b:
#                     if float(row[1]) >= self.maxCurrent:
#                         continue
#                 times.append(int(row[0]))
#                 datas.append(float(row[1]))
#
#         if len(times) > 0 and b == False:
#             self._data_clean(times, datas)
#
#         return times, datas
#
#     def _load_current_data(self,target):
#         print(f"加载电流{target}数据...")
#         dayDirs = os.listdir(self.dataPath)
#
#         time = []
#         val = []
#         for dayDir in dayDirs:
#             targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
#             times, datas = self._read_file(targetFilePath,True)
#             time.extend(times)
#             val.extend(datas)
#         self.currentData[target]["data"].append(time)
#         self.currentData[target]["data"].append(val)
#         pass
#
#     def _load_deal_data(self,target):
#         print(f"加载{target}数据...")
#         dayDirs = os.listdir(self.dataPath)
#         time = []
#         val = []
#
#         for dayDir in dayDirs:
#             targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
#             times, datas = self._read_file(targetFilePath)
#             time.extend(times)
#             val.extend(datas)
#         self.dealData[target]["data"].append(time)
#         self.dealData[target]["data"].append(val)
#         pass
#
#     #判断24小时内数据是否有大于2A电流
#     def _check24_hour_current_big_2a(self,target,startPos):
#         # 根据轴取开始
#         starttime = self.currentData[target]["data"][0][startPos]
#         endtime = starttime + 24 * 3600
#         currentTime = starttime
#         nowPos = startPos
#         while currentTime < endtime:
#             y = self.currentData[target]["data"][1][nowPos]
#             if y >self.splitCurrent:
#                 return True,nowPos
#             nowPos = nowPos +1
#             if nowPos >= len(self.currentData[target]["data"][0]):
#                 return False,nowPos
#             currentTime = self.currentData[target]["data"][0][nowPos]
#         return False,nowPos
#         pass
#
#     def _find_first_light_start(self,target):
#         print("分析第一个光照期")
#         xlist = self.currentData[target]["data"][0]
#         ylist = self.currentData[target]["data"][1]
#
#         nowPos = 0
#         for i in range(len(xlist)):
#             x = xlist[i]
#             y = ylist[i]
#             if y > self.splitCurrent:
#                 continue
#             if i<nowPos:
#                 continue
#             b,nowPos = self._check24_hour_current_big_2a(target,i)
#             if not b:
#                 return i,x
#         pass
#
#     def _parse_current_range(self,target):
#         print("分析电流数据周期...")
#         xlist = self.currentData[target]["data"][0]
#         ylist = self.currentData[target]["data"][1]
#         nightRange = []
#         lightRange = []
#         nowState = 1  # 1光照 0地影
#         startPos,startTime = self._find_first_light_start(target)
#         nowPos = 0
#         print("\n")
#         for i in range(startPos,len(xlist)):
#             progress = int(50 * i / len(xlist))
#             percentage = 100 * i / len(xlist)
#             progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
#             print(f"\r正在处理数据... {progress_bar}", end="")
#
#             x = xlist[i]
#             y = ylist[i]
#             if nowState == 1:
#                 if y > self.splitCurrent:
#                     nowState = 0
#                     lightRange.append([startTime,xlist[i-1]])
#                     startTime = x
#             else:
#                 if i < nowPos:
#                     continue
#                 b, nowPos = self._check24_hour_current_big_2a(target, i)
#                 if not b:
#                     nowState = 1
#                     nightRange.append([startTime,xlist[i-1]])
#                     startTime = x
#                     pass
#             pass
#         print("\n")
#         return nightRange,lightRange
#         pass
#
#     #获取最佳周期，最佳周期为最长周期，默认最长周期具有完整特征
#     def _get_best_range(self,timeRange):
#         res = []
#         split = 0
#         for i in timeRange:
#             if i[1] - i[0] > split:
#                 split = i[1] - i[0]
#                 res = i
#
#         return res
#         pass
#
#     def _fit_deal_fun(self,target,nightRange,lightRange):
#         xlist = self.dealData[target]["data"][0]
#         ylist = self.dealData[target]["data"][1]
#         xn = []
#         yn = []
#         xl = []
#         yl = []
#
#         # 取光照和地影样本数据
#         for i in range(len(xlist)):
#             progress = int(50 * i / len(xlist))
#             percentage = 100 * i / len(xlist)
#             progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
#             print(f"\r正在处理数据... {progress_bar}", end="")
#             x = xlist[i]
#             y = ylist[i]
#             if nightRange[0] <= x <= nightRange[1]:
#                 xn.append(x)
#                 yn.append(y)
#             if lightRange[0] <= x <= lightRange[1]:
#                 xl.append(x)
#                 yl.append(y)
#         print("\n")
#         fun = p.polynomial.Polynomial.fit(xl, yl, deg=4)
#         x = []
#         y = []
#
#         lightRangeDays = lightRange[1] - lightRange[0]
#         for i in range(0, lightRangeDays, 5):
#             x.append(i)
#             y.append(fun(np.array([lightRange[0] + i]))[0])
#             pass
#
#         self.dealData[target]["light"]["fun"] = p.polynomial.Polynomial.fit(x, y, deg=8)
#
#         fun = p.polynomial.Polynomial.fit(xn, yn, deg=4)
#         x = []
#         y = []
#         nightRangeDay = nightRange[1] - nightRange[0]
#         for i in range(0, nightRangeDay, 5):
#             x.append(i)
#             y.append(fun(np.array([nightRange[0] + i]))[0])
#             pass
#
#         self.dealData[target]["night"]["fun"] = p.polynomial.Polynomial.fit(x, y, deg=8)
#
#         pass
#
#     def train(self,datapath):
#         self.dataPath = datapath
#         self._load_current_data(self.rel_tel[0])
#         self.nightRange,self.lightRange = self._parse_current_range(self.rel_tel[0])
#         self.bestNightRange = self._get_best_range(self.nightRange)
#         self.bestLightRange = self._get_best_range(self.lightRange)
#
#         for field in self.dealData.keys():
#             self._load_deal_data(field)
#             self._fit_deal_fun(field,self.bestNightRange,self.bestLightRange)
#
#         self._save_model()
#         pass
#
#     def _split_feature_time_range(self,starttime,daysNum,step,nightRange,lightRange,nightFun,lightFun):
#         end_time = starttime+ daysNum*24*3600
#         nightDays = nightRange[1] - nightRange[0]
#         lightDays = lightRange[1] - lightRange[0]
#         subDay = nightDays + lightDays
#         lastTime= 0
#         lastStatue = 0 #0 地影 1 光照
#         if nightRange[1] > lightRange[1]:
#             lastTime = nightRange[1]
#             lastStatue= 0
#         else:
#             lastTime = lightRange[1]
#             lastStatue = 1
#
#         x = []
#         y = []
#         for i in range(starttime,end_time,step):
#             yuDay = (i - lastTime) % subDay
#             if lastStatue == 0:
#                 if yuDay<=lightDays:
#                     val = lightFun(np.array([yuDay]))[0]
#                     x.append(i)
#                     y.append(val)
#                 else:
#                     val = nightFun(np.array([yuDay-lightDays]))[0]
#                     x.append(i)
#                     y.append(val)
#                 pass
#
#             if lastStatue == 1:
#                 if yuDay<=nightDays:
#                     val = nightDays(np.array([yuDay]))[0]
#                     x.append(i)
#                     y.append(val)
#                 else:
#                     val = lightFun(np.array([yuDay-nightDays]))[0]
#                     x.append(i)
#                     y.append(val)
#                 pass
#             pass
#         x=[stampToTime(item) for item in x]
#         return x,y
#         pass
#
#     def forword(self,starttime,daysNum,step = 5):
#         res = {}
#         for field in self.dealData.keys():
#             deal = self.dealData[field]
#             if deal["light"]["fun"] is None or deal["night"]["fun"] is None:
#                 continue
#             x,y = self._split_feature_time_range(timeToStamp(starttime),daysNum,step,self.bestNightRange,self.bestLightRange,
#                                                  deal["night"]["fun"],deal["light"]["fun"])
#             res[field]= {"x":x,"y":y}
#             pass
#         return res
#         pass
#
#     def draw_pic(self,target):
#         print(f"构建{target}分析图")
#         # plt.figure(figsize=(200, 20))
#         xList = []
#         yList = []
#         if self.dealData.__contains__(target):
#             if len(self.dealData[target]["data"]) == 0:
#                 self._load_deal_data(target)
#             xList = self.dealData[target]["data"][0]
#             yList = self.dealData[target]["data"][1]
#             pass
#         else:
#             if len(self.currentData[target]["data"]) == 0:
#                 self._load_current_data(target)
#             xList = self.currentData[target]["data"][0]
#             yList = self.currentData[target]["data"][1]
#
#
#         xl = []
#         yl = []
#         xn = []
#         yn = []
#
#         for i in range(len(xList)):
#             progress = int(50 * i / len(xList))
#             percentage = 100 * i / len(xList)
#             progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
#             print(f"\r正在处理数据... {progress_bar}", end="")
#             x = xList[i]
#             y = yList[i]
#
#             for r in self.nightRange:
#                 if r[0] <= x <= r[1]:
#                     xn.append(x)
#                     yn.append(y)
#             for r in self.lightRange:
#                 if r[0] <= x <= r[1]:
#                     xl.append(x)
#                     yl.append(y)
#             pass
#
#         plt.scatter(np.array(xn), np.array(yn), label='night', color='red')
#         plt.scatter(np.array(xl), np.array(yl), label='light', color='blue')
#         plt.xlabel('Time')
#         plt.ylabel('val')
#         plt.title(f"./{target}地影光照趋势周期分析图.png")
#         plt.legend()
#         plt.savefig(f"./{target}地影光照趋势周期分析图.png")
#         plt.close()
#         print('\n构图完成')
#         pass


class Qushi2(object):
    def __init__(self, dataPath,modelPath,tel, rel_tel):
        self.maxCurrent = 40
        self.splitCurrent = 20
        # self.splitCurrentV = 87.2
        self.bigger = None
        self.smaller = None
        self.dataPath = dataPath
        self.modelPath = modelPath
        self.tel = tel
        self.rel_tel = rel_tel
        self.dealData = {}
        [self.dealData.update({item: {"light": {"fun": None}, "night": {"fun": None}, "data": [], "code": item}}) for
         item in tel]
        # self.dealData["1034"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1034","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1035"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1035","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1030"]= {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1030","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1031"]= {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1031","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1040"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1040","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1041"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1041","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1043"]= {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1043","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1162"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1162","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1164"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"1164","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["9216"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9216","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["9222"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9222","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["9226"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9226","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["9230"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9230","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["9565"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9565","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["9570"] = {"light":{"fun":None},"night":{"fun":None},"data":[],"code":"9570","nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None}
        # self.dealData["1026"] = {"light": {"fun": None}, "night": {"fun": None}, "data": [], "code": "1026",
        #                          "nmaxfun": None, "nminfun": None, "lmaxfun": None, "lminfun": None}
        # self.dealData["1027"] = {"light": {"fun": None}, "night": {"fun": None}, "data": [], "code": "1027",
        #                          "nmaxfun": None, "nminfun": None, "lmaxfun": None, "lminfun": None}

        self.currentData = {}
        self.currentData = {rel_tel: {"data": [], "code": rel_tel}}
        # self.currentData["1026"] = {"data":[],"code":"1026"}
        # self.currentData["1030"] = {"data": [], "code": "1030"}

        self.bestNightRange = []
        self.bestLightRange = []

        self.nightRange = []
        self.lightRange = []
        pass

    def _save_model(self):
        res = {}
        for field in self.dealData.keys():
            deal = self.dealData[field]
            if deal["light"]["fun"] is None or deal["night"]["fun"] is None:
                continue
            saveMode = {
                "light": {"coef": None, "domain": None, "window": None},
                "night": {"coef": None, "domain": None, "window": None},
                "nmaxfun": {"coef": None, "domain": None, "window": None},
                "nminfun": {"coef": None, "domain": None, "window": None},
                "lmaxfun": {"coef": None, "domain": None, "window": None},
                "lminfun": {"coef": None, "domain": None, "window": None}
            }
            if not deal["light"]["fun"] is None:
                saveMode["light"]["coef"] = deal["light"]["fun"].coef.tolist()
                saveMode["light"]["domain"] = deal["light"]["fun"].domain.tolist()
                saveMode["light"]["window"] = deal["light"]["fun"].window.tolist()

            if not deal["night"]["fun"] is None:
                saveMode["night"]["coef"] = deal["night"]["fun"].coef.tolist()
                saveMode["night"]["domain"] = deal["night"]["fun"].domain.tolist()
                saveMode["night"]["window"] = deal["night"]["fun"].window.tolist()


            if not deal["nmaxfun"] is None:
                saveMode["nmaxfun"]["coef"] = deal["nmaxfun"].coef.tolist()
                saveMode["nmaxfun"]["domain"] = deal["nmaxfun"].domain.tolist()
                saveMode["nmaxfun"]["window"] = deal["nmaxfun"].window.tolist()

            if not deal["nminfun"] is None:
                saveMode["nminfun"]["coef"] = deal["nminfun"].coef.tolist()
                saveMode["nminfun"]["domain"] = deal["nminfun"].domain.tolist()
                saveMode["nminfun"]["window"] = deal["nminfun"].window.tolist()

            if not deal["lmaxfun"] is None:
                saveMode["lmaxfun"]["coef"] = deal["lmaxfun"].coef.tolist()
                saveMode["lmaxfun"]["domain"] = deal["lmaxfun"].domain.tolist()
                saveMode["lmaxfun"]["window"] = deal["lmaxfun"].window.tolist()

            if not deal["lminfun"] is None:
                saveMode["lminfun"]["coef"] = deal["lminfun"].coef.tolist()
                saveMode["lminfun"]["domain"] = deal["lminfun"].domain.tolist()
                saveMode["lminfun"]["window"] = deal["lminfun"].window.tolist()



            res[field] = saveMode
        res["bestLightRange"] = self.bestLightRange
        res["bestNightRange"] = self.bestNightRange
        res["nightRange"] = self.nightRange
        res["lightRange"] = self.lightRange
        modelStr = json.dumps(res)
        with open(self.modelPath,"w") as f:
            f.write(modelStr)
        pass

    def set_data_path(self,path):
        self.datapath = path
        pass


    def load_model(self,model_file):
        print(f"加载模型文件 {model_file}")
        if not os.path.isfile(model_file):
            print(f"模型文件 {model_file} 不存在")
            return
        with open(model_file,"r") as f:
            modelStr = f.read()
            models = json.loads(modelStr)

            for field in models.keys():
                model = models[field]
                if field == "bestLightRange":
                    self.bestLightRange = model
                    continue
                if field == "bestNightRange":
                    self.bestNightRange = model
                    continue
                if field == "nightRange":
                    self.nightRange = model
                    continue
                if field == "lightRange":
                    self.lightRange = model
                    continue

                if field not in self.tel:
                    continue
                self.dealData[field]["light"]["fun"] = p.polynomial.Polynomial(model["light"]["coef"], model["light"]["domain"], model["light"]["window"])
                self.dealData[field]["night"]["fun"] = p.polynomial.Polynomial(model["night"]["coef"],
                                                                               model["night"]["domain"],
                                                                               model["night"]["window"])
                self.dealData[field]["nmaxfun"] = p.polynomial.Polynomial(model["nmaxfun"]["coef"],
                                                                               model["nmaxfun"]["domain"],
                                                                               model["nmaxfun"]["window"])
                self.dealData[field]["nminfun"] = p.polynomial.Polynomial(model["nminfun"]["coef"],
                                                                          model["nminfun"]["domain"],
                                                                          model["nminfun"]["window"])
                self.dealData[field]["lmaxfun"] = p.polynomial.Polynomial(model["lmaxfun"]["coef"],
                                                                          model["lmaxfun"]["domain"],
                                                                          model["lmaxfun"]["window"])
                self.dealData[field]["lminfun"] = p.polynomial.Polynomial(model["lminfun"]["coef"],
                                                                          model["lminfun"]["domain"],
                                                                          model["lminfun"]["window"])

        pass

    def _read_file(self,csv_file,b=False):
        times = []
        datas = []
        with open(csv_file, 'r', encoding="utf-8") as f:
            reader = csv.reader(f)
            for row in reader:
                if b:
                    if float(row[1]) >= self.maxCurrent:
                        continue
                times.append(int(row[0]))
                datas.append(float(row[1]))


        # 缩进变了
        #     if len(times) >0 and b == False:
            time = []
            data = []
            if len(times) > 0 :
                # time, data = self._data_clean(times,datas)
                self._data_clean(times, datas)

            return times, datas
            pass

        pass


    def _load_current_data(self,target):
        print(f"加载电流{target}数据...")
        dayDirs = os.listdir(self.dataPath)

        time = []
        val = []


        for dayDir in dayDirs:
            targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
            times, datas = self._read_file(targetFilePath,False)

            time.extend(times)
            val.extend(datas)
        self.currentData[target]["data"].append(time)
        self.currentData[target]["data"].append(val)
        # self.dealData[target]["data"].append(time)
        # self.dealData[target]["data"].append(val)
        pass

    def _load_deal_data(self,target):
        print(f"加载{target}数据...")
        dayDirs = os.listdir(self.dataPath)
        time = []
        val = []

        for dayDir in dayDirs:
            targetFilePath = os.path.join(self.dataPath, dayDir, target + ".txt")
            times, datas = self._read_file(targetFilePath)
            time.extend(times)
            val.extend(datas)
        self.dealData[target]["data"].append(time)
        self.dealData[target]["data"].append(val)
        pass


    #判断24小时内数据是否有大于2A电流
    def _check24_hour_current_big_2a(self,target,startPos):
        # 根据轴取开始

        starttime = self.currentData[target]["data"][0][startPos]
        # starttime = self.dealData[target]["data"][0][startPos]
        endtime = starttime + 24 * 3600
        currentTime = starttime
        nowPos = startPos
        while currentTime < endtime:

            y = self.currentData[target]["data"][1][nowPos]
            # y = self.dealData[target]["data"][1][nowPos]
            if y >self.splitCurrent:
                return True,nowPos
            nowPos = nowPos +1
            if nowPos >= len(self.currentData[target]["data"][0]):
            # if nowPos >= len(self.dealData[target]["data"][0]):
                return False,nowPos
            currentTime = self.currentData[target]["data"][0][nowPos]
            # currentTime = self.dealData[target]["data"][0][nowPos]
        return False,nowPos
        pass



    # def _check24_hour_current_big_88V(self,target,startPos):
    #     # 根据轴取开始
    #
    #     starttime = self.currentData[target]["data"][0][startPos]
    #     # starttime = self.dealData[target]["data"][0][startPos]
    #     endtime = starttime + 24 * 3600
    #     currentTime = starttime
    #     nowPos = startPos
    #     while currentTime < endtime:
    #
    #         y = self.currentData[target]["data"][1][nowPos]
    #         # y = self.dealData[target]["data"][1][nowPos]
    #         if y >self.splitCurrentV:
    #             return True,nowPos
    #         nowPos = nowPos +1
    #         if nowPos >= len(self.currentData[target]["data"][0]):
    #         # if nowPos >= len(self.dealData[target]["data"][0]):
    #             return False,nowPos
    #         currentTime = self.currentData[target]["data"][0][nowPos]
    #         # currentTime = self.dealData[target]["data"][0][nowPos]
    #     return False,nowPos
    #     pass

    def _check24_hour_current_rule(self,target,startPos):
        # 根据轴取开始

        starttime = self.currentData[target]["data"][0][startPos]
        # starttime = self.dealData[target]["data"][0][startPos]
        endtime = starttime + 24 * 3600
        currentTime = starttime
        nowPos = startPos
        while currentTime < endtime:

            y = self.currentData[target]["data"][1][nowPos]
            # y = self.dealData[target]["data"][1][nowPos]
            if self.bigger and self.smaller:
                if self.smaller < y < self.bigger:
                    return True, nowPos
            elif self.bigger:
                if y < self.bigger:
                    return True, nowPos
            elif self.smaller:
                if y > self.smaller:
                    return True, nowPos
            else:
                pass
            nowPos = nowPos + 1
            if nowPos >= len(self.currentData[target]["data"][0]):
            # if nowPos >= len(self.dealData[target]["data"][0]):
                return False,nowPos
            currentTime = self.currentData[target]["data"][0][nowPos]
            # currentTime = self.dealData[target]["data"][0][nowPos]
        return False,nowPos
        pass

    def _find_first_light_start(self,target):
        print("分析第一个光照期")
        xlist = self.currentData[target]["data"][0]
        ylist = self.currentData[target]["data"][1]

        # xlist = self.dealData[target]["data"][0]
        # ylist = self.dealData[target]["data"][1]

        nowPos = 0
        for i in range(len(xlist)):
            x = xlist[i]
            y = ylist[i]
            if y > self.splitCurrent:
                continue
            if i<nowPos:
                continue
            b,nowPos = self._check24_hour_current_big_2a(target,i)
            if not b:
                return i,x
        pass



    def _find_first_light_start_rule(self,target):
        print("分析第一个光照期")
        xlist = self.currentData[target]["data"][0]
        ylist = self.currentData[target]["data"][1]

        # xlist = self.dealData[target]["data"][0]
        # ylist = self.dealData[target]["data"][1]

        nowPos = 0
        for i in range(len(xlist)):
            x = xlist[i]
            y = ylist[i]
            # if y > self.splitCurrentV:
            #     continue
            if self.bigger and self.smaller:
                if self.smaller < y < self.bigger:
                    continue
            elif self.bigger:
                if y < self.bigger:
                    continue
            elif self.smaller:
                if y > self.smaller:
                    continue
            else:
                pass
            if i<nowPos:
                continue
            b,nowPos = self._check24_hour_current_rule(target,i)
            if not b:
                return i,x
        pass

    # def _find_first_light_start_V(self,target):
    #     print("分析第一个光照期")
    #     xlist = self.currentData[target]["data"][0]
    #     ylist = self.currentData[target]["data"][1]
    #
    #     # xlist = self.dealData[target]["data"][0]
    #     # ylist = self.dealData[target]["data"][1]
    #
    #     nowPos = 0
    #     for i in range(len(xlist)):
    #         x = xlist[i]
    #         y = ylist[i]
    #         if y > self.splitCurrentV:
    #             continue
    #         if i<nowPos:
    #             continue
    #         b,nowPos = self._check24_hour_current_rule(target,i)
    #         if not b:
    #             return i,x
    #     pass

    def _parse_current_range(self,target):
        print("分析电流数据周期...")
        xlist = self.currentData[target]["data"][0]
        ylist = self.currentData[target]["data"][1]
        # xlist = self.dealData[target]["data"][0]
        # ylist = self.dealData[target]["data"][1]
        nightRange = []
        lightRange = []
        nowState = 1  # 1光照 0地影
        startPos,startTime = self._find_first_light_start(target)
        nowPos = 0
        print("\n")
        for i in range(startPos,len(xlist)):
            progress = int(50 * i / len(xlist))
            percentage = 100 * i / len(xlist)
            progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
            print(f"\r正在处理数据... {progress_bar}", end="")

            x = xlist[i]
            y = ylist[i]
            if nowState == 1:
                if y > self.splitCurrent:
                    nowState = 0
                    lightRange.append([startTime,xlist[i-1]])
                    startTime = x
            else:
                if i < nowPos:
                    continue
                b, nowPos = self._check24_hour_current_big_2a(target, i)
                if not b:
                    nowState = 1
                    nightRange.append([startTime,xlist[i-1]])
                    startTime = x
        print("\n")
        return nightRange,lightRange
        pass
    # def _parse_current_range_V(self,target):
    #     print("分析电压数据周期...")
    #     xlist = self.currentData[target]["data"][0]
    #     ylist = self.currentData[target]["data"][1]
    #     # xlist = self.dealData[target]["data"][0]
    #     # ylist = self.dealData[target]["data"][1]
    #     nightRange = []
    #     lightRange = []
    #     nowState = 1  # 1光照 0地影
    #     startPos,startTime = self._find_first_light_start_V(target)
    #     nowPos = 0
    #     print("\n")
    #
    #     length = len(xlist)
    #     step = math.floor((length - startPos)/100)
    #     for i in range(startPos,length):
    #         if (i - startPos) % step == 0:
    #             progress = int(50 * i / length)
    #             percentage = 100 * i / length
    #             progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
    #             print(f"\r正在处理数据... {progress_bar}", end="")
    #
    #         x = xlist[i]
    #         y = ylist[i]
    #         if nowState == 1:
    #             if y > self.splitCurrentV:
    #                 nowState = 0
    #                 lightRange.append([startTime,xlist[i-1]])
    #                 startTime = x
    #         else:
    #             if i < nowPos:
    #                 continue
    #             b, nowPos = self._check24_hour_current_rule(target, i)
    #             if not b:
    #                 nowState = 1
    #                 nightRange.append([startTime,xlist[i-1]])
    #                 startTime = x
    #     print("处理数据结束\n")
    #     return nightRange,lightRange
    #     pass

    def _parse_current_range_rule(self,target):
        print("分析电压数据周期...")
        xlist = self.currentData[target]["data"][0]
        ylist = self.currentData[target]["data"][1]
        # xlist = self.dealData[target]["data"][0]
        # ylist = self.dealData[target]["data"][1]
        nightRange = []
        lightRange = []
        nowState = 1  # 1光照 0地影
        startPos,startTime = self._find_first_light_start_rule(target)
        nowPos = 0
        print("\n")

        length = len(xlist)
        step = math.floor((length - startPos)/100)
        for i in range(startPos,length):
            if (i - startPos) % step == 0:
                progress = int(50 * i / length)
                percentage = 100 * i / length
                progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
                print(f"\r正在处理数据... {progress_bar}", end="")

            x = xlist[i]
            y = ylist[i]
            if nowState == 1:
                # if y > self.splitCurrentV:
                if self.bigger and self.smaller:
                    if self.smaller < y < self.bigger:
                        nowState = 0
                        lightRange.append([startTime, xlist[i - 1]])
                        startTime = x
                elif self.bigger:
                    if y < self.bigger:
                        nowState = 0
                        lightRange.append([startTime, xlist[i - 1]])
                        startTime = x
                elif self.smaller:
                    if y > self.smaller:
                        nowState = 0
                        lightRange.append([startTime, xlist[i - 1]])
                        startTime = x
                else:
                    pass
            else:
                if i < nowPos:
                    continue
                b, nowPos = self._check24_hour_current_rule(target, i)
                if not b:
                    nowState = 1
                    nightRange.append([startTime,xlist[i-1]])
                    startTime = x
        print("处理数据结束\n")
        return nightRange,lightRange
        pass


    #获取最佳周期，最佳周期为最长周期，默认最长周期具有完整特征
    def _get_best_range(self,timeRange):
        res = []
        split = 0
        for i in timeRange:
            if i[1] - i[0] > split:
                split = i[1] - i[0]
                res = i

        return res
        pass

    def _fit_deal_fun(self,target,nightRange,lightRange):
        xlist = self.dealData[target]["data"][0]
        ylist = self.dealData[target]["data"][1]
        xn = []
        yn = []
        xl = []
        yl = []

        nxmax = []
        nymax = []
        nxmin = []
        nymin = []
        lxmax = []
        lymax = []
        lxmin = []
        lymin = []
        ndata = []
        ldata = []
        #
        for r in self.lightRange:
            ldata.append([[],[]])
        for r in self.nightRange:
            ndata.append([[],[]])

        #取光照和地影样本数据
        length = len(xlist)
        step = math.floor(length / 100)
        for j in range(length):
            if j % step == 0:
                progress = int(50 * j / length)
                percentage = 100 * j / length
                progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
                print(f"\r正在处理数据... {progress_bar}", end="")
            x = xlist[j]
            y = ylist[j]
            if nightRange[0] <=x<=nightRange[1]:
                xn.append(x)
                yn.append(y)
            if lightRange[0] <=x<=lightRange[1]:
                xl.append(x)
                yl.append(y)

            for i in range(len(self.lightRange)):
                r = self.lightRange[i]
                if r[0] <= x <= r[1]:
                    ldata[i][0].append(x)
                    # ldata[i][0]=x
                    ldata[i][1].append(y)
                    # ldata[i][1] = y
                    pass
                pass
            for i in range(len(self.nightRange)):
                r = self.nightRange[i]
                if r[0] <= x <= r[1]:
                    ndata[i][0].append(x)
                    ndata[i][1].append(y)
                    # ndata[i][0] = x
                    # ndata[i][1] =y
                    pass
                pass



        print("处理数据结束\n")


        for n in ndata:
            if not n[0]:
                continue
            maxval = max(n[1])
            pos = n[1].index(maxval)
            maxx = n[0][pos]
            nxmax.append(maxx)
            nymax.append(maxval)

            minval = min(n[1])
            pos = n[1].index(minval)
            minx = n[0][pos]
            nxmin.append(minx)
            nymin.append(minval)
            pass
        for l in ldata:
            if not l[0]:
                continue
            maxval = max(l[1])
            pos = l[1].index(maxval)
            maxx = l[0][pos]
            lxmax.append(maxx)
            lymax.append(maxval)

            minval = min(l[1])
            pos = l[1].index(minval)
            minx = l[0][pos]
            lxmin.append(minx)
            lymin.append(minval)
            pass

        self.dealData[target]["nmaxfun"] = p.polynomial.Polynomial.fit(nxmax,nymax,deg=1)
        self.dealData[target]["nminfun"] = p.polynomial.Polynomial.fit(nxmin, nymin, deg=1)
        self.dealData[target]["lmaxfun"] = p.polynomial.Polynomial.fit(lxmax, lymax, deg=1)
        self.dealData[target]["lminfun"] = p.polynomial.Polynomial.fit(lxmin, lymin, deg=1)

        fun = p.polynomial.Polynomial.fit(xl, yl, deg=4)
        x = []
        y = []

        lightRangeDays = lightRange[1]-lightRange[0]
        for i in range(0,lightRangeDays,5):
            x.append(i)
            y.append(fun(np.array([lightRange[0]+i]))[0])

        self.dealData[target]["light"]["fun"] = p.polynomial.Polynomial.fit(x, y, deg=8)

        # fun = p.polynomial.Polynomial.fit(xn, yn, deg=4)
        x = []
        y = []
        nightRangeDay = nightRange[1] - nightRange[0]
        for i in range(0,nightRangeDay,5):
            x.append(i)
            y.append(fun(np.array([nightRange[0]+i]))[0])


        self.dealData[target]["night"]["fun"] = p.polynomial.Polynomial.fit(x, y, deg=8)

        pass

    def train(self,dataPath, bigger, smaller):
        self.bigger = bigger
        self.smaller = smaller

        self.datapath = dataPath
        # self._load_current_data("1027")
        # self._load_current_data("1030")
        self._load_current_data(self.rel_tel)

        # self.nightRange,self.lightRange = self._parse_current_range_V("1030")
        self.nightRange,self.lightRange = self._parse_current_range_rule(self.rel_tel)

        # self.nightRange, self.lightRange = self._parse_current_range("1027")
        self.bestNightRange = self._get_best_range(self.nightRange)
        self.bestLightRange = self._get_best_range(self.lightRange)

        for field in self.dealData.keys():
            self._load_deal_data(field)
            self._fit_deal_fun(field,self.bestNightRange,self.bestLightRange)

        self._save_model()
        pass




    def _split_feature_time_range(self,starttime,daysNum,step,nightRange,lightRange,nightFun,lightFun):
        end_time = starttime+ daysNum*24*3600
        nightDays = nightRange[1] - nightRange[0]
        lightDays = lightRange[1] - lightRange[0]
        subDay = nightDays + lightDays
        lastTime= 0
        lastStatue = 0 #0 地影 1 光照
        if nightRange[1] > lightRange[1]:
            lastTime = nightRange[1]
            lastStatue= 0
            pass
        else:
            lastTime = lightRange[1]
            lastStatue = 1

        x = []
        y = []
        for i in range(starttime,end_time,step):
            yuDay = (i - lastTime) % subDay
            if lastStatue == 0:
                if yuDay<=lightDays:


                    # 这有一点小改动
                    val = lightFun(np.array([yuDay])[0])
                    x.append(i)
                    y.append(val)
                else:
                    val = nightFun(np.array([yuDay-lightDays])[0])
                    x.append(i)
                    y.append(val)


            if lastStatue == 1:
                if yuDay<=nightDays:
                    val = nightDays(np.array([yuDay])[0])
                    x.append(i)
                    y.append(val)
                else:
                    val = lightFun(np.array([yuDay-nightDays])[0])
                    x.append(i)
                    y.append(val)
                pass
            pass
        # x = [stampToTime(item) for item in x]
        return x,y
        pass

    def forword(self,time,daysNum,step = 5):
        res = {}
        for field in self.dealData.keys():
            deal = self.dealData[field]
            if deal["light"]["fun"] is None or deal["night"]["fun"] is None:
                continue
            x,y = self._split_feature_time_range(time,daysNum,step,self.bestNightRange,self.bestLightRange,
                                                 deal["night"]["fun"],deal["light"]["fun"])
            res[field]= {"x":x,"y":y}
            pass
        return res
        pass

    # 新增函数
    # 返回最后一个状态和时间
    def _find_laststatus_time(self):
        nlasttime = self.bestNightRange[1]
        llasttime = self.bestLightRange[1]
        if nlasttime > llasttime:
            return 1,nlasttime
        else:
            return 0,llasttime
        pass


    def _fun_max_min(self,nrange,lrange,nmaxfun,nminfun,lmaxfun,lminfun,splitSec):
        resn = []
        resl = []
        nx =[]
        for x in range(nrange[0],nrange[1],splitSec):
            nx.append(x)
            pass

        lx = []
        for x in range(lrange[0],lrange[1],splitSec):
            lx.append(x)
            pass
        nmaxres = nmaxfun(np.array(nx))
        nminres = nminfun(np.array(nx))
        lmaxres = lmaxfun(np.array(lx))
        lminres = lminfun(np.array(lx))
        resn.extend(nmaxres)
        resn.extend(nminres)
        resl.extend(lmaxres)
        resl.extend(lminres)

        return max(resn),min(resn),max(resl),min(resl)
        pass

    # 预测制定年份的每个周期起始点和取值范围
    # def forwardValRange(self,target,year,splitSec = 5):
    def forwardValRange(self, target, starttime,days, splitSec=5):
        target = target[0]
        range_start = time.mktime(time.strptime(f"{starttime}","%Y-%m-%d %H:%M:%S"))
        # starttime = timeToStamp(starttime)
        range_end = range_start +int(days)*86400
        # range_end = time.mktime(time.strptime(f"{year+1}-01-01 00:00:00", "%Y-%m-%d %H:%M:%S"))

        lrange = self.bestLightRange[1]-self.bestLightRange[0]
        nrange = self.bestNightRange[1]-self.bestNightRange[0]

        res = [ ]
        nowStatus,nowPos = self._find_laststatus_time()
        while nowPos < range_end:
            bTurn =False
            if nowStatus == 1:
                lrange =lrange-splitSec
                if lrange <=0:
                    nowStatus = 0
                    nrange =self.bestNightRange[1]-self.bestNightRange[0]
                    bTurn = True
                pass
            else:
                nrange = nrange - splitSec
                if nrange<=0:
                    nowStatus = 1
                    lrange =self.bestLightRange[1]-self.bestLightRange[0]
                    bTurn = True
                pass

            if nowPos >=range_start and bTurn is True:
                item = {"from":0,"to":0,"time":0,"nrange":[],"lrange":[]}
                item["to"] = nowStatus
                item["from"] = 1- nowStatus
                item["time"] = nowPos

                # 这个地方应该是定的两天

                if nowStatus == 0:
                    # item["lrange"].append(nowPos-lrange)
                    item["lrange"].append(nowPos - 10*24*3600)
                    item["lrange"].append(nowPos)
                else:
                    item["nrange"].append(nowPos)
                    # item["nrange"].append(nowPos+nrange)
                    item["nrange"].append(nowPos + 10*24*3600)

                if nowStatus == 1:
                    # item["lrange"].append(nowPos-nrange)
                    item["lrange"].append(nowPos - 10*24*3600)
                    item["lrange"].append(nowPos)
                else:
                    item["nrange"].append(nowPos)
                    # item["nrange"].append(nowPos+lrange)
                    item["nrange"].append(nowPos + 10*24*3600)

                res.append(item)
                pass
            nowPos = nowPos +splitSec

        ret_res = []
    #     "nmaxfun":None,"nminfun":None,"lmaxfun":None,"lminfun":None
        deal = self.dealData[target]
        for item in res:
            resItem = {"from":0,"to":0,"time":0,"max":0,"min":0}
            resItem["from"] = item["from"]
            resItem["to"] = item["to"]
            resItem["time"] = item["time"]
            maxnval,minnval,maxlval,minlval = self._fun_max_min(item["nrange"],item["lrange"],deal["nmaxfun"],deal["nminfun"],deal["lmaxfun"],deal["lminfun"],splitSec)
            if resItem["from"] == 1:
                resItem["max"] = maxnval
                resItem["min"] = minnval
            elif resItem["from"] == 0:
                resItem["max"] = maxlval
                resItem["min"] = minlval


            # resItem["maxn"]=maxnval
            # resItem["minn"] =minnval
            # resItem["maxl"] = maxlval
            # resItem["minl"] = minlval
            ret_res.append(resItem)
            pass
        # for x in ret_res:
        #     if x["from"] == 1:
        #         x.pop("maxl")
        #         x.pop("minl")
        #         # del.x["maxl"]
        #         # del.x['minl']
        #     elif x["from"] == 0:
        #         x.pop("maxn")
        #         x.pop("minn")
        return ret_res
        pass

    def huatu(self,res, starttime, days):
        # qushi = Qushi2()
        # # qushi.train("../new/电池趋势数据/tel")
        # # qushi.set_data_path("../new/电池趋势数据/tel")
        # qushi.load_model("./weight.dt")
        # res = qushi.forwardValRange(canshu, starttime, days)

        x = []
        maxy = []
        miny = []
        for time in range(res[0]["time"], (res[0]["time"] + int(days) * 86400), 86400):
            x.append(time)
            for i in range(1, len(res)):
                if time < res[i]["time"]:
                    maxy.append(res[i - 1]['max'])
                    miny.append(res[i - 1]['min'])
                    break
                elif time >= res[-1]["time"]:
                    maxy.append(res[-1]['max'])
                    miny.append(res[- 1]['min'])
                    break
        res = {self.tel[0]:{"x":x, "miny":miny, "maxy":maxy}}

        return res



    def _data_clean(self,x,y):
        subSum = []
        avg = np.array(y).mean()

        for i in y:
            subSum.append(abs(i-avg))
        avgSub = sum(subSum)/len(subSum)
        if avgSub > 0.0001:
            dataLen = len(y)
            for i in range(dataLen):
                p = dataLen - i -1
                val = y[p]
                if abs(val - avg) > 10*avgSub or val >= 5000:

                    del y[p]
                    del x[p]



    # def _data_clean(self, time, data):
    #
    #     count =1
    #     data_size = len(data)
    #     while count > 0:
    #         time, data = self.iter_three_sigma(time, data)
    #         # print(len(data))
    #         if len(data) == data_size:
    #             break
    #         else:
    #             data_size = len(data)
    #         count -= 1
    #     return time, data

        # def iter_three_sigma(time, data):
        #     data = np.array(data)
        #     average = np.nanmean(data)
        #     std = np.nanstd(data)
        #     index = np.where((data > (average + 3 * std)) | (data < (average - 3 * std)) | (np.abs(data) > 10000))
        #     data = data.tolist()
        #     for idx in range(len(index[0]) - 1, -1, -1):
        #         del time[index[0][idx]]
        #         del data[index[0][idx]]
        #     return time, data

    def iter_three_sigma(self, time, data):
        data = np.array(data)
        average = np.nanmean(data)
        std = np.nanstd(data)
        if std < 0.001:
            return time, data
        index = np.where((data > (average + 3 * std)) | (data < (average - 3 * std)) | (np.abs(data) > 10000))
        data = data.tolist()
        for idx in range(len(index[0]) - 1, -1, -1):
            del time[index[0][idx]]
            del data[index[0][idx]]
        return time, data






    def draw_pic(self,target):
        print(f"构建{target}分析图")
        plt.figure(figsize=(200, 20))
        xList = []
        yList = []
        if self.dealData.__contains__(target):
            if len(self.dealData[target]["data"]) == 0:
                self._load_deal_data(target)
            xList = self.dealData[target]["data"][0]
            yList = self.dealData[target]["data"][1]
            pass
        else:

            # 有改动
            if len(self.dealData[target]["data"]) >= 0:
            # if len(self.currentData[target]["data"]) >= 0:
                self._load_current_data(target)
                pass
            # xList = self.currentData[target]["data"][0]
            # yList = self.currentData[target]["data"][1]
            xList = self.dealData[target]["data"][0]
            yList = self.dealData[target]["data"][1]
            pass

        xl = []
        yl = []
        xn = []
        yn = []

        for i in range(len(xList)):
            progress = int(50 * i / len(xList))
            percentage = 100 * i / len(xList)
            progress_bar = f"[{'#' * progress}{'.' * (50 - progress)}] {percentage:.2f}%"
            print(f"\r正在处理数据... {progress_bar}", end="")
            x = xList[i]
            y = yList[i]

            for r in self.nightRange:
                if r[0] <= x <= r[1]:
                    xn.append(x)
                    yn.append(y)
            for r in self.lightRange:
                if r[0] <= x <= r[1]:
                    xl.append(x)
                    yl.append(y)
            pass

        print("\n")

        plt.scatter(np.array(xn), np.array(yn), label='night', color='red')
        plt.scatter(np.array(xl), np.array(yl), label='light', color='blue')
        #plt.scatter(np.array(res["x"]), np.array(res["y"]), label='feature', color='green')
        plt.xlabel('time')
        plt.ylabel('val')
        plt.title(f"./{target}地影光照趋势周期分析图.png")
        plt.legend()
        plt.savefig(f"./{target}地影光照趋势周期分析图.png")
        plt.close()
        print('\n构图完成')
        pass

    def guanlian(self,A, B):
        self._load_deal_data(A)
        AList = self.dealData[A]["data"][1]

        # print(AList)
        self._load_deal_data(B)
        BList = self.dealData[B]["data"][1]
        if len(AList)>=len(BList):
            AList = AList[:len(BList)]
        else:
            BList = BList[:len(AList)]
        df = pd.DataFrame({"A": AList, "B": BList})
        # df = pd.DataFrame(AList,BList)
        # a = df.corr(method="spearman")
        a = df.corr(method="spearman")
        # print(a[A],a[B])
        return a["A"]["B"]

    def choose(self,list):
        if len(list)==1:
            return [[1]]
        if len(list)==0:
            return [[]]
        else:
            # res_item = [1]*len(list)
            # res_item_copy = res_item.copy()
            # res = [res_item_copy]*len(list)
            # res_copy = res.copy()
            res = []
            for i in range(len(list)):
                res_item = [1] * len(list)
                res.append(res_item)
            res_copy = res.copy()

            for i in range(len(list)):
                for j in range(i+1,len(list)):
                    a = abs(self.guanlian(list[i],list[j]))
                    res_copy[i][j]*=a
                    res_copy[j][i]*=a
        return res_copy

    def guanlian2(self,AList, BList):
        # self._load_deal_data(A)
        # AList = self.dealData[A]["data"][1]
        #
        # # print(AList)
        # self._load_deal_data(B)
        # BList = self.dealData[B]["data"][1]
        if len(AList)>=len(BList):
            AList = AList[:len(BList)]
        else:
            BList = BList[:len(AList)]
        df = pd.DataFrame({"A": AList, "B": BList})
        # df = pd.DataFrame(AList,BList)
        # a = df.corr(method="spearman")
        a = df.corr(method="pearson")
        # print(a[A],a[B])
        return a["A"]["B"]

    def choose2(self,list):
        if len(list)==1:
            return [[1]]
        if len(list)==0:
            return [[]]
        else:
            ori_list = []
            for n in range(len(list)):
                self._load_deal_data(list[n])
                # 遥测每日均值
                # a = np.mean(self.dealData[list[n]]["data"][1])
                # ori_list.append(a)

                # 原始遥测
                ori_list.append(self.dealData[list[n]]["data"][1])
            res = []
            for m in range(len(list)):
                res_item = [1] * len(list)
                res.append(res_item)
            res_copy = res.copy()

            for i in range(len(list)):
                for j in range(i+1,len(list)):
                    a = abs(self.guanlian2(ori_list[i],ori_list[j]))
                    res_copy[i][j]*=a
                    res_copy[j][i]*=a
        return res_copy

    # def huatu(self,target):
    #     self._load_deal_data(target)
    #     AList = self.dealData[target]["data"][1]
    #     A_simple_List = []
    #     A_simple_Index = []
    #     for i in range(0,len(AList),3600):
    #         A_simple_List.append(AList[i])
    #         A_simple_Index.append(i)
    #     plt.figure(figsize=(200, 20))
    #     plt.plot(A_simple_Index, A_simple_List, color='red', label="")
    #     plt.legend()
    #     plt.savefig("./" + str(target) +".png")
    #     plt.close()