import pandas as pd


def algsMain(*args, **kwargs):
    """
    范围一致性检验：检查指定字段是否为数值型且值在指定范围内。
    sources 为 None 时检查所有数据表。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要检查的数据源名称列表，None 表示所有数据源。
            field: str, 要检查的字段名。
            min_val: float, 允许的最小值。
            max_val: float, 允许的最大值。
    :return: (bool, result) 成功返回检查结果，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        field = params.get('field')
        min_val = params.get('min_val')
        max_val = params.get('max_val')

        if field is None:
            raise ValueError("必须提供 field 参数")
        if min_val is None or max_val is None:
            raise ValueError("必须提供 min_val 和 max_val 参数")
        if min_val > max_val:
            raise ValueError(f"min_val ({min_val}) 不能大于 max_val ({max_val})")

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")

        target_sources = sources if sources is not None else list(data.keys())
        analysis = {}

        for name in target_sources:
            df = data[name]
            has_field = field in df.columns
            is_numeric = False
            out_of_range_count = 0
            non_numeric_count = 0
            total_checked = 0

            if has_field:
                s = df[field]
                is_numeric = pd.api.types.is_numeric_dtype(s)
                if is_numeric:
                    valid = s.dropna()
                    total_checked = len(valid)
                    out_of_range_count = int(((valid < min_val) | (valid > max_val)).sum())
                else:
                    non_numeric_count = int(s.notna().sum())

            consistent = has_field and is_numeric and out_of_range_count == 0 and non_numeric_count == 0

            analysis[name] = {
                'field_exists': has_field,
                'is_numeric': is_numeric,
                'consistent': consistent,
                'total_checked': total_checked,
                'out_of_range_count': out_of_range_count,
                'non_numeric_count': non_numeric_count
            }

        ret = {
            'data': data,
            'analysis': {
                'sources': sources,
                'field': field,
                'min_val': min_val,
                'max_val': max_val,
                'details': analysis
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)
