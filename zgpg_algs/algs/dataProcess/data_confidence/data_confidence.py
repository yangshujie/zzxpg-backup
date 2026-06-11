import pandas as pd
import numpy as np
from collections import Counter


def algsMain(*args, **kwargs):
    """
    数据置信度评估算子：按完整性、准确性、一致性、冗余性四个核心维度评估数据质量。
    不修改原始数据。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要评估的数据源名称列表，None 表示所有数据源。
            rules: list[str]|None, 逻辑一致性规则列表，如 ["col_a <= col_b"]，默认 None。
            expected_types: dict|None, 期望的字段类型映射，如 {"age": "numeric"}，默认 None。
            expected_ranges: dict|None, 期望的字段范围映射，如 {"age": {"min": 0, "max": 150}}，默认 None。
    :return: (bool, result) 成功返回评估结果，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        rules = params.get('rules')
        expected_types = params.get('expected_types')
        expected_ranges = params.get('expected_ranges')

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")

        target_sources = sources if sources is not None else list(data.keys())
        details = {}

        for name in target_sources:
            df = data[name]
            dims = {}

            # 1. 完整性
            dims['completeness'] = _assess_completeness(df)

            # 2. 准确性
            dims['accuracy'] = _assess_accuracy(df, expected_types, expected_ranges)

            # 3. 一致性
            dims['consistency'] = _assess_consistency(df, rules)

            # 4. 冗余性
            dims['redundancy'] = _assess_redundancy(df)

            # 综合置信度得分（四维度等权平均）
            scores = [dims[d]['score'] for d in ['completeness', 'accuracy', 'consistency', 'redundancy']]
            dims['overall_score'] = round(sum(scores) / len(scores), 4)

            details[name] = dims

        ret = {
            'data': data,
            'analysis': {
                'sources': sources,
                'details': details
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)


# ── 完整性 ─────────────────────────────────────────────

def _assess_completeness(df):
    total_cells = df.shape[0] * df.shape[1]
    missing_cells = int(df.isnull().sum().sum())
    missing_rate = round(missing_cells / total_cells, 4) if total_cells > 0 else 0.0

    col_missing = {}
    for col in df.columns:
        n_missing = int(df[col].isnull().sum())
        col_missing[col] = {
            'missing_count': n_missing,
            'missing_rate': round(n_missing / len(df), 4) if len(df) > 0 else 0.0
        }

    complete_rows = int((~df.isnull().any(axis=1)).sum())
    row_completeness_rate = round(complete_rows / len(df), 4) if len(df) > 0 else 1.0

    score = round(1 - missing_rate, 4)

    return {
        'score': score,
        'total_cells': total_cells,
        'missing_cells': missing_cells,
        'missing_rate': missing_rate,
        'row_completeness_rate': row_completeness_rate,
        'complete_rows': complete_rows,
        'total_rows': len(df),
        'column_details': col_missing
    }


# ── 准确性 ─────────────────────────────────────────────

def _assess_accuracy(df, expected_types=None, expected_ranges=None):
    checks_passed = 0
    checks_total = 0
    column_details = {}

    for col in df.columns:
        col_result = {'checks': []}

        # 类型检查
        if expected_types and col in expected_types:
            checks_total += 1
            expected = expected_types[col]
            actual_type = 'numeric' if pd.api.types.is_numeric_dtype(df[col]) else 'non-numeric'
            passed = actual_type == expected
            col_result['checks'].append({
                'check': 'type',
                'expected': expected,
                'actual': actual_type,
                'passed': passed
            })
            if passed:
                checks_passed += 1

        # 范围检查
        if expected_ranges and col in expected_ranges:
            if pd.api.types.is_numeric_dtype(df[col]):
                checks_total += 1
                r = expected_ranges[col]
                min_val, max_val = r.get('min'), r.get('max')
                valid = df[col].dropna()
                in_range = True
                if min_val is not None:
                    in_range = in_range and (valid >= min_val).all()
                if max_val is not None:
                    in_range = in_range and (valid <= max_val).all()
                violations = int((~in_range).sum()) if isinstance(in_range, pd.Series) else 0
                col_result['checks'].append({
                    'check': 'range',
                    'min': min_val,
                    'max': max_val,
                    'violations': violations,
                    'passed': violations == 0
                })
                if violations == 0:
                    checks_passed += 1

        # 数值列异常值检测（3-sigma）
        if pd.api.types.is_numeric_dtype(df[col]):
            valid = df[col].dropna()
            if len(valid) >= 3:
                checks_total += 1
                mean_val = valid.mean()
                std_val = valid.std()
                if std_val > 0:
                    outliers = int(((valid - mean_val).abs() > 3 * std_val).sum())
                else:
                    outliers = 0
                col_result['outlier_count'] = outliers
                col_result['outlier_ratio'] = round(outliers / len(valid), 4) if len(valid) > 0 else 0.0
                passed = outliers == 0
                col_result['checks'].append({
                    'check': 'outlier_3sigma',
                    'outliers': outliers,
                    'passed': passed
                })
                if passed:
                    checks_passed += 1

        column_details[col] = col_result

    score = round(checks_passed / checks_total, 4) if checks_total > 0 else 1.0

    return {
        'score': score,
        'checks_passed': checks_passed,
        'checks_total': checks_total,
        'column_details': column_details
    }


# ── 一致性 ─────────────────────────────────────────────

def _assess_consistency(df, rules=None):
    checks_passed = 0
    checks_total = 0
    rule_results = []

    # 重复行检查
    checks_total += 1
    duplicate_rows = int(df.duplicated().sum())
    duplicate_row_ratio = round(duplicate_rows / len(df), 4) if len(df) > 0 else 0.0
    row_passed = duplicate_rows == 0
    if row_passed:
        checks_passed += 1
    rule_results.append({
        'rule': '_duplicate_rows',
        'duplicate_rows': duplicate_rows,
        'duplicate_row_ratio': duplicate_row_ratio,
        'passed': row_passed
    })

    # 格式一致性检查（同列内值的类型是否统一）
    format_issues = 0
    for col in df.columns:
        if df[col].dtype == object:
            non_null = df[col].dropna()
            if len(non_null) > 0:
                types = non_null.apply(type).unique()
                if len(types) > 1:
                    format_issues += 1
    checks_total += len(df.columns)
    format_passed_count = len(df.columns) - format_issues
    checks_passed += format_passed_count

    # 自定义逻辑规则检查
    if rules:
        import re
        for rule_str in rules:
            checks_total += 1
            passed, message = _evaluate_rule(df, rule_str)
            if passed:
                checks_passed += 1
            rule_results.append({
                'rule': rule_str,
                'passed': passed,
                'message': message
            })

    score = round(checks_passed / checks_total, 4) if checks_total > 0 else 1.0

    return {
        'score': score,
        'checks_passed': checks_passed,
        'checks_total': checks_total,
        'format_issues': format_issues,
        'rules': rule_results
    }


def _evaluate_rule(df, rule_str):
    import re
    pattern = r'^(.+?)\s*(==|!=|<=|>=|<|>)\s*(.+)$'
    match = re.match(pattern, rule_str.strip())
    if not match:
        return False, f"无法解析规则: {rule_str}"

    left_expr, operator, right_expr = match.group(1).strip(), match.group(2), match.group(3).strip()

    try:
        left = _resolve_operand(df, left_expr)
        right = _resolve_operand(df, right_expr)
    except ValueError as e:
        return False, str(e)

    ops = {
        '==': lambda a, b: a == b,
        '!=': lambda a, b: a != b,
        '<':  lambda a, b: a < b,
        '<=': lambda a, b: a <= b,
        '>':  lambda a, b: a > b,
        '>=': lambda a, b: a >= b,
    }

    result = ops[operator](left, right)
    if isinstance(result, pd.Series):
        violations = int((~result).sum())
        return violations == 0, f"违规行数: {violations}/{len(df)}"
    return True, "非 Series 比较"


def _resolve_operand(df, expr):
    if expr in df.columns:
        return df[expr]
    try:
        return float(expr)
    except ValueError:
        pass
    if (expr.startswith("'") and expr.endswith("'")) or (expr.startswith('"') and expr.endswith('"')):
        return expr[1:-1]
    raise ValueError(f"无法识别的操作数: {expr}")


# ── 冗余性 ─────────────────────────────────────────────

def _assess_redundancy(df):
    checks = {}

    # 完全重复行
    duplicate_rows = int(df.duplicated().sum())
    checks['duplicate_rows'] = {
        'count': duplicate_rows,
        'ratio': round(duplicate_rows / len(df), 4) if len(df) > 0 else 0.0
    }

    # 完全重复列
    dup_cols = set()
    cols = list(df.columns)
    for i in range(len(cols)):
        for j in range(i + 1, len(cols)):
            if df[cols[i]].equals(df[cols[j]]):
                dup_cols.add(cols[j])
    duplicate_columns = len(dup_cols)
    checks['duplicate_columns'] = {
        'count': duplicate_columns,
        'ratio': round(duplicate_columns / len(cols), 4) if len(cols) > 0 else 0.0,
        'columns': list(dup_cols)
    }

    # 高相关列（皮尔逊相关系数 > 0.95）
    numeric_cols = df.select_dtypes(include='number').columns.tolist()
    high_corr_pairs = []
    if len(numeric_cols) >= 2:
        corr_matrix = df[numeric_cols].corr()
        for i in range(len(numeric_cols)):
            for j in range(i + 1, len(numeric_cols)):
                corr_val = corr_matrix.iloc[i, j]
                if abs(corr_val) > 0.95:
                    high_corr_pairs.append({
                        'col_a': numeric_cols[i],
                        'col_b': numeric_cols[j],
                        'correlation': round(float(corr_val), 4)
                    })
    checks['high_correlation_pairs'] = {
        'count': len(high_corr_pairs),
        'pairs': high_corr_pairs
    }

    # 列级信息熵（离散程度的度量，值越低越冗余）
    entropy_details = {}
    for col in df.columns:
        non_null = df[col].dropna()
        if len(non_null) > 0 and df[col].dtype == object:
            value_counts = non_null.value_counts(normalize=True)
            entropy = float(-(value_counts * np.log2(value_counts)).sum())
        elif pd.api.types.is_numeric_dtype(df[col]) and len(non_null) > 1:
            entropy = float(np.log2(non_null.std())) if non_null.std() > 0 else 0.0
        else:
            entropy = 0.0
        entropy_details[col] = round(entropy, 4)
    checks['information_entropy'] = entropy_details

    # 综合冗余度：重复行、重复列、高相关对的比例取平均
    r1 = checks['duplicate_rows']['ratio']
    r2 = checks['duplicate_columns']['ratio']
    r3 = len(high_corr_pairs) / (len(numeric_cols) * (len(numeric_cols) - 1) / 2) if len(numeric_cols) >= 2 else 0.0
    redundancy_ratio = round((r1 + r2 + r3) / 3, 4)
    score = round(1 - redundancy_ratio, 4)

    return {
        'score': score,
        'redundancy_ratio': redundancy_ratio,
        'details': checks
    }
