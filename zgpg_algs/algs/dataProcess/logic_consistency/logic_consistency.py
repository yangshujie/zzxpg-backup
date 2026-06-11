import pandas as pd
import re


def algsMain(*args, **kwargs):
    """
    逻辑一致性检验：根据指定规则检查数据表内字段间的逻辑关系。
    rules 中支持的运算符: ==, !=, <, <=, >, >=
    规则示例: "start_date <= end_date", "age >= 0"

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list, 要检查的数据源名称列表，不可为空。
            rules: list[str], 逻辑规则列表，如 ["col_a <= col_b", "col_c >= 0"]。
    :return: (bool, result) 成功返回检查结果，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        rules = params.get('rules', [])

        if not sources:
            raise ValueError("必须提供 sources 参数，不可为空")
        missing = [s for s in sources if s not in data]
        if missing:
            raise ValueError(f"数据源 {missing} 不存在")
        if not rules:
            raise ValueError("必须提供 rules 参数")

        analysis = {}

        for name in sources:
            df = data[name]
            rule_results = []

            for rule_str in rules:
                passed, total, message = _evaluate_rule(df, rule_str)
                rule_results.append({
                    'rule': rule_str,
                    'passed': passed,
                    'total_rows': total,
                    'message': message
                })

            all_passed = all(r['passed'] for r in rule_results)
            analysis[name] = {
                'consistent': all_passed,
                'rules': rule_results
            }

        ret = {
            'data': data,
            'analysis': {
                'sources': sources,
                'rules': rules,
                'details': analysis
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)


def _evaluate_rule(df, rule_str):
    """
    解析并执行单条规则。
    规则格式: "left_operand operator right_operand"
    """
    pattern = r'^(.+?)\s*(==|!=|<=|>=|<|>)\s*(.+)$'
    match = re.match(pattern, rule_str.strip())
    if not match:
        return False, 0, f"无法解析规则: {rule_str}"

    left_expr, operator, right_expr = match.group(1).strip(), match.group(2), match.group(3).strip()

    try:
        left = _resolve_operand(df, left_expr)
        right = _resolve_operand(df, right_expr)
    except ValueError as e:
        return False, 0, str(e)

    ops = {
        '==': lambda a, b: a == b,
        '!=': lambda a, b: a != b,
        '<':  lambda a, b: a < b,
        '<=': lambda a, b: a <= b,
        '>':  lambda a, b: a > b,
        '>=': lambda a, b: a >= b,
    }

    result = ops[operator](left, right)
    violations = int((~result).sum()) if isinstance(result, pd.Series) else 0
    total = len(df) if isinstance(result, pd.Series) else 0
    passed = violations == 0

    return passed, total, f"违规行数: {violations}/{total}" if isinstance(result, pd.Series) else "非 Series 比较"


def _resolve_operand(df, expr):
    """将操作数解析为列数据或常量。"""
    # 尝试作为列名
    if expr in df.columns:
        return df[expr]
    # 尝试作为数值常量
    try:
        return float(expr)
    except ValueError:
        pass
    # 尝试作为字符串常量（单引号或双引号包裹）
    if (expr.startswith("'") and expr.endswith("'")) or (expr.startswith('"') and expr.endswith('"')):
        return expr[1:-1]
    raise ValueError(f"无法识别的操作数: {expr}，不是列名或合法常量")
