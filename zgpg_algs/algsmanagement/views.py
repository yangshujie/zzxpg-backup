# -*- coding: utf-8 -*-
# 导入蓝图模块，数据传输模块
from flask import Blueprint, request, jsonify, url_for

import json
from algsmanagement.tasks import t_algs_load,t_loss,t_network,t_predict,t_getOrbitPic

# 创建蓝图，蓝图必须有前两个参数，为“蓝图名”，“当前运行文件名”。
# 后两个是设置蓝图文件夹（蓝图文件夹即为app1文件夹）在访问私有网页文件夹templates的位置目录，以及私有静态文件的位置目录
algsmanagement = Blueprint('algsmanagement',__name__, url_prefix='/algsmanagement')


@algsmanagement.route('/algs_load', methods=['POST'])
def algs_load():
    """
    调用算法
    """
    postBody = request.get_data()
    print(json.loads(postBody))
    task = t_algs_load.apply_async(args=[json.loads(postBody)])
    return jsonify({"location": url_for("algsmanagement.taskstatus", task_id=task.id)})


@algsmanagement.route('/status/<task_id>',methods=['POST'])
def taskstatus(task_id):
    try:
        task = t_algs_load.AsyncResult(task_id)
        if task.successful():
            ret_status,ret_obj=task.get()
            if not ret_status:
                return {"code":500,"result":ret_obj}
            return {"code":200,"result":ret_obj}
        else:
            return {"code":0,"result":"pending"}
    except Exception as e:
        return {"code":500,"result":e.args}


@algsmanagement.route('/loss', methods=['POST'])
def loss():
    '''
    退化模型
    :return:
    '''
    postBody = request.get_data()
    print(json.loads(postBody))
    task = t_loss.apply_async(args=[json.loads(postBody)])
    return jsonify({"location": url_for("algsmanagement.taskstatus", task_id=task.id)})


@algsmanagement.route('/network', methods=['POST'])
def network():
    '''
    神经网络
    :return:
    '''
    postBody = request.get_data()
    print(json.loads(postBody))
    task = t_network.apply_async(args=[json.loads(postBody)])
    return jsonify({"location": url_for("algsmanagement.taskstatus", task_id=task.id)})

@algsmanagement.route('/predict', methods=['POST'])
def predict():
    '''
    趋势预测
    :return:
    '''
    postBody = request.get_data()
    print(json.loads(postBody))
    task = t_predict.apply_async(args=[json.loads(postBody)])
    return jsonify({"location": url_for("algsmanagement.taskstatus", task_id=task.id)})

@algsmanagement.route('/getOrbitPic', methods=['POST'])
def getOrbitPic():
    '''
    获取轨位显示图
    :return:
    '''
    postBody = request.get_data()
    print(json.loads(postBody))
    task = t_getOrbitPic.apply_async(args=[json.loads(postBody)])
    return jsonify({"location": url_for("algsmanagement.taskstatus", task_id=task.id)})