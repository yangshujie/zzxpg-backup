# coding:utf-8
import os, json
from importlib import import_module,reload
import pandas as pd
import numpy as np
from algsmanagement.models import *
import time
from algsmanagement.XZ_orbitPos import getOrbitPic
from algsmanagement.kingbase_util import KingbaseDB
from config.config import FIELD_MAPPING_URL, KINGBASE_CONFIG_zhpq
import requests

# ======================== 通用 ========================

# 模块导入缓存，避免重复导入（90天数据只需导入2次，而不是180次）
_module_cache = {}

# s_preprocess预处理算子动态导入说明：
# 新接口参考s_algs_load，由接口参数定位算法，避免新增algs算法文件后修改service.py。
# 参数约定：
#   1. algs是固定算法根目录，不需要由接口传入。
#   2. algs_type是算法分类标识，预处理算法固定传 "dataProcess"。
#   3. algs_name是具体算法文件夹名，取自code_url里的zip名称。
#      例如 code_url = "algs/dataProcess/timestamp_deduplicate.zip"，则 algs_name = "timestamp_deduplicate"。
# 路径拼接规则：algs.<algs_type>.<algs_name>.<algs_name>
# 请求示例：{"operator": "数据值时间去重", "algs_type": "dataProcess", "algs_name": "timestamp_deduplicate", "parameters": {}}
# 旧接口兼容：如果step只传operator，则根据operator查询pypz_algorithm.algorithm_code_url，
#             再从code_url解析algs_type和algs_name。新增算法只需保证算法表有对应记录。
# 注意：zip解压后的目录和算法入口文件需同名，并提供 algsMain(data=current_data, params=params)。


def _module_path_from_algorithm_code_url(code_url):
    """根据algs/dataProcess/demo.zip解析为algs.dataProcess.demo.demo。"""
    if not code_url:
        return None

    normalized_url = code_url.replace('\\', '/')
    parts = [part for part in normalized_url.split('/') if part]
    if len(parts) < 3:
        return None

    algs_type = parts[-2]
    zip_name = parts[-1]
    algs_name = zip_name[:-4] if zip_name.lower().endswith('.zip') else zip_name
    if not algs_type or not algs_name:
        return None
    return "algs" + "." + algs_type + "." + algs_name + "." + algs_name


def _query_preprocess_module_path_by_operator(operator):
    """兼容旧接口：根据中文operator从算法表查询code_url并解析模块路径。"""
    if not operator:
        return None
    try:
        table_name = "pgzc_algorithm"
        sql = f"""
            SELECT algorithm_code_url
            FROM {table_name}
            WHERE algorithm_name = %s
            LIMIT 1
        """
        with KingbaseDB(database=KINGBASE_CONFIG_zhpq["database"],schema=KINGBASE_CONFIG_zhpq["schema"]) as db2:
            result = db2.query(sql, (operator,))
        if not result:
            return None
        return _module_path_from_algorithm_code_url(result[0][0])
    except Exception as e:
        import traceback
        print("❌ Actual error:")
        print(traceback.format_exc())
        print(f"Type: {type(e)}, Message: {e}")

def _resolve_preprocess_module_path(step):
    """根据新接口参数或旧operator查询结果解析预处理算法模块路径。"""
    algs_type = step.get("algs_type")
    algs_name = step.get("algs_name")
    if algs_type and algs_name:
        return "algs" + "." + algs_type + "." + algs_name + "." + algs_name
    return _query_preprocess_module_path_by_operator(step.get("operator"))

# 性能统计字典，记录各个预处理步骤的耗时
_performance_stats = {
    'file_read': [],           # 文件读取耗时
    'module_import_ty': [],     # ty模块导入耗时
    'module_import_tc': [],     # tc模块导入耗时
    'data_preprocess': [],      # 数据预处理耗时（n_sigma/three_sigma等）
    'dataframe_create': [],    # DataFrame创建耗时
    'data_ffill': [],          # 数据填充耗时
    'data_convert': [],        # 数据转换耗时（tolist等）
    'total_per_day': []        # 每天总耗时
}

def to_json_safe(obj):
    if isinstance(obj, dict):
        return {k: to_json_safe(v) for k, v in obj.items()}
    elif isinstance(obj, (list, tuple, set)):
        return [to_json_safe(item) for item in obj]
    elif isinstance(obj, np.integer):
        return int(obj)
    elif isinstance(obj, np.floating):
        return None if np.isnan(obj) else float(obj)
    elif isinstance(obj, np.bool_):
        return bool(obj)
    elif isinstance(obj, np.ndarray):
        return obj.tolist()
    elif isinstance(obj, pd.Timestamp):
        return str(obj)
    elif obj is pd.NaT:            # 修复点：用 is 而不是 isinstance
        return None
    else:
        return obj


def infer_sql_type(value):
    """根据Python值推断SQL列类型"""
    if value is None:
        return 'TEXT'
    if isinstance(value, bool):
        return 'BOOLEAN'
    if isinstance(value, (int, float)):
        return 'NUMERIC(10,2)'
    if isinstance(value, str):
        return 'TEXT'
    return 'TEXT'


def s_save_preprocessed_to_kingbase(table_name, data):
    """
    将预处理后的数据保存到Kingbase数据库

    Args:
        table_name: 表名
        data: 数据列表，如 [{'f': 1.0, 'field1': 1.0, ...}, ...]

    Returns:
        (bool, str) 成功返回 (True, '成功信息')，失败返回 (False, '错误信息')
    """
    if not data:
        return False, "数据为空"

    try:
        with KingbaseDB() as db:
            if db.table_exists(table_name):
                # 表已存在：检查并补充缺失字段，追加数据
                existing_cols = db.get_table_columns(table_name)
                first_row = data[0]
                for col_name, value in first_row.items():
                    if col_name not in existing_cols:
                        col_name_lower = col_name.lower()
                        if col_name_lower == 'time' or col_name_lower == 'timestamp':
                            col_type = 'TIMESTAMP'
                        elif col_name_lower == 'batch_id':
                            col_type = 'BIGINT'
                        else:
                            col_type = infer_sql_type(value)
                        db.add_column(table_name, col_name, col_type)
                cols = list(data[0].keys())
            else:
                # 表不存在：根据第一条数据推断列类型并创建表
                first_row = data[0]
                columns = {}
                for col_name, value in first_row.items():
                    col_name_lower = col_name.lower()
                    if col_name_lower == 'time' or col_name_lower == 'timestamp':
                        columns[col_name] = 'TIMESTAMP'
                    elif col_name_lower == 'batch_id':
                        columns[col_name] = 'BIGINT'
                    else:
                        columns[col_name] = infer_sql_type(value)
                db.create_table(table_name, columns)
                cols = list(data[0].keys())

            # 插入数据（追加到已有数据后面）
            placeholders = ', '.join(['%s'] * len(cols))
            col_names = ', '.join([f'"{c}"' for c in cols])
            sql = f'INSERT INTO "{table_name}" ({col_names}) VALUES ({placeholders})'

            for row in data:
                values = []
                for col in cols:
                    val = row.get(col)
                    col_name_lower = col.lower()
                    if (col_name_lower == 'time' or col_name_lower == 'timestamp') and val is not None:
                        val = pd.to_datetime(val)
                    elif isinstance(val, float) and np.isnan(val):
                        val = None
                    values.append(val)
                db.execute(sql, values)

            return True, f"成功保存 {len(data)} 条数据到表 {table_name}"

    except Exception as e:
        return False, f"保存到Kingbase失败: {str(e)}"

def s_preprocess(task):
    """
    执行数据预处理流程（动态加载算法版本）
    :param task: dict 或 JSON 字符串，包含数据源和步骤配置
                 data_sources格式: {"表名": {"字段1": "id1", "字段2": "id2", ...}, ...}
                 steps: 预处理步骤列表
                 可选字段: start_time, end_time 用于时间筛选
                 可选字段: custom_algorithms 自定义算法路径映射 {"算法名": "模块路径", ...}
                 可选字段: enable_reload 是否重新加载模块（开发环境可设为True）
                 可选字段: algorithm_category 算法分类目录，默认"dataProcess"
    :return: (bool, json_str) 成功返回 True 和结果 JSON，失败返回 False 和错误 JSON
    """
    # 预处理算法耗时统计
    preprocess_stats = {
        'module_load': [],      # 模块加载耗时
        'algorithm_exec': [],   # 算法执行耗时
        'total': []             # 总耗时
    }
    total_start_time = time.time()

    try:
        # 解析输入
        if isinstance(task, str):
            try:
                task = json.loads(task)
            except (json.JSONDecodeError, TypeError) as e:
                return False, json.dumps({"error": f"任务JSON解析失败: {e}"})
        if not isinstance(task, dict):
            return False, json.dumps({"error": f"任务格式错误: 期望dict，实际为{type(task).__name__}"})

        steps = task.get("steps")
        data_sources = task.get("data_sources")
        if not steps or not data_sources:
            return False, json.dumps({"error": "缺少 steps 或 data_sources"})

        start_time = task.get("start_time", None)
        end_time = task.get("end_time", None)

        # 算法分类目录（默认为dataProcess）
        algorithm_category = task.get("algorithm_category", "dataProcess")

        # 从Kingbase查询数据源
        time_fields = {'time', 'timestamp'}
        dataframes = {}
        with KingbaseDB() as db:
            for table_name, fields in data_sources.items():
                fields = set(fields.keys())
                if "timestamp" not in fields:
                    fields.add('timestamp')
                rows = db.query_data(table_name, fields=fields, start_time=start_time, end_time=end_time)
                if not rows:
                    return False, json.dumps({"error": f"表 {table_name} 查询结果为空"})
                df = pd.DataFrame(rows)
                for col in df.columns:
                    if col.lower() not in time_fields:
                        df[col] = pd.to_numeric(df[col], errors='coerce')
                dataframes[table_name] = df

        ret = {}
        ret['original'] = {name: to_json_safe(df.to_dict(orient='records')) for name, df in dataframes.items()}

        # 解析模板
        current_data = dataframes
        ret['analysis'] = {}
        ret['performance'] = {}  # 存储各步骤耗时统计

        for step in steps:
            enabled = step.get('enabled', True)
            if not enabled:
                continue

            method = step.get('operator')
            raw_params = step.get('parameters', {})
            #兼容paramters为list格式和dict格式
            if isinstance(raw_params, list):
                params = {}
                for item in raw_params:
                    if 'name' not in item:
                        continue
                    val = item['value']
                    ptype = item.get('type','string')
                    if ptype == 'boolean':
                        val = str(val).lower() in ('true', '1', 'yes')
                    elif ptype == 'number':
                        val = float(val) if '.' in str(val) else int(val)
                    elif ptype == 'array':
                        if isinstance(val, str):
                            val = json.loads(val)
                    #string类型保持原样
                    params[item['name']] = val
            else:
                params = raw_params
            if not method:
                return False, json.dumps({"error": "步骤缺少 operator 字段"})
            module_path = _resolve_preprocess_module_path(step)
            if not module_path:
                return False, json.dumps(
                    {"error": f"预处理算子 {method} 缺少algs_type/algs_name，且未在算法表查询到code_url"},
                    ensure_ascii=False)

            step_start_time = time.time()

            # 动态加载算法模块
            try:
                module_load_start = time.time()
                # 使用 _get_module 动态加载（带缓存）
                func = _get_module(module_path)
                module_load_time = time.time() - module_load_start
                preprocess_stats['module_load'].append(module_load_time)
                # 执行算法
                alg_exec_start = time.time()
                success, this_step_ret = func.algsMain(data=current_data, params=params)
                alg_exec_time = time.time() - alg_exec_start
                preprocess_stats['algorithm_exec'].append(alg_exec_time)

                if not success:
                    return False, json.dumps({"error": f"预处理算子 {method} 执行失败: {this_step_ret}"})

                current_data = this_step_ret['data']
                ret['analysis'][method] = to_json_safe(this_step_ret['analysis'])

                # 记录该步骤耗时
                step_total_time = time.time() - step_start_time
                ret['performance'][method] = {
                    'module_load_time': round(module_load_time, 4),
                    'exec_time': round(alg_exec_time, 4),
                    'total_time': round(step_total_time, 4)
                }

            except Exception as e:
                return False, json.dumps({"error": f"预处理算子 {method} 执行异常: {str(e)}"})

        # 将最终数据存入结果
        batch_id = task.get("batchId")
        ret['preprocessed'] = {}
        for name, df in current_data.items():
            ret['preprocessed']["pre_" + name] = to_json_safe(df.to_dict(orient='records'))
            records = df.to_dict(orient='records')
            if batch_id is not None:
                for row in records:
                    row['batch_id'] = int(batch_id)
            s_save_preprocessed_to_kingbase("pre_" + name,records)

        # 调用字段映射关系接口，将字段信息同步到评估平台

        field_mapping = {}
        field_mapping["batchId"] = batch_id
        field_mapping_list = []
        for new_table_name in ret['preprocessed'].keys():
            matched_old_tables = []
            for old_table_name in data_sources.keys():
                if old_table_name in new_table_name:
                    matched_old_tables.append(old_table_name)
            for old_table_name in matched_old_tables:
                for field_name, field_id in data_sources[old_table_name].items():
                    field_mapping_list.append({
                        "fieldName3": field_name,
                        "id": field_id,
                        "tableName3": new_table_name
                    })
        field_mapping["relations"] = field_mapping_list
        try:
            resp = requests.post(FIELD_MAPPING_URL, json=field_mapping, timeout=10)
            resp.raise_for_status()
        except Exception as e:
            print(f"调用字段映射关系接口失败: {str(e)}")

        # 修改ycl_task表中指定task_id的status为"已完成"
        task_id = task.get("task_id")
        if task_id:
            try:
                with KingbaseDB() as db:
                    success, msg = db.update_task_status(task_id, "已完成")
                    if not success:
                        print(f"更新任务状态失败: {msg}")
            except Exception as e:
                print(f"更新任务状态异常: {str(e)}")

        # 汇总性能统计
        total_time = time.time() - total_start_time
        preprocess_stats['total'].append(total_time)
        ret['summary_performance'] = {
            'total_time': round(total_time, 4),
            'module_load_avg': round(sum(preprocess_stats['module_load']) / len(preprocess_stats['module_load']), 4) if preprocess_stats['module_load'] else 0,
            'algorithm_exec_avg': round(sum(preprocess_stats['algorithm_exec']) / len(preprocess_stats['algorithm_exec']), 4) if preprocess_stats['algorithm_exec'] else 0,
            'steps_count': len([s for s in steps if s.get('enabled', True)])
        }

        return True, json.dumps(ret, ensure_ascii=False)

    except Exception as e:
        task_id = task.get("task_id") if isinstance(task, dict) else None
        if task_id:
            try:
                with KingbaseDB() as db:
                    success, msg = db.update_task_status(task_id, "失败")
                    if not success:
                        print(f"更新任务状态失败: {msg}")
            except Exception as e:
                print(f"更新任务状态异常: {str(e)}")
        return False, json.dumps({"error": f"全局异常: {str(e)}"})


#========================================

def get_performance_stats():
    """
    获取性能统计信息
    :return: 统计字典
    """
    stats_summary = {}
    for key, values in _performance_stats.items():
        if values:
            stats_summary[key] = {
                'count': len(values),
                'total': sum(values),
                'avg': sum(values) / len(values),
                'min': min(values),
                'max': max(values)
            }
        else:
            stats_summary[key] = {'count': 0, 'total': 0, 'avg': 0, 'min': 0, 'max': 0}
    return stats_summary

def print_performance_stats():
    """
    打印性能统计信息
    """
    stats = get_performance_stats()
    print("\n" + "="*80)
    print("预处理性能统计")
    print("="*80)
    print(f"{'步骤':<25} {'次数':<10} {'总耗时(s)':<15} {'平均耗时(s)':<15} {'最小(s)':<12} {'最大(s)':<12}")
    print("-"*80)
    
    step_names = {
        'file_read': '文件读取',
        'module_import_ty': 'TY模块导入',
        'module_import_tc': 'TC模块导入',
        'data_preprocess': '数据预处理',
        'dataframe_create': 'DataFrame创建',
        'data_ffill': '数据填充',
        'data_convert': '数据转换',
        'total_per_day': '每天总耗时'
    }
    
    for key, name in step_names.items():
        stat = stats[key]
        if stat['count'] > 0:
            print(f"{name:<25} {stat['count']:<10} {stat['total']:<15.3f} {stat['avg']:<15.3f} {stat['min']:<12.3f} {stat['max']:<12.3f}")
    
    print("="*80)
    total_time = sum([stats[k]['total'] for k in step_names.keys() if k != 'total_per_day'])
    print(f"预处理总耗时: {total_time:.3f}秒")
    if stats['total_per_day']['count'] > 0:
        print(f"平均每天耗时: {stats['total_per_day']['avg']:.3f}秒")
    print("="*80 + "\n")

def reset_performance_stats():
    """
    重置性能统计
    """
    for key in _performance_stats:
        _performance_stats[key] = []

def _get_module(module_path, module_name="algsMain", enable_reload=True):
    """
    获取模块（带缓存），避免重复导入
    :param module_path: 模块路径，如 "algs.dataProcess.ty_name.ty_name"
    :param module_name: 要获取的属性名
    :param enable_reload: 是否reload模块（生产环境可设为False以提升性能）
    :return: 模块对象
    """
    cache_key = f"{module_path}.{module_name}"
    if cache_key not in _module_cache:
        _module_cache[cache_key] = import_module(module_path, module_name)
    elif enable_reload:
        # 即使缓存了，也reload以确保最新代码（开发环境）
        reload(_module_cache[cache_key])
    return _module_cache[cache_key]

"""
执行微小卫星算法库入口函数
输入参数：
    alg_type - 算法类型，此值为ALGType内的一个属性，通过此值确定具体实例化调用哪个算法的实现
    param - 算法数据和参数内容，此参数为一个字典结构，具体内容如下：
        {
            data:[]     用于计算的原始数据，此数据为列表结构，具体结构参考对应算法介绍
            config:{}   用于算法内部参数调整，此数据为字典结构，具体内容参考对应算法介绍
        }

返回值：
    如果返回为None 则算法调用失败，成功返回值请参考对应算法介绍
"""


class ALGType:
    """ 统计分析
        传入参数：
            data - 结构为二维度素组 ，第一行为字段名称类型说明，结构为:[("名称1"，类型1),("名称2"，类型2)...]
                整体数据样例如下：[[("字段1",int),("字段2",int),("字段3",int),("字段4",int)],(1,2,3,14),(7,5,6,17),(4,8,9,10)]

            config - 结构如下：
            {
                refer_field:字符串， 统计标尺字段，用于对数据进行统计分组
                stat_range:整数|二维数组，当值为数值，标尺字段的最小值开始，每stat_range值为一组
                                        当值为二维数组时，则按照二维数组来划分统计分组 ,结构为[[开始值1,结束值1]，[开始值2,结束值2],[开始值3,结束值3]...]
                                        样例如下： [[0,3],[4,9],[10,30]]

                condition：数组，结构为条件对象数组，统计分析的过程按照条件来统计结果信息,结构为 [{"统计条件名称",."统计表达式"},...]
                           统计表达式为字符串，格式如下：“{字段名称1} 运算符 目标数值或{字段名称2}” 样例如下：
                           "({字段1} > 3) or ({字段2} <= {字段1}) and ({字段1} > {字段2}*4)"
            }
,
        返回值：
            返回值是对象数组 ,结构为
            [{
                start:数值  统计分组开始值
                end:数值   统计分组结束值
                stat: 对象数组 统计结果，
                    [
                        “统计条件名称1”:{
                                        count :数值 分组内符合条件记录数
                                        rate : 频率统计值
                                    }，
                         “统计条件名称2”:{
                                        count :数值 分组内符合条件记录数
                                        rate : 频率统计值
                                    }
                    ]
            }]
            实际样例如下：
            [{'end': 6,
              'start': 1,
              'stat': [{'数学成绩': {'count': 2, 'rate': 0.6666666666666666},
                        '语文成绩': {'count': 1, 'rate': 0.3333333333333333}}]},
             {'end': 11,
              'start': 6,
              'stat': [{'数学成绩': {'count': 1, 'rate': 0.3333333333333333},
                        '语文成绩': {'count': 1, 'rate': 0.3333333333333333}}]},
             {'end': 15,
              'start': 11,
              'stat': [{'数学成绩': {'count': 0, 'rate': 0}, '语文成绩': {'count': 0, 'rate': 0}}]}]

    """
    STAT_ANALYSE = 0,  # 统计分析算法

    """ 滑动窗口算法
        传入参数：
            data - 结构为一维度素组 ，例如 [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
            config - 结构如下：
            {
                window_size:整数，滑动窗口大小，默认为3，必须大于等于1
                step_size：整数，滑动窗口步长，默认为1，必须大于等于1
                dropna：布尔值，是否删除含有缺失值的行，默认为0不删除，设为1则删除含有缺失值的行
                method：整数，方法选择，默认为0，表示使用滑动平均；为1时表示使用滑动求和
            }

        返回值：
            返回值是二维数组 ,结构为[[数据项1，计算结果1],[数据项2，计算结果2]...], 例如：
            [[0.0, nan], [1.0, nan], [2.0, 1.0], [3.0, 2.0], [4.0, 3.0], [5.0, 4.0], [6.0, 5.0], [7.0, 6.0], [8.0, 7.0], [9.0, 8.0]]


        注意事项：
            window_size和step_size必须大于等于1，否则会报错
            dropna为布尔值，只能为0或1，不得传入其他值
            返回的列表中可能含有缺失值，根据dropna的值来决定是否删除含有缺失值的行
            滑动平均和滑动求和的结果列名称分别为“滑动平均”和“滑动求和”
    """
    SLIDE_WINDOW = 1,  # 滑动窗口算法

    VERTICAL_HORIZONTAL_COMPARE = 2,  # 纵向横向比较算法

    INTERVALIZE = 3  # 量纲化之区间化

    RANGE_METHOD = 4  # 量纲化之归一化

    CAL_WEIGHT = 5  # 熵值法


# def wxwx_alg_exec(alg_type, param):
#     if alg_type == ALGType.CAL_WEIGHT:
#         return cal_weight(param)

def parse_data_from_txt(sat_id_list, storage_dir, ty_name, tc_name, relList, print_stats=True, reset_stats=True):
    '''
    取遥测原始数据并处理（批量优化版本 - 关键优化：减少90%的重复操作）
    关键优化点：
    1. 模块只导入一次（原来90天要导入180次，现在只需2次）
    2. 预配置pandas读取参数，减少循环开销
    3. 批量处理优化
    :param sat_id_list: 卫星ID列表
    :param storage_dir: 存储目录
    :param ty_name: ty算法名称
    :param tc_name: tc算法名称
    :param relList: 关联列表
    :param print_stats: 是否在处理完成后打印性能统计（默认True）
    :param reset_stats: 是否在处理开始前重置统计（默认True）
    :return:
    '''
    try:
        if reset_stats:
            reset_performance_stats()
        # 优化1：预配置pandas读取参数（避免每次循环都创建字典）
        read_params_base = {
            'header': None,
            'names': ['time', 'data'],
            'sep': ',',
            'engine': 'c',
            'dtype': {'time': float, 'data': float},
            'skipinitialspace': True
        }
        # 检测pandas版本（pandas 2.0+使用on_bad_lines，旧版本使用error_bad_lines）
        try:
            pd_version = pd.__version__.split('.')[0]
            use_on_bad_lines = int(pd_version) >= 2
        except:
            use_on_bad_lines = False
        
        result_list = []
        dir_names = sorted(os.listdir(storage_dir))  # 排序保证顺序一致
        
        # 优化2：批量处理所有天的数据
        for dir_name in dir_names:
            day_start_time = time.time()  # 记录每天开始时间
            tel_list = []
            time_list = []
            data_by_day = []
            
            # 批量读取所有sat_id的文件
            file_read_start = time.time()
            for sat_id in sat_id_list:
                file_name = os.path.join(storage_dir, dir_name, f"{sat_id}.txt")
                
                # 使用pandas直接读取CSV
                try:
                    read_params = read_params_base.copy()
                    read_params['filepath_or_buffer'] = file_name
                    
                    if use_on_bad_lines:
                        try:
                            df = pd.read_csv(**read_params, on_bad_lines='skip')
                        except TypeError:
                            read_params['error_bad_lines'] = False
                            read_params['warn_bad_lines'] = False
                            df = pd.read_csv(**read_params)
                    else:
                        read_params['error_bad_lines'] = False
                        read_params['warn_bad_lines'] = False
                        df = pd.read_csv(**read_params)
                    
                    # 优化：保持numpy数组，n_sigma函数已支持numpy数组输入
                    # 只在最后需要list时才转换，减少中间转换开销
                    time_data = df['time'].dropna().values  # 保持numpy数组
                    tel_data = df['data'].dropna().values   # 保持numpy数组
                except Exception:
                    # 备选方案：numpy
                    try:
                        data_array = np.loadtxt(file_name, delimiter=',', dtype=float, ndmin=2)
                        time_data = data_array[:, 0]  # 保持numpy数组
                        tel_data = data_array[:, 1]   # 保持numpy数组
                    except Exception:
                        # 最后备选：字符串解析
                        with open(file_name, "r") as f:
                            lines = f.readlines()
                        time_data = []
                        tel_data = []
                        for line in lines:
                            line = line.strip()
                            if line:
                                parts = line.split(',', 1)
                                if len(parts) == 2:
                                    try:
                                        time_data.append(float(parts[0]))
                                        tel_data.append(float(parts[1]))
                                    except ValueError:
                                        continue
                
                tel_list.append(tel_data)
                time_list.append(time_data)
            
            file_read_time = time.time() - file_read_start
            _performance_stats['file_read'].append(file_read_time)
            
            st, result = combine_dataframe_format(time_list, tel_list, sat_id_list, ty_name, tc_name, relList)
            if st:
                # 优化：批量转换，减少循环开销
                data_convert_start = time.time()
                data_by_day.append(result.index.values.tolist())
                data_by_day.extend([result[sat_id].values.tolist() for sat_id in sat_id_list])
                data_convert_time = time.time() - data_convert_start
                _performance_stats['data_convert'].append(data_convert_time)
            else:
                return False, result
            result_list.append(data_by_day)
            
            # 记录每天总耗时
            day_total_time = time.time() - day_start_time
            _performance_stats['total_per_day'].append(day_total_time)
        
        # 打印性能统计
        if print_stats:
            print_performance_stats()
        
        return True, result_list

        # result_list = []
        # for sat_id in sat_id_list:
        #     data_by_tel = []
        #     for dir_name in os.listdir(storage_dir):
        #         file_name = storage_dir + dir_name + "\\" + str(sat_id) + ".txt"
        #         with open(file_name, "r") as f:
        #             all_data = f.read()
        #         # time_data = [eval(x.split(",")[0]) for x in all_data.split("\n") if x != ""]
        #         tel_data = [eval(x.split(",")[1]) for x in all_data.split("\n") if x != ""]
        #         data_by_tel.append(tel_data)
        #     result_list.append(data_by_tel)
        # return True, result_list
    except Exception as e:
        return False, str(e.args)


def n_sigma(time, data, n, count):
    """
    深度优化版本：减少不必要的迭代和转换，提前退出
    支持numpy数组和list输入，自动处理类型转换
    优化点：
    1. 使用更高效的统计计算
    2. 减少不必要的数组复制
    3. 提前退出机制
    """
    # 如果已经是numpy数组，避免重复转换
    if not isinstance(time, np.ndarray):
        time = np.array(time, dtype=np.float64)
    if not isinstance(data, np.ndarray):
        data = np.array(data, dtype=np.float64)
    
    data_size = len(data)
    if data_size == 0:
        return time.tolist(), data.tolist()
    
    # 优化：预计算，避免重复计算
    for iteration in range(count):
        # 优化：使用nanmean和nanstd，但避免重复计算
        # 如果数据量很大，可以考虑使用更高效的算法
        valid_mask = ~np.isnan(data)
        if not valid_mask.any():
            break
            
        valid_data = data[valid_mask]
        if len(valid_data) == 0:
            break
            
        average = np.mean(valid_data)  # 已经过滤了nan，直接用mean更快
        std = np.std(valid_data, ddof=0)  # ddof=0更快
        
        if std == 0 or np.isnan(std):  # 标准差为0，所有数据相同
            break
        
        # 使用布尔索引
        threshold_upper = average + n * std
        threshold_lower = average - n * std
        mask = (data <= threshold_upper) & (data >= threshold_lower) & valid_mask
        
        # 如果没有任何数据被剔除，提前退出
        if np.all(mask):
            break
            
        # 更新数据（使用视图而不是复制，如果可能）
        time = time[mask]
        data = data[mask]
        
        # 如果数据量没有变化，提前退出
        new_size = len(data)
        if new_size == data_size:
            break
        data_size = new_size
    
    return time.tolist(), data.tolist()


def iter_n_sigma(time, data, n):
    """
    优化版本：使用布尔索引代替del操作，性能提升10-100倍
    """
    data = np.array(data)
    time = np.array(time)
    
    average = np.nanmean(data)
    std = np.nanstd(data)
    
    # 使用布尔索引，O(n)复杂度，避免del操作的O(n²)复杂度
    mask = (data <= (average + n * std)) & (data >= (average - n * std))
    
    # 直接返回过滤后的数据
    return time[mask].tolist(), data[mask].tolist()

#
# def three_sigma(time, data):
#     """
#     优化版本：减少不必要的迭代和转换，提前退出
#     """
#     count = 5
#     data_size = len(data)
#     time = np.array(time)
#     data = np.array(data)
#
#     for _ in range(count):
#         average = np.nanmean(data)
#         std = np.nanstd(data)
#
#         # 使用布尔索引
#         mask = (data <= (average + 3 * std)) & (data >= (average - 3 * std)) & (np.abs(data) <= 10000)
#
#         # 如果没有任何数据被剔除，提前退出
#         if np.all(mask):
#             break
#
#         # 更新数据
#         time = time[mask]
#         data = data[mask]
#
#         # 如果数据量没有变化，提前退出
#         if len(data) == data_size:
#             break
#         data_size = len(data)
#
#     return time.tolist(), data.tolist()


def iter_three_sigma(time, data):
    """
    优化版本：使用布尔索引代替del操作，性能提升10-100倍
    """
    data = np.array(data)
    time = np.array(time)
    
    average = np.nanmean(data)
    std = np.nanstd(data)
    
    # 使用布尔索引，O(n)复杂度
    mask = (data <= (average + 3 * std)) & (data >= (average - 3 * std)) & (np.abs(data) <= 10000)
    
    # 直接返回过滤后的数据
    return time[mask].tolist(), data[mask].tolist()


def combine_dataframe_format(timelist, datalist, idlist, ty_name, tc_name, relList):
    '''
    遥测对齐（优化版本 - 使用模块缓存，减少导入开销）
    :param timelist:
    :param datalist:
    :param idlist:
    :return:
    '''
    try:
        # 优化：使用缓存的模块导入，避免90天重复导入180次
        module_import_ty_start = time.time()
        ty_module_path = "algs" + ".dataProcess." + ty_name + '.' + ty_name
        ins_algs = _get_module(ty_module_path, "algsMain")
        module_import_ty_time = time.time() - module_import_ty_start
        _performance_stats['module_import_ty'].append(module_import_ty_time)
        
        five_sigma_st = False
        relIdx = 0
        for tmpidx in range(len(timelist)):
            if idlist[tmpidx] in relList:
                five_sigma_st = True
                relIdx = tmpidx
                break

        # 数据预处理耗时统计
        data_preprocess_start = time.time()
        if five_sigma_st is True:
            for tmpidx in range(len(timelist)):
                if 'noTy' in relList and relIdx == tmpidx:
                    # timelist[tmpidx], datalist[tmpidx] = n_sigma(timelist[tmpidx], datalist[tmpidx], 7, 1)
                    pass
                elif 'doTy' in relList:
                    timelist[tmpidx], datalist[tmpidx] = n_sigma(timelist[tmpidx], datalist[tmpidx], 3, 5)
                else:
                    timelist[tmpidx], datalist[tmpidx] = n_sigma(timelist[tmpidx], datalist[tmpidx], 5, 1)
        else:
            for tmpidx in range(len(timelist)):
                timelist[tmpidx], datalist[tmpidx] = ins_algs.algsMain(timelist[tmpidx], datalist[tmpidx])[1]
        data_preprocess_time = time.time() - data_preprocess_start
        _performance_stats['data_preprocess'].append(data_preprocess_time)

        # 优化：一次性创建DataFrame并去重，避免多次转换
        dataframe_create_start = time.time()
        res = []
        for idx, (time_data, data) in enumerate(zip(timelist, datalist)):
            # 确保time_data和data是list（DataFrame需要）
            # 注意：使用time_data避免与time模块冲突
            if isinstance(time_data, np.ndarray):
                time_data = time_data.tolist()
            if isinstance(data, np.ndarray):
                data = data.tolist()
            
            # 优化：先对index去重，再创建DataFrame，避免创建后去重的开销
            # 使用字典去重（保留第一个），比DataFrame去重快
            if len(time_data) != len(set(time_data)):  # 有重复值才处理
                seen = set()
                unique_time = []
                unique_data = []
                for t, d in zip(time_data, data):
                    if t not in seen:
                        seen.add(t)
                        unique_time.append(t)
                        unique_data.append(d)
                time_data = unique_time
                data = unique_data
            
            # 创建DataFrame
            df = pd.DataFrame({idlist[idx]: data}, index=time_data)
            res.append(df)
        dataframe_create_time = time.time() - dataframe_create_start
        _performance_stats['dataframe_create'].append(dataframe_create_time)

        # 优化：使用缓存的模块导入
        module_import_tc_start = time.time()
        tc_module_path = "algs" + ".dataProcess." + tc_name + '.' + tc_name
        ins_algs = _get_module(tc_module_path, "algsMain")
        module_import_tc_time = time.time() - module_import_tc_start
        _performance_stats['module_import_tc'].append(module_import_tc_time)
        
        # 数据填充耗时统计
        data_ffill_start = time.time()
        status, data = ins_algs.algsMain(res)
        data_ffill_time = time.time() - data_ffill_start
        _performance_stats['data_ffill'].append(data_ffill_time)

        # status, data = data_prehandle(res)
        # print("res", res)
        if status == False:
            return False, data
        return True, data
    except Exception as e:
        return False, e.args


def data_prehandle(dataframes):
    '''
    填充或剔除（优化版本）
    :param dataframes:
    :return:
    '''
    try:
        if len(dataframes) == 0:
            return False, "Empty dataframes"
        
        # 优化：使用pd.concat代替循环merge，性能提升5-20倍
        res_merge = pd.concat(dataframes, axis=1, join='outer')
        
        res_merge.replace([np.inf, -np.inf], np.nan, inplace=True)
        res_merge = res_merge.ffill()  # 使用ffill()代替已弃用的fillna(method="ffill")
        res_merge = res_merge.dropna(how="any")
        return True, res_merge
    except Exception as e:
        return False, e.args


def s_algs_load(task):
    try:
        # input_data = json.loads(request.get_data())
        config = eval(task["config"])
        algs_name = task["algs_name"]
        algs_type = task["algs_type"]
        ins_algs = import_module("algs" + "." + algs_type + "." + algs_name + '.' + algs_name,
                                 "algsMain")  # 根据算法类型去不同的文件夹下导入
        reload(ins_algs)
        relList = []
        for key in config.keys():
            relList.append(config[key])
        if task["dataType"] == "0":  # dataType==0时输入数据不为遥测数据   dataType==1时输入数据为遥测数据，根据路径找本地文件获取遥测
            config = {
                "config": config
            }

            data = eval(task["data"])
            # return "success"
            # print(data)

            if algs_type == "weight":
                if str(type(data[0])) == "<class 'list'>":
                    size = len(data[0])
                    data = pd.DataFrame(data)

                    data = data.replace("null", np.nan)
                    data = data.dropna()
                    if data.empty:
                        data = [[1 for i in range(size)]]
                    else:
                        data = [list(data.iloc[i]) for i in range(len(data.index))]

            st_alg, payload = ins_algs.algsMain(data, **config)
            if st_alg is False:
                return False, payload
            return True, payload
        else:
            config_new = {
                "config": config
            }
            if "notel" in task.keys() and task["notel"] == 1:
                data = eval(task["data"])
                st_alg, ret = ins_algs.algsMain(data, **config_new)
                if st_alg is False:
                    return False, ret
                return True, ret
            ty_name = config["ty_name"]
            tc_name = config["tc_name"]
            tel_name_list = eval(task["data"])
            config_new["config"]["data"] = tel_name_list
            tel_storage_path = task["dataPath"]
            if "basePath" in task.keys():
                tel_base_path = task["basePath"]
                st_base, base_data = parse_data_from_txt(tel_name_list, tel_base_path, ty_name, tc_name, relList)
                config_new["config"]["base_data"] = base_data
        st, data = parse_data_from_txt(tel_name_list, tel_storage_path, ty_name, tc_name, relList)
        # 判断关联的遥测参数在idList的索引
        for key in config.keys():
            if config[key] in tel_name_list:
                for i, item in enumerate(tel_name_list):
                    if config[key] == item:
                        config_new["config"][key] = i
        ret = None
        if st:
            status,ret = ins_algs.algsMain(data, **config_new)
            if status==False:
                return False, ret
            # return "success"
        else:
            return False, data
        return True, ret
    except Exception as e:
        return False, e.args


def s_loss(task):
    '''
    退化模型
    :param task:
    :return:
    '''
    try:
        #############################
        dataPath = task["dataPath"]
        spit_time = task["spit_time"]
        spit_range = task["round"]
        targetFields = task["targetFields"]
        able_out_dl = task["able_out_dl"]
        able_in_dl = task["able_in_dl"]
        able_all_dy = task["able_all_dy"]
        hlRule = task["hlRule"]
        hyRule = task["hyRule"]
        fdRule = task["fdRule"]
        ###############################

        # 退化训练
        tuihua = Tuihua(spit_time,spit_range,targetFields,able_out_dl,able_in_dl,able_all_dy,hlRule,hyRule,fdRule)
        ret=tuihua.train(dataPath)

        return True, ret
    except Exception as e:
        return False, {"msg": e.args}


def s_predict(task):
    '''
    趋势预测
    :param task:
    :return:
    '''
    try:
        #############################
        dataPath = task["dataPath"]
        modelPath = task["modelPath"]
        handleType = task["handleType"]  # 1表示单参数预测，2 表示训练，3表示结果
        predictdays = task["predictdays"]  # 预测天数
        linenums = task["linenums"]  # 线数
        tel = task["tel"]  # 目标遥测 训练时传入的是遥测id字符串列表，预测时列表中只能有一个
        rel_tel = task["rel_tel"]
        starttime = task["starttime"]
        predictTime = task["predictTime"]
        bigger = None
        if "bigger" in task.keys():
            bigger = task["bigger"]  # 门限规则，如bigger=5  --> x<5  smaller=2  -- > x > 2    都不为None则2<x<5
        smaller = None
        if "smaller" in task.keys():
            smaller = task["smaller"]

        ###############################

        if handleType == 1:  # 单参数趋势预测
            fixp = task["fixp"]
            qushi = Qushi(dataPath, tel, starttime)
            # 未来天数，线数，目标遥测，修正
            ret = qushi.dealData(predictdays, linenums, fixp)
            return True, ret
        elif handleType == 2:  # 关联预测训练
            # #模型训练
            qushi = Qushi2(dataPath, modelPath, tel, rel_tel)
            qushi.train(dataPath, bigger, smaller)

            # tel_list = ["1026", "1027", "1030", "1034", "1035", "1040", "1043", "1162", "9216", "9222", "9226"]
            # res_list = qushi.choose2(tel_list)
            # for i in range(len(tel_list)):
            #     print(str(tel_list[i]) + "关联性为:" + str(np.nanmean(res_list[i])) + "\n")

            return True, {"msg": "训练成功"}
        elif handleType == 3:  # 关联预测模型结果输出
            # 生产使用
            final_res = {}
            for item in tel:
                qushi = Qushi2(dataPath, modelPath, [item], rel_tel)
                qushi.load_model(modelPath)
                # 开始时间，预测天，步长
                # res = qushi.forword(predictTime, predictdays, 60)

                res = qushi.forwardValRange([item], predictTime, predictdays)
                res = qushi.huatu(res, predictTime, predictdays)
                final_res.update(res)
            return True, final_res
        elif handleType == 4:  # 计算遥测之间的关联度
            qushi = Qushi2(dataPath, modelPath, tel, rel_tel)
            res_list = qushi.choose2(tel)
            res_tel = tel[0]
            max_relevance = np.nanmean(res_list[0])
            for i in range(len(tel)):
                relevance = np.nanmean(res_list[i])
                print(str(tel[i]) + "关联性为:" + str(relevance) + "\n")
                if relevance > max_relevance:
                    max_relevance = relevance
                    res_tel = tel[i]


            # 分析图
            # qushi.draw_pic("1030")
            # qushi.draw_pic("1031")
            # qushi.draw_pic("1034")
            # qushi.draw_pic("1035")
            # qushi.draw_pic("1040")
            # qushi.draw_pic("1041")
            # qushi.draw_pic("1043")
            # qushi.draw_pic("1162")
            # qushi.draw_pic("1164")
            # qushi.draw_pic("9216")
            # qushi.draw_pic("9222")
            # qushi.draw_pic("9226")
            # qushi.draw_pic("9230")
            # qushi.draw_pic("9565")
            # qushi.draw_pic("9570")
            return True, {"res_tel": res_tel}
    except Exception as e:
        return False, {"msg": e.args}


def s_network(task):
    '''
    神经网络
    :param task:
    :return:
    '''
    try:
        #############################
        dataPath = task["dataPath"]
        modelPath = task["modelPath"]
        handleType = task["handleType"]  # 1 表示bp训练，2表示bp结果 3表示rnn训练，4表示rnn结果
        predictdata = task["predictdata"]
        # datay = task["datay"]
        ###############################

        if handleType == 1:
            bp = BpModel(dataPath, modelPath,dataPath)
            print("train")

            # bp.train(dataPath, epochs=2000, batchsize=300,
            bp.train(dataPath, epochs=task["epochs"],lr=task["lr"])  #训练接口  epochs：训练轮次  lr：神经网络参数
            # print("test")
            # bp.test(dataPath)  # 测试
            return True, {"msg": "训练成功"}
        elif handleType == 2:
            print("new forword")
            nbp = BpModel(dataPath, modelPath,dataPath)
            nbp.load_model(modelPath)  # 加载模型

            ret = nbp.forword(eval(predictdata))  # 识别数据

            starttime = task["startTime"]
            endtime = task["endTime"]

            beginstamp = timeToStamp(starttime)
            endstamp = timeToStamp(endtime)
            times = [starttime]
            days = int((endstamp - beginstamp) / 86400)
            for day in range(days):
                beginstamp += 86400
                tmptime = stampToTime(beginstamp)
                times.append(tmptime)

            return True, {"预测结果": {"x": times, "y": ret}}
        elif handleType == 3:
            # 训练步骤
            deepnet = RNNDeepNetModel(dataPath, modelPath,dataPath)
            print("train")
            deepnet.train(dataPath, epochs=task["epochs"], batchsize=task["batchSize"], lr=task["lr"])
            return True, {"msg": "训练成功"}
            # print("test")
            # deepnet.test(dataPath)
        elif handleType == 4:
            # 识别步骤
            print("new forword")
            ndeepnet = RNNDeepNetModel(dataPath, modelPath,dataPath)
            ndeepnet.load_model(modelPath)

            ret,idx_list = ndeepnet.forword(eval(predictdata))
            starttime = task["startTime"]
            endtime = task["endTime"]

            beginstamp = timeToStamp(starttime)
            endstamp = timeToStamp(endtime)
            times = [starttime]
            days = int((endstamp - beginstamp) / 86400)
            for day in range(days):
                beginstamp += 86400
                tmptime = stampToTime(beginstamp)
                if day in idx_list:
                    times.append(tmptime)

            return True, {"预测结果": {"x": times, "y": ret}}
            pass

        pass
    except Exception as e:
        return False, {"msg": e.args}


def s_getOrbitPic(task):
    try:
        return getOrbitPic(task)
    except Exception as e:
        return False, {"msg": e.args}


if __name__ == '__main__':
    # data = {'data': '[["-0-","-1-","-2-","-3-","-4-","-5-","-6-","-7-","-8-","-9-","-10-","-11-"],[0.06,0.06,0.06,0.06,0.06,0.06,0.06,0.2,0.06,0.06,0.06,0.2],[0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.2,0.5]]', 'dataType': '0', 'algs_name': 'evidence_fusionMethod', 'config': '{}', 'algs_type': 'weight'}
    # dat1 = {'data': '[["-0-","-1-","-2-","-3-","-4-","-5-","-6-","-7-","-8-","-9-","-10-","-11-"],[0.06,0.06,0.06,0.06,0.06,0.06,0.06,0.2,0.06,0.06,0.06,0.2],[0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.03,0.2,0.5]]', 'dataType': '0', 'algs_name': 'evidence_fusionMethod', 'config': '{}', 'algs_type': 'weight'}
    # data = {'data': '["null","null","null","null","null","null","null","null","null","null","null","null","null","null","null","null",99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903]', 'dataType': '0', 'algs_name': 'cal_weight', 'config': '{"k":0.0,"b":"0","nocancha":1,"roundTFlag":0,"min":0.0,"max":0.0,"zgpg_start_time":"2018-12-09 00:00:00","zgpg_end_time":"2019-01-10 00:00:00","sat_type":"BD3G01-2","indicatorType":1,"ty_name":"three_sigma","tc_name":"data_ffill","base_data":["None","None","None","None","None","None","None","None","None","None","None","None","None","None","None","None",99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,99.03,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903,0.9903]}', 'algs_type': 'weight'}
    # data = {'data':'[["null", 1, 0.9],[0.5,0.3,0.3],["null", "null", 1]]'}
    # data = {'data': '[9315]', 'basePath': 'D:\\zgpg\\ruoyi\\ruoyi-system\\teldata\\weight\\30a8c4c1-35e3-46f6-927f-795c6d4df861\\', 'dataType': '1', 'notel': 0, 'algs_name': 'statistic', 'config': '{"":"","nocancha":1,"roundTFlag":0,"k":0.0,"zgpg_start_time":"2019-08-01 00:00:00","zgpg_end_time":"2019-08-31 23:59:59","sat_type":"BD3G01-3","indicatorType":1,"ty_name":"three_sigma","tc_name":"data_ffill"}', 'algs_type': 'norm', 'dataPath': 'D:\\zgpg\\ruoyi\\ruoyi-system\\teldata\\weight\\30a8c4c1-35e3-46f6-927f-795c6d4df861\\'}

    #
    #
    #
    #
    data= {'data': '[144, 143]', 'dataType': '1', 'notel': 0, 'algs_name': 'marginOfCurCircleEnergyByPower', 'config': '{"P_Load":143,"P_Charge":-1,"P_Branch":-1,"P_Loss":-1,"illumination_min":"0.01","illumination_max":"","count":"5","I_SA":-1,"nocancha":1,"roundTFlag":0,"k":1.0,"zgpg_start_time":"2018-12-09 00:00:00","zgpg_end_time":"2019-01-10 23:59:59","sat_type":"BD3G01-2","indicatorType":1,"ty_name":"three_sigma","tc_name":"data_ffill"}', 'algs_type': 'character', 'dataPath': 'D:\\zgpg\\ruoyi\\ruoyi-system\\telData/weight/cadc20e2-8077-4982-a3c6-b8171c920de9/'}
    print(s_algs_load(data))


    # import logging
    #
    # log = logging.Logger("service")
    # log.info(log, "打印日志开始")



    # data2={'hlRule': ' 4.099062 <= {1028} <= 4.4068188 and {1030} < 88.5 and {1026} < 0.6',
    #        'round': 24, 'handleType': 1, 'able_out_dl': [0.5, 40],
    #        'hyRule': ' -0.0034532 < {1028} <= 4.99062 and 88.5 <= {1030} and {1026} < 0.6', 'spit_time': 12, 'modelPath': '', 'fdRule': ' {1030} < 89 and 0.7 < {1026} and {1028} < -0.0034532', 'targetFields': {'out_dl_field': '1026', 'in_dl_field': '1028', 'all_dy_field': '1030'}, 'able_all_dy': [75, 90], 'dataPath': 'D:\\zgpg\\ruoyi\\ruoyi-system\\teldata\\weight\\9e3d7495-52c0-47db-aa6b-2ca22e820b60\\', 'able_in_dl': [-1.5, 4.5]}
    # # 退化
    # data1={
    #     "dataPath": "D:/cc/tuihua/tel",
    #     "spit_time":12,
    #     "round": 24,
    #     "targetFields":{"out_dl_field":"1026", "in_dl_field":"1028","all_dy_field":"1030"},  #放电电流，充电电流，整租电压
    #     "able_out_dl": [0.5,40],   #放电电流有效区间
    #     "able_in_dl":[-1.5,4.5],   #充电电流有效区间
    #     "able_all_dy":[75,90],     #电压有效区间
    #     "hlRule": "4.099062 <= {1028} <= 4.4068188  and {1030} < 88.5 and {1026} < 0.6",  #恒流充电规则
    #     "hyRule":"-0.003453200000000006 <{1028} < 4.099062  and {1030} >= 88.5 and {1026} < 0.6",  #恒压充电规则
    #     "fdRule":" {1030} < 89 and {1026} > 0.7 and {1028} < -0.003453200000000006"  #放电规则
    #
    # }
    # s_loss(data1)

    # data = {'linenums': 1, 'handleType': 3, 'predictTime': '2022-01-02 00:00:00', 'predictdays': 366, 'modelPath': 'D:\\zgpg\\ruoyi\\ruoyi-system\\pt/b07bfceb50cd48ccad63cbd5a06b6216.pt', 'tel': ['9570'], 'rel_tel': 1030, 'starttime': '', 'dataPath': ''}
    #
    # s_predict(data)

    # data = {'data': '[10269, 10272]', 'dataType': '1', 'notel': 0, 'algs_name': 'attitudeControlAccuracy', 'config': '{"attitudeMode":10272,"ang_append1":-1,"ang_append2":-1,"ang_append3":-1,"flagValue":"6","note":"noTy","diffValue":"0.001","count":"10","calType":"0","nocancha":1,"roundTFlag":0,"k":1.0,"zgpg_start_time":"2023-01-01 00:00:00","zgpg_end_time":"2023-06-28 00:00:00","sat_type":"BD3G01-2","indicatorType":1,"ty_name":"three_sigma","tc_name":"data_ffill"}', 'algs_type': 'character', 'dataPath': 'D:\\zgpg\\ruoyi\\ruoyi-system\\telData/weight/ba2f122a-16af-48c7-8390-e7fe3b0eb66e/'}
    #
    # print(s_predict(data))

    # data = {'endTime': '2023-07-02 00:00:00', 'startTime': '2023-07-01 00:00:00', 'orbitElementUrl': 'http://5.30.73.79:8085/xwsb/sync/orbitelement/getHistortyOrbitelement', 'orbitPropagatorUrl': 'http://5.30.73.87:8083/algorithmOut/input'}
    #
    #
    # print(s_getOrbitPic(data))






    '''
    退化： 选退化的相关遥测，多个
            关联遥测：第一个是蓄电池组放电电流主份，第二个是蓄电池组充电电流主份
            "datax": [1776182398, 1807286398, 1838044798],
             "datay": [770.31592989, 600, 100],
    趋势预测：单   ："predictdays": 300,  # 预测天数
                "linenums": 1, 
                "tel": ["1034", "1035","1030","1031","1040","1041","1043","1162","1164",
                        "9216","9222","9226","9230","9565","9570"],  #预测的目标遥测
                 "rel_tel": "",    #不存在关联遥测
                "starttime": "2023-06-01 00:00:00"
                “endtime”
            多  ："predictdays": 2,  # 预测天数
                "linenums": 1, #不需要
                "tel": ["1034", "1035","1030","1031","1040","1041","1043","1162","1164",
                        "9216","9222","9226","9230","9565","9570"],  #预测的目标遥测
                 "rel_tel": "",    #关联遥测
                "starttime": "2023-06-01 00:00:00"
                “endtime”
    神经网络： 评估任务id
                starttime： 待预测开始时间
                endtime：待预测结束时间
    '''
