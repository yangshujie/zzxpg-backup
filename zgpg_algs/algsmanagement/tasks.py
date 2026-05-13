# /usr/bin/env python
# -*- coding:utf-8 -*-
from config.config import celery
from algsmanagement.service import s_algs_load,s_loss,s_predict,s_network, s_getOrbitPic, s_preprocess


@celery.task(name="t_preprocess",bind=True)
def t_preprocess(data):
    ret_status, ret_obj = s_preprocess(data)
    return ret_status,ret_obj


@celery.task(name="t_algs_load",bind=True)
def t_algs_load(self, task):
    ret_status, ret_obj = s_algs_load(task)
    return ret_status,ret_obj


@celery.task(name="t_loss",bind=True)
def t_loss(self, task):
    ret_status, ret_obj = s_loss(task)
    return ret_status,ret_obj


@celery.task(name="t_network",bind=True)
def t_network(self, task):
    ret_status, ret_obj = s_network(task)
    return ret_status,ret_obj

@celery.task(name="t_predict",bind=True)
def t_predict(self, task):
    ret_status, ret_obj = s_predict(task)
    return ret_status,ret_obj

@celery.task(name="t_getOrbitPic",bind=True)
def t_getOrbitPic(self, task):
    ret_status, ret_obj = s_getOrbitPic(task)
    return ret_status,ret_obj