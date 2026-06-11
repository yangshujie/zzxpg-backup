import psycopg2
from dbutils.persistent_db import PersistentDB
from config.config import KINGBASE_CONFIG_sjhz


class KingbaseDB:
    def __init__(self, host=None, port=None, user=None, password=None, database=None, schema=None):
        config = KINGBASE_CONFIG_sjhz.copy()
        if host:
            config['host'] = host
        if port:
            config['port'] = port
        if user:
            config['user'] = user
        if password:
            config['password'] = password
        if database:
            config['database'] = database
        if schema:
            config['schema'] = schema

        self.pool = PersistentDB(
            creator=psycopg2,
            maxusage=None,
            setsession=[],
            ping=1,
            closeable=False,
            host=config['host'],
            port=config['port'],
            user=config['user'],
            password=config['password'],
            database=config['database'],
            options=f'-c search_path={config["schema"]}'
        )

    def __enter__(self):
        self.connection = self.pool.connection()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.connection.close()

    def query(self, sql, args=None):
        with self.connection.cursor() as cursor:
            cursor.execute(sql, args)
            result = cursor.fetchall()
        return result

    def execute(self, sql, args=None):
        with self.connection.cursor() as cursor:
            cursor.execute(sql, args)
            affected_rows = cursor.rowcount
        self.connection.commit()
        return affected_rows

    def insert(self, sql, args=None):
        with self.connection.cursor() as cursor:
            cursor.execute(sql, args)
            lastrowid = cursor.lastrowid
        self.connection.commit()
        return lastrowid

    def create_table(self, table_name, columns):
        """
        创建数据库表

        Args:
            table_name: 表名
            columns: 列定义字典，格式为 {列名: 列类型} 或 [{'name': 列名, 'type': 列类型, 'constraint': 约束}, ...]
        """
        column_defs = []
        if isinstance(columns, dict):
            for col_name, col_type in columns.items():
                column_defs.append(f'"{col_name}" {col_type}')
        elif isinstance(columns, list):
            for col in columns:
                col_def = f'"{col["name"]}" {col["type"]}'
                if 'constraint' in col:
                    col_def += f' {col["constraint"]}'
                column_defs.append(col_def)

        sql = f'CREATE TABLE IF NOT EXISTS "{table_name}" ({", ".join(column_defs)})'
        return self.execute(sql)

    def table_exists(self, table_name):
        """检查表是否存在"""
        sql = """
            SELECT EXISTS (
                SELECT FROM information_schema.tables
                WHERE table_schema = %s
                AND table_name = %s
            )
        """
        schema = KINGBASE_CONFIG_sjhz.get('schema', 'public')
        result = self.query(sql, (schema, table_name))
        return result[0][0] if result else False

    def get_table_columns(self, table_name):
        """获取表的所有列名"""
        sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = %s
            AND table_name = %s
            ORDER BY ordinal_position
        """
        schema = KINGBASE_CONFIG_sjhz.get('schema', 'public')
        result = self.query(sql, (schema, table_name))
        return [row[0] for row in result]

    def add_column(self, table_name, column_name, column_type):
        """为已有表添加新列"""
        sql = f'ALTER TABLE "{table_name}" ADD COLUMN "{column_name}" {column_type}'
        return self.execute(sql)

    def query_data(self, table_name, fields=None, start_time=None, end_time=None):
        """
        查询表中指定字段的数据

        Args:
            table_name: 表名
            fields: 字段名列表，如 ['timestamp', 'key']，为None时查询全部字段
            start_time: 起始时间（可选），用于按timestamp字段筛选
            end_time: 结束时间（可选），用于按timestamp字段筛选

        Returns:
            list[dict]: 查询结果，如 [{'key1': 1.0, 'key2': 2.0}, ...]
        """
        if fields:
            col_names = ', '.join([f'"{f}"' for f in fields])
        else:
            col_names = '*'

        sql = f'SELECT {col_names} FROM "{table_name}"'
        conditions = []
        params = []

        if start_time:
            conditions.append('"timestamp" >= %s')
            params.append(start_time)
        if end_time:
            conditions.append('"timestamp" <= %s')
            params.append(end_time)

        if conditions:
            sql += ' WHERE ' + ' AND '.join(conditions)

        result = self.query(sql, tuple(params) if params else None)

        if not result:
            return []

        if fields:
            return [dict(zip(fields, row)) for row in result]
        else:
            # 无fields时，需要获取列名来构建dict
            with self.connection.cursor() as cursor:
                cursor.execute(f'SELECT * FROM "{table_name}" LIMIT 0')
                col_names = [desc[0] for desc in cursor.description]
            return [dict(zip(col_names, row)) for row in result]

    def drop_table(self, table_name, if_exists=True):
        """删除表"""
        if if_exists:
            sql = f'DROP TABLE IF EXISTS "{table_name}"'
        else:
            sql = f'DROP TABLE "{table_name}"'
        return self.execute(sql)

    def truncate_table(self, table_name):
        """清空表数据"""
        sql = f'TRUNCATE TABLE "{table_name}"'
        return self.execute(sql)

    def update_task_status(self, task_id, status):
        """
        更新任务状态

        Args:
            task_id: 任务ID
            status: 新状态值

        Returns:
            (bool, str) 成功返回 (True, '成功信息')，失败返回 (False, '错误信息')
        """
        if not task_id:
            return False, "task_id不能为空"

        try:
            sql = 'UPDATE ycl_task SET status = %s WHERE id = %s'
            affected_rows = self.execute(sql, (status, task_id))
            if affected_rows == 0:
                return False, f"未找到task_id为{task_id}的任务"
            return True, f"成功更新任务{task_id}状态为{status}"
        except Exception as e:
            return False, f"更新任务状态失败: {str(e)}"


if __name__ == "__main__":
    with KingbaseDB() as db:
        # 测试连接
        result = db.query("SELECT version()")
        print(result)

        # 创建表示例
        columns = {
            'id': 'SERIAL PRIMARY KEY',
            'name': 'VARCHAR(100)',
            'value': 'NUMERIC(10,2)',
            'created_at': 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP'
        }
        # db.create_table('test_table', columns)

        # 检查表是否存在
        # print(db.table_exists('test_table'))
