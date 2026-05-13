# -*- coding: utf-8 -*-
# 导入蓝图模块，数据传输模块
from flask import Blueprint, request, jsonify, url_for

import json
from algsmanagement.service import s_preprocess
from algsmanagement.vector_service import s_vector_health, s_vector_embed, s_vector_embed_single, \
    s_vector_similarity, s_vector_path_similarity, s_vector_batch_similarity
from algsmanagement.tasks import t_algs_load, t_loss, t_network, t_predict, t_getOrbitPic, t_preprocess

# 创建蓝图，蓝图必须有前两个参数，为“蓝图名”，“当前运行文件名”。
# 后两个是设置蓝图文件夹（蓝图文件夹即为app1文件夹）在访问私有网页文件夹templates的位置目录，以及私有静态文件的位置目录
algsmanagement = Blueprint('algsmanagement',__name__, url_prefix='/algsmanagement')


@algsmanagement.route('/s_preprocess', methods=['POST'])
def test_preprocess():
    try:
        postBody = request.get_data()
        print(f"Content-Type: {request.content_type}")
        print(f"Raw body: {postBody[:500]}")
        data = json.loads(postBody)
        ret_status, ret_obj = s_preprocess(data)
        return ret_obj
    except json.JSONDecodeError as e:
        print(f"JSON decode error: {e}")
        print(f"Body content: {postBody}")
        return {"code": 500, "result": f"Invalid JSON: {str(e)}"}


@algsmanagement.route('/preprocess', methods=['POST'])
def preprocess():
    """
    调用算法
    """
    postBody = request.get_data()
    print(json.loads(postBody))
    task = t_preprocess.apply_async(args=[json.loads(postBody)])
    return jsonify({"location": url_for("algsmanagement.taskstatus", task_id=task.id)})

#-----------------------------------------------

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


#-----------------------------------------------
# 向量相关接口（同步）
#-----------------------------------------------

@algsmanagement.route('/vector_health', methods=['POST'])
def vector_health():
    """向量模型健康检查"""
    try:
        ret_status, ret_obj = s_vector_health({})
        code = 200 if ret_status else 500
        return jsonify({"code": code, "result": ret_obj})
    except Exception as e:
        return jsonify({"code": 500, "result": str(e)})


@algsmanagement.route('/vector_embed', methods=['POST'])
def vector_embed():
    """批量文本向量化"""
    data = request.get_json(silent=True)
    if not data or 'nodes' not in data:
        return jsonify({"code": 400, "result": "缺少 nodes 参数"})
    try:
        ret_status, ret_obj = s_vector_embed(data)
        code = 200 if ret_status else 500
        return jsonify({"code": code, "result": ret_obj})
    except Exception as e:
        return jsonify({"code": 500, "result": str(e)})


@algsmanagement.route('/vector_embed_single', methods=['POST'])
def vector_embed_single():
    """单条文本向量化"""
    data = request.get_json(silent=True)
    if not data or 'node' not in data:
        return jsonify({"code": 400, "result": "缺少 node 参数"})
    try:
        ret_status, ret_obj = s_vector_embed_single(data)
        code = 200 if ret_status else 500
        return jsonify({"code": code, "result": ret_obj})
    except Exception as e:
        return jsonify({"code": 500, "result": str(e)})


@algsmanagement.route('/vector_similarity', methods=['POST'])
def vector_similarity():
    """计算两个向量的余弦相似度"""
    data = request.get_json(silent=True)
    if not data or 'vector1' not in data or 'vector2' not in data:
        return jsonify({"code": 400, "result": "缺少 vector1 或 vector2 参数"})
    try:
        ret_status, ret_obj = s_vector_similarity(data)
        code = 200 if ret_status else 500
        return jsonify({"code": code, "result": ret_obj})
    except Exception as e:
        return jsonify({"code": 500, "result": str(e)})


@algsmanagement.route('/vector_path_similarity', methods=['POST'])
def vector_path_similarity():
    """计算两条路径的逐层相似度"""
    data = request.get_json(silent=True)
    if not data or 'path1' not in data or 'path2' not in data:
        return jsonify({"code": 400, "result": "缺少 path1 或 path2 参数"})
    try:
        ret_status, ret_obj = s_vector_path_similarity(data)
        code = 200 if ret_status else 500
        return jsonify({"code": code, "result": ret_obj})
    except Exception as e:
        return jsonify({"code": 500, "result": str(e)})


@algsmanagement.route('/vector_batch_similarity', methods=['POST'])
def vector_batch_similarity():
    """批量路径相似度计算"""
    data = request.get_json(silent=True)
    if not data or 'new_path' not in data or 'skeleton_paths' not in data:
        return jsonify({"code": 400, "result": "缺少 new_path 或 skeleton_paths 参数"})
    try:
        ret_status, ret_obj = s_vector_batch_similarity(data)
        code = 200 if ret_status else 500
        return jsonify({"code": code, "result": ret_obj})
    except Exception as e:
        return jsonify({"code": 500, "result": str(e)})