# 字段映射关系API地址
FIELD_MAPPING_URL = "http://localhost:9501/fieldMappingRelation/editPred"

# Kingbase数据库配置
KINGBASE_CONFIG_sjhz = {
    'host': '127.0.0.1',
    'port': 54321,
    'user': 'system',
    'password': 'Wxwx2026!!!',
    'database': 'sjhz',
    'schema': 'sjcl-system'
}
KINGBASE_CONFIG_zhpq= {
    'host': '10.20.162.108',
    'port': 54321,
    'user': 'system',
    'password': 'Wxwx2026!!!',
    'database': 'zhpg',
    'schema': 'public'
}
# 'host': '10.20.162.108',
# 'port': 54321,
# 'user': 'system',
# 'password': 'Wxwx2026!!!',
# 'database': 'txxtpq',
# 'schema': 'algorithm'
# Celery 5.x 配置
class Celery_config():
    broker_url = "redis://:@127.0.0.1:6379/1"
    result_backend = "redis://:@127.0.0.1:6379/2"
    accept_content = ['json', 'pickle']
    task_serializer = 'json'
    result_serializer = 'json'
    enable_utc = True
    timezone = 'Asia/Shanghai'
    broker_connection_retry_on_startup = True
    imports = (
        'algs',
        'algsmanagement.tasks'
    )

from celery import Celery

def make_celery(app=None):
    """创建 Celery 实例，兼容 Flask 应用上下文"""
    c = Celery(__name__)
    # 必须始终加载 Celery_config；否则默认 broker 为 amqp://（RabbitMQ），与 Redis 配置不一致
    c.config_from_object(Celery_config)
    if app is not None:
        class FlaskTask(c.Task):
            def __call__(self, *args, **kwargs):
                with app.app_context():
                    return self.run(*args, **kwargs)
        c.Task = FlaskTask
    return c

celery = make_celery()