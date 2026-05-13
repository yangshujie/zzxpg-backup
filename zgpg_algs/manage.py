# /usr/bin/env python
# -*- coding:utf-8 -*-
from flask import Flask

from flask_cors import CORS
from celery import platforms
from config.config import make_celery

app = Flask(__name__)
CORS(app, supports_credentials=True)

celery = make_celery(app)
platforms.C_FORCE_ROOT = True

# 必须在导入 views/tasks 之前挂上带 Flask 上下文的实例，否则任务会绑在未配置的默认 Celery（amqp）上
import config.config as _cfg
_cfg.celery = celery

from algsmanagement.views import algsmanagement

app.register_blueprint(algsmanagement, url_prefix='/algsmanagement')


if __name__ == '__main__':
    # app.run("0.0.0.0",6000,threaded=False,processes=4)
    app.run("0.0.0.0", 6000)
    # manage.run()
    #python -m celery -A manage.celery worker -l info -P eventlet
