import json
import random
import numpy as np
import networkx as nx
from collections import Counter, defaultdict
import math
import sys
import time
import hdbscan
from sklearn.metrics.pairwise import cosine_similarity

# ==================== 配置参数区 ====================
CONFIG = {
    'DATA_FILE': 'ucidata/通信对抗_太空方向_500条任务数据_v3.json',
    'OUTPUT_FILE': 'ucidata/skeleton_result_HDBSCAN_050805.json',
    # 随机游走参数
    'BASE_WALK_LENGTH': 15,
    'BASE_NUM_WALKS': 100,
    'MIN_WALK_LENGTH': 8,
    'MAX_WALK_LENGTH': 25,
    'ADAPTIVE_WALK': True,

    # Node2Vec 参数
    'NODE2VEC_DIM': 64,
    'NODE2VEC_P': 0.5,
    'NODE2VEC_Q': 2.0,
    'NODE2VEC_MAX_NODES': 2000,

    # 自适应阈值参数
    'AUTO_ADJUST_SUPPORT': True,
    'REF_PATH_COUNT': 5000,
    'MIN_SUPPORT': 0.05,
    'MAX_SUPPORT': 0.30,
    'MIN_PATH_SUPPORT': 0.01,
    'MAX_PATH_SUPPORT': 0.10,

    # 路径统计参数
    'MIN_PATH_LENGTH': 3,
    'MAX_PATH_LENGTH': 12,

    # 链路聚类参数
    'USE_SEMANTIC_CLUSTERING': True,
    'HDBSCAN_MIN_CLUSTER_SIZE': 2,
    'HDBSCAN_MIN_SAMPLES': 2,

    'PRINT_DELAY': 0,
    'VERBOSE': True,
    'SAVE_JSON': True,
}

# 术语映射表
TERM_MAP = {
    'J/S': '干信比',
    'MTBF': '平均故障间隔时间',
    'MTTR': '平均修复时间',
    'RMSE': '均方根误差',
    'CEP': '圆概率误差',
    'EIRP': '等效全向辐射功率',
    'BIT': '机内测试',
    'BER': '误码率',
    'SNR': '信噪比',
    'CFAR': '恒虚警率检测',
    'AHP': '层次分析法',
}

if CONFIG['PRINT_DELAY'] > 0:
    _print = print


    def print(*args, **kwargs):
        _print(*args, **kwargs)
        sys.stdout.flush()
        time.sleep(CONFIG['PRINT_DELAY'])

random.seed(42)
np.random.seed(42)

# 导入
try:
    import os

    os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'
    from sentence_transformers import SentenceTransformer

    SEMANTIC_AVAILABLE = True
    print("Sentence Transformers 已加载")
except ImportError:
    SEMANTIC_AVAILABLE = False
    print("Sentence Transformers 未安装")


# ==================== 数据加载 ====================
def load_tasks_from_json(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    tasks = data.get('tasks', [])
    metadata = data.get('metadata', {})
    print(f"加载 {len(tasks)} 个任务")
    return tasks, metadata


def translate_terms(text):
    for abbr, full in TERM_MAP.items():
        if abbr in text:
            text = text.replace(abbr, full)
    return text


def build_knowledge_graph_from_tasks(tasks):
    G = nx.MultiDiGraph()
    for task in tasks:
        for node in task['nodes']:
            nid = node['id']
            if not G.has_node(nid):
                G.add_node(nid, type=node['type'], name=node.get('name', ''),
                           level=node.get('level', ''), domain=node.get('domain', '通信对抗'),
                           unit=node.get('unit', ''))
        for edge in task['edges']:
            G.add_edge(edge['source'], edge['target'], relation=edge['relation'])
    if CONFIG['VERBOSE']:
        print(f"知识图谱构建完成: 节点数 {G.number_of_nodes()}, 边数 {G.number_of_edges()}")
    return G


# ==================== Node2Vec 训练 ====================
def train_node2vec(G):
    from gensim.models import Word2Vec

    print("\n训练 Node2Vec 模型...")

    task_nodes = [n for n in G.nodes if G.nodes[n]['type'] == '任务']
    sample_nodes = task_nodes[:min(CONFIG['NODE2VEC_MAX_NODES'], len(task_nodes))]
    print(f"采样 {len(sample_nodes)} 个任务节点进行游走")

    walks = []
    for start_node in sample_nodes:
        for _ in range(CONFIG['BASE_NUM_WALKS']):
            walk = [start_node]
            cur = start_node
            for _ in range(CONFIG['BASE_WALK_LENGTH']):
                nbrs = list(G.successors(cur))
                if not nbrs:
                    break
                if len(walk) == 1:
                    nxt = random.choice(nbrs)
                else:
                    prev = walk[-2]
                    probs = []
                    for nb in nbrs:
                        if nb == prev:
                            probs.append(1.0 / CONFIG['NODE2VEC_P'])
                        elif G.has_edge(prev, nb):
                            probs.append(1.0)
                        else:
                            probs.append(1.0 / CONFIG['NODE2VEC_Q'])
                    probs = np.array(probs) / sum(probs)
                    nxt = np.random.choice(nbrs, p=probs)
                walk.append(nxt)
                cur = nxt
            walks.append(walk)

    walks_str = [[str(node) for node in walk] for walk in walks]

    model = Word2Vec(
        walks_str,
        vector_size=CONFIG['NODE2VEC_DIM'],
        window=5,
        min_count=1,
        sg=1,
        workers=4,
        epochs=10
    )

    node2vec_vectors = {}
    zero_count = 0
    for node in G.nodes:
        try:
            node2vec_vectors[node] = model.wv[str(node)]
        except KeyError:
            node2vec_vectors[node] = np.zeros(CONFIG['NODE2VEC_DIM'])
            zero_count += 1

    print(f"Node2Vec 训练完成，{zero_count} 个零向量")
    return node2vec_vectors


# ==================== 自适应随机游走 ====================
def analyze_graph_structure(G):
    task_nodes = [n for n in G.nodes if G.nodes[n]['type'] == '任务']
    if not task_nodes:
        return None
    patterns = []
    for tn in task_nodes[:10]:
        visited = set([tn])
        queue = [(tn, [tn])]
        while queue:
            node, path = queue.pop(0)
            if len(path) > 8:
                continue
            for nb in G.successors(node):
                if nb not in visited:
                    new_path = path + [nb]
                    type_seq = [G.nodes[n]['type'] for n in new_path]
                    patterns.append(type_seq)
                    visited.add(nb)
                    queue.append((nb, new_path))
    if patterns:
        pattern_counter = Counter([tuple(p) for p in patterns])
        most_common = pattern_counter.most_common(1)[0][0]
        return list(most_common)
    return None


_WALK_LENGTH_CACHE = {}


def calculate_adaptive_walk_length(G, domain):
    if domain in _WALK_LENGTH_CACHE:
        return _WALK_LENGTH_CACHE[domain]
    domain_nodes = [n for n in G.nodes if G.nodes[n].get('domain') == domain]
    if not domain_nodes:
        return CONFIG['BASE_WALK_LENGTH']
    subgraph = G.subgraph(domain_nodes)
    density = len(subgraph.edges()) / max(len(domain_nodes), 1)
    walk_length = int(CONFIG['BASE_WALK_LENGTH'] * (1 + density))
    walk_length = max(CONFIG['MIN_WALK_LENGTH'], min(CONFIG['MAX_WALK_LENGTH'], walk_length))
    _WALK_LENGTH_CACHE[domain] = walk_length
    return walk_length


def biased_random_walk_adaptive(G, start_node, walk_length, p, q, detected_pattern=None):
    walk = [start_node]
    cur = start_node
    if detected_pattern and len(detected_pattern) > 1:
        pattern_idx = 1
        current_type = G.nodes[cur]['type']
        try:
            pos = detected_pattern.index(current_type)
            pattern_idx = pos + 1
        except ValueError:
            pass
        for step in range(walk_length - 1):
            nbrs = list(G.successors(cur))
            if not nbrs:
                break
            if pattern_idx < len(detected_pattern):
                target_type = detected_pattern[pattern_idx]
                matching_nbrs = [nb for nb in nbrs if G.nodes[nb]['type'] == target_type]
                if matching_nbrs:
                    nxt = random.choice(matching_nbrs)
                    walk.append(nxt)
                    cur = nxt
                    pattern_idx += 1
                    continue
            if len(walk) == 1:
                probs = [1.0] * len(nbrs)
            else:
                prev = walk[-2]
                probs = []
                for nb in nbrs:
                    if nb == prev:
                        probs.append(1.0 / p)
                    elif G.has_edge(prev, nb):
                        probs.append(1.0)
                    else:
                        probs.append(1.0 / q)
            probs = np.array(probs) / sum(probs)
            nxt = np.random.choice(nbrs, p=probs)
            walk.append(nxt)
            cur = nxt
    else:
        for _ in range(walk_length - 1):
            nbrs = list(G.successors(cur))
            if not nbrs:
                break
            if len(walk) == 1:
                probs = [1.0] * len(nbrs)
            else:
                prev = walk[-2]
                probs = []
                for nb in nbrs:
                    if nb == prev:
                        probs.append(1.0 / p)
                    elif G.has_edge(prev, nb):
                        probs.append(1.0)
                    else:
                        probs.append(1.0 / q)
            probs = np.array(probs) / sum(probs)
            nxt = np.random.choice(nbrs, p=probs)
            walk.append(nxt)
            cur = nxt
    return walk


def perform_random_walks_adaptive(G):
    task_nodes = [n for n in G.nodes if G.nodes[n]['type'] == '任务']
    if CONFIG['VERBOSE']:
        print(f"\n找到 {len(task_nodes)} 个任务节点")
    detected_pattern = None
    if CONFIG['ADAPTIVE_WALK']:
        detected_pattern = analyze_graph_structure(G)
        if CONFIG['VERBOSE'] and detected_pattern:
            print(f"探测到的典型路径模式: {' → '.join(detected_pattern)}")
    domains = set([G.nodes[tn].get('domain', '通信对抗') for tn in task_nodes])
    domain_walk_lengths = {d: calculate_adaptive_walk_length(G, d) for d in domains}
    all_walks = []
    for tn in task_nodes:
        domain = G.nodes[tn].get('domain', '通信对抗')
        walk_len = domain_walk_lengths[domain]
        for _ in range(CONFIG['BASE_NUM_WALKS']):
            walk = biased_random_walk_adaptive(G, tn, walk_len, 0.5, 2.0, detected_pattern)
            if len(walk) >= 3:
                all_walks.append(walk)
    if CONFIG['VERBOSE']:
        print(f"共生成 {len(all_walks)} 条随机游走路径")
    return all_walks


def get_adaptive_threshold(total_paths, base_threshold):
    if not CONFIG['AUTO_ADJUST_SUPPORT']:
        return base_threshold
    ref_paths = CONFIG['REF_PATH_COUNT']
    adjusted = base_threshold * math.sqrt(ref_paths / max(total_paths, 100))
    min_val = CONFIG['MIN_PATH_SUPPORT'] if base_threshold < 0.1 else CONFIG['MIN_SUPPORT']
    max_val = CONFIG['MAX_PATH_SUPPORT'] if base_threshold < 0.1 else CONFIG['MAX_SUPPORT']
    return max(min_val, min(max_val, adjusted))


def mine_frequent_patterns_adaptive(walks, G):
    domain_walks = defaultdict(list)
    domain_patterns = defaultdict(lambda: defaultdict(Counter))
    for walk in walks:
        first = walk[0]
        if first not in G.nodes:
            continue
        domain = G.nodes[first].get('domain', '通信对抗')
        domain_walks[domain].append(walk)
    for domain, dom_walks in domain_walks.items():
        total = len(dom_walks)
        if total == 0:
            continue
        for walk in dom_walks:
            for length in range(CONFIG['MIN_PATH_LENGTH'], min(len(walk), CONFIG['MAX_PATH_LENGTH'] + 1)):
                for i in range(len(walk) - length + 1):
                    segment = walk[i:i + length]
                    seg_types = [G.nodes[n]['type'] for n in segment]
                    if seg_types[0] != '任务':
                        continue
                    names = [translate_terms(G.nodes[n].get('name', n)) for n in segment]
                    path_str = ' → '.join(names)
                    domain_patterns[domain][length][path_str] += 1
    return domain_patterns, domain_walks


# ==================== 构建树结构 ====================
def count_tree_nodes(node):
    if not node:
        return 0
    count = 1
    for child in node.get('children', []):
        count += count_tree_nodes(child)
    return count


def build_standard_tree_from_paths(paths_list, total_count):
    """从路径列表构建标准格式的树结构 - 自动去重合并"""
    if not paths_list:
        return None

    tree = {}
    for item in paths_list:
        path = item['path']
        count = item['count']
        cur = tree
        for i, node_name in enumerate(path):
            if node_name not in cur:
                cur[node_name] = {}
            if i == len(path) - 1:
                cur[node_name]['_count'] = cur[node_name].get('_count', 0) + count
            else:
                cur = cur[node_name]

    def convert_to_node(node_name, node_data):
        # 检查去重
        if hasattr(convert_to_node, '_cache') and node_name in convert_to_node._cache:
            cached = convert_to_node._cache[node_name]
            # 合并 count
            if '_count' in node_data and '_count' in cached:
                cached['support'] = round(cached.get('support', 0) + node_data['_count'] / max(total_count, 1), 4)
            return cached

        node = {
            'id': f"node_{hash(node_name) & 0xffffffff}",
            'label': node_name,
            'children': []
        }
        if '_count' in node_data:
            node['support'] = round(node_data['_count'] / max(total_count, 1), 4)

        # 用于去重的子节点名称集合
        child_names = set()

        for child_name, child_data in node_data.items():
            if child_name.startswith('_'):
                continue
            child_node = convert_to_node(child_name, child_data)
            # 按名称去重：如果已经存在相同名称的子节点，跳过
            if child_node['label'] not in child_names:
                child_names.add(child_node['label'])
                node['children'].append(child_node)

        # 缓存
        if not hasattr(convert_to_node, '_cache'):
            convert_to_node._cache = {}
        convert_to_node._cache[node_name] = node

        return node

    # 重置缓存
    convert_to_node._cache = {}

    roots = []
    for name, data in tree.items():
        if not name.startswith('_'):
            roots.append(convert_to_node(name, data))

    if len(roots) == 1:
        return roots[0]
    else:
        return {
            'id': 'root',
            'label': '评估指标体系',
            'children': roots
        }


# ==================== 按任务拆分骨架 ====================
def encode_path_dual(path_nodes, node2vec_vectors, sentence_model):
    """双通道编码：Sentence Transformer + Node2Vec"""
    # 语义通道
    path_text = " → ".join(path_nodes)
    semantic_vec = sentence_model.encode(path_text)

    # 结构通道（Node2Vec）
    node_vecs = []
    for node in path_nodes:
        if node in node2vec_vectors:
            node_vecs.append(node2vec_vectors[node])
        else:
            node_vecs.append(np.zeros(CONFIG['NODE2VEC_DIM']))
    structure_vec = np.mean(node_vecs, axis=0) if node_vecs else np.zeros(CONFIG['NODE2VEC_DIM'])

    # 拼接（512 + 64 = 576 维）
    return np.concatenate([semantic_vec, structure_vec])


def run_tree_fusion_split(domain_patterns, domain_walks, original_tasks, node2vec_vectors):
    """每个任务生成一个骨架，任务内部路径用 HDBSCAN 双通道聚类"""
    all_skeletons = []

    # 按任务名称分组收集路径
    task_paths_map = defaultdict(list)

    for domain, patterns in domain_patterns.items():
        total = len(domain_walks.get(domain, []))
        if total == 0:
            continue

        for length, counter in patterns.items():
            for path_str, count in counter.items():
                parts = path_str.split(' → ')
                if len(parts) >= 1:
                    task_name = parts[0]
                    task_paths_map[task_name].append({
                        'path': parts,
                        'count': count,
                        'support': count / total if total > 0 else 0
                    })

    print(f"\n共发现 {len(task_paths_map)} 个任务")

    # 加载 Sentence Transformer 模型（用于语义聚类）
    sentence_model = None
    if CONFIG['USE_SEMANTIC_CLUSTERING'] and SEMANTIC_AVAILABLE:
        sentence_model = SentenceTransformer('./m3e-small', local_files_only=True)
        print("Sentence Transformer 模型已加载，将对每个任务内部的路径进行 HDBSCAN 双通道聚类")

    skeleton_id = 1

    for task_name, paths in task_paths_map.items():
        if not paths:
            continue

        print(f"\n处理任务: {task_name}，共 {len(paths)} 条原始路径")

        # 按支持度排序
        paths = sorted(paths, key=lambda x: -x['count'])

        # ========== 任务内部路径双通道聚类 ==========
        if CONFIG['USE_SEMANTIC_CLUSTERING'] and sentence_model and len(paths) > 1:
            # 构建双通道向量
            vectors = []
            for item in paths:
                vec = encode_path_dual(item['path'], node2vec_vectors, sentence_model)
                vectors.append(vec)

            vectors = np.array(vectors)
            print(f"  双通道向量维度: {vectors.shape[1]} (语义512 + 结构{CONFIG['NODE2VEC_DIM']})")

            # HDBSCAN 聚类
            clusterer = hdbscan.HDBSCAN(
                min_cluster_size=CONFIG['HDBSCAN_MIN_CLUSTER_SIZE'],
                min_samples=CONFIG['HDBSCAN_MIN_SAMPLES'],
                metric='euclidean'
            )
            labels = clusterer.fit_predict(vectors)

            # 按聚类结果合并路径
            clusters = defaultdict(list)
            for idx, label in enumerate(labels):
                clusters[int(label)].append(paths[idx])

            # 合并后的路径
            merged_paths = []
            for cluster_id, cluster_paths in clusters.items():
                if cluster_id == -1:
                    # 噪声点，单独保留
                    for item in cluster_paths:
                        merged_paths.append(item)
                else:
                    # 取簇内出现次数最多的作为代表
                    best = max(cluster_paths, key=lambda x: x['count'])
                    best['cluster_size'] = len(cluster_paths)
                    merged_paths.append(best)

            print(f"  HDBSCAN 聚类后: {len(merged_paths)} 条路径（原 {len(paths)} 条）")
            paths = merged_paths

        # 取前10条路径构建树
        top_paths = paths[:10]
        total_count = sum(p['count'] for p in paths)

        tree_data = build_standard_tree_from_paths(top_paths, total_count)

        # 从原始任务中查找补充信息
        domain = ""
        equipment_level = []
        purposes = []

        for task in original_tasks:
            for node in task.get('nodes', []):
                if node.get('type') == '任务' and node.get('name') == task_name:
                    level = task.get('level', '')
                    # level_map = {'装备级': '设备级', '系统级': '系统级', '体系级': '体系级'}
                    equipment_level = [level] if level else []
                    domain = task.get('domain', '')
                    for n in task.get('nodes', []):
                        if n.get('type') == '评估目的':
                            purposes.append(n.get('name', ''))
                    break

        skeleton = {
            "skeletonId": skeleton_id,
            "skeletonName": task_name,
            "field": domain,
            "equipmentLevel": equipment_level,
            "purpose": purposes,
            "treeData": tree_data if tree_data else {},
            "createTime": "",
            "updateTime": "",
            "remark": "",
            "evaluationCriteria": [],
            "algoConfig": [],
            "operators": []
        }

        all_skeletons.append(skeleton)
        skeleton_id += 1

        print(f"  生成骨架 ID: {skeleton_id - 1}")
        print(f"  树节点数: {count_tree_nodes(tree_data)}")

    return all_skeletons


# def generate_html_output(skeletons, original_tasks):
#     """生成指定格式的输出"""
#     result = {"skeletonList": []}
#
#     # 构建原始任务信息的快速查找字典
#     task_info_map = {}
#     for task in original_tasks:
#         task_name = None
#         for node in task.get('nodes', []):
#             if node.get('type') == '任务':
#                 task_name = node.get('name', '')
#                 break
#         if task_name:
#             task_info_map[task_name] = task
#
#     for sk in skeletons:
#         skeleton_name = sk.get("skeletonName", "")
#
#         # 查找对应的原始任务
#         original_task = task_info_map.get(skeleton_name, {})
#         original_nodes = original_task.get('nodes', [])
#
#         # 提取 evaluationCriteria（type='评价准则' 的节点）
#         evaluation_criteria = []
#         for node in original_nodes:
#             if node.get('type') == '评价准则':
#                 criteria = {}
#                 if node.get('purpose'):
#                     criteria['purpose'] = node.get('purpose')
#                 if node.get('dimensionName'):
#                     criteria['dimensionName'] = node.get('dimensionName')
#                 if node.get('qualified'):
#                     criteria['qualified'] = node.get('qualified')
#                 if node.get('basicallyQualified'):
#                     criteria['basicallyQualified'] = node.get('basicallyQualified')
#                 if node.get('noQualified'):
#                     criteria['noQualified'] = node.get('noQualified')
#                 if criteria:
#                     evaluation_criteria.append(criteria)
#
#         # 提取 algoConfig（type='汇聚算法' 或 '权重算法' 的节点）
#         algo_config = []
#         for node in original_nodes:
#             if node.get('type') in ['汇聚算法', '权重算法']:
#                 config = {}
#                 if node.get('algoName'):
#                     config['algoName'] = node.get('algoName')
#                 if node.get('convergenceStrategy'):
#                     config['convergenceStrategy'] = node.get('convergenceStrategy')
#                 if node.get('missHandling'):
#                     config['missHandling'] = node.get('missHandling')
#                 if node.get('version'):
#                     config['version'] = node.get('version')
#                 if config:
#                     algo_config.append(config)
#
#         # 提取 operators（type='计算算子' 或 '归一化算子' 的节点）
#         operators = []
#         for node in original_nodes:
#             if node.get('type') in ['计算算子', '归一化算子']:
#                 operator = {}
#                 if node.get('algoId'):
#                     operator['algoId'] = node.get('algoId')
#                 if node.get('algoLabel'):
#                     operator['algoLabel'] = node.get('algoLabel')
#                 if node.get('applicableType'):
#                     operator['applicableType'] = node.get('applicableType')
#                 if node.get('conditions'):
#                     operator['conditions'] = node.get('conditions')
#                 if operator:
#                     operators.append(operator)
#
#         # 获取 treeData（从原始任务的指标树构建）
#         tree_data = build_tree_data_from_task(original_task)
#
#         # 构建输出项
#         output_item = {
#             "skeletonName": skeleton_name,
#             "field": sk.get("field", ""),
#             "equipmentLevel": sk.get("equipmentLevel", []),
#             "purpose": sk.get("purpose", []),
#             "remark": "",
#             "evaluationCriteria": evaluation_criteria,
#             "algoConfig": algo_config,
#             "treeData": tree_data,
#             "operators": operators
#         }
#
#         result["skeletonList"].append(output_item)
#
#     return result
def generate_html_output(skeletons, original_tasks):
    """生成指定格式的输出 - 带去重"""
    result = []

    task_info_map = {}
    for task in original_tasks:
        task_name = None
        for node in task.get('nodes', []):
            if node.get('type') == '任务':
                task_name = node.get('name', '')
                break
        if task_name:
            task_info_map[task_name] = task

    for sk in skeletons:
        skeleton_name = sk.get("skeletonName", "")
        original_task = task_info_map.get(skeleton_name, {})
        original_nodes = original_task.get('nodes', [])

        # purpose 去重
        purposes = list(set([
            n.get('name', '') for n in original_nodes
            if n.get('type') == '评估目的' and n.get('name')
        ]))

        # evaluationCriteria 去重（按 dimensionName）
        criteria_list = []
        seen_criteria = set()
        for node in original_nodes:
            if node.get('type') == '评价准则':
                dim_name = node.get('dimensionName', '')
                if dim_name and dim_name not in seen_criteria:
                    seen_criteria.add(dim_name)
                    criteria = {}
                    if node.get('purpose'):
                        criteria['purpose'] = node.get('purpose')
                    if node.get('dimensionName'):
                        criteria['dimensionName'] = node.get('dimensionName')
                    if node.get('qualified'):
                        criteria['qualified'] = node.get('qualified')
                    if node.get('basicallyQualified'):
                        criteria['basicallyQualified'] = node.get('basicallyQualified')
                    if node.get('noQualified'):
                        criteria['noQualified'] = node.get('noQualified')
                    if criteria:
                        criteria_list.append(criteria)

        # algoConfig 去重（按 algoName）
        config_list = []
        seen_config = set()
        for node in original_nodes:
            if node.get('type') in ['汇聚算法', '权重算法']:
                algo_name = node.get('algoName', '')
                if algo_name and algo_name not in seen_config:
                    seen_config.add(algo_name)
                    config = {}
                    if node.get('algoName'):
                        config['algoName'] = node.get('algoName')
                    if node.get('convergenceStrategy'):
                        config['convergenceStrategy'] = node.get('convergenceStrategy')
                    if node.get('missHandling'):
                        config['missHandling'] = node.get('missHandling')
                    if node.get('version'):
                        config['version'] = node.get('version')
                    if config:
                        config_list.append(config)

        # operators 去重（按 algoId）
        operator_list = []
        seen_operator = set()
        for node in original_nodes:
            if node.get('type') in ['计算算子', '归一化算子']:
                algo_id = node.get('algoId', '')
                if algo_id and algo_id not in seen_operator:
                    seen_operator.add(algo_id)
                    operator = {}
                    if node.get('algoId'):
                        operator['algoId'] = node.get('algoId')
                    if node.get('algoLabel'):
                        operator['algoLabel'] = node.get('algoLabel')
                    if node.get('applicableType'):
                        operator['applicableType'] = node.get('applicableType')
                    if node.get('conditions'):
                        operator['conditions'] = node.get('conditions')
                    if operator:
                        operator_list.append(operator)

        tree_data = build_tree_data_from_task(original_task)

        output_item = {
            "skeletonName": skeleton_name,
            "field": sk.get("field", ""),
            "equipmentLevel": sk.get("equipmentLevel", []),
            "purpose": purposes,
            "remark": "",
            "evaluationCriteria": criteria_list,
            "algoConfig": config_list,
            "treeData": tree_data,
            "operators": operator_list
        }

        result.append(output_item)

    return result



def build_tree_data_from_task(task):
    """从原始任务构建树形数据 - 包含所有字段，没有就留空"""
    nodes = task.get('nodes', [])
    edges = task.get('edges', [])

    # 构建所有节点的映射
    all_nodes_map = {}
    for node in nodes:
        node_id = node.get('id', '')
        if node_id:
            all_nodes_map[node_id] = node

    # 构建指标节点的完整信息
    indicator_map = {}
    for node_id, node in all_nodes_map.items():
        if node.get('type') == '指标':
            indicator_map[node_id] = {
                'id': node_id,
                'label': node.get('name', ''),
                'indicatorType': node.get('indicatorType', ''),
                'workMode': node.get('workMode'),
                'unit': node.get('unit', ''),
                'type': node.get('type', ''),
                'valueMin': node.get('valueMin'),
                'valueMax': node.get('valueMax'),
                'description': node.get('description', ''),
                'valueCategory': node.get('valueCategory', ''),
                'computeRule': node.get('computeRule')
            }

    # 构建父子关系
    parent_child = defaultdict(list)
    for edge in edges:
        if edge.get('relation') == 'parentIndicator':
            parent_child[edge['source']].append(edge['target'])

    # 找根节点
    root_ids = []
    for node_id in indicator_map:
        is_child = False
        for p, children in parent_child.items():
            if node_id in children:
                is_child = True
                break
        if not is_child:
            root_ids.append(node_id)

    if not root_ids:
        return {}

    def build_node(node_id):
        node_data = indicator_map.get(node_id, {})

        result = {
            'id': node_id,
            'label': node_data.get('label', ''),
            'indicatorType': node_data.get('indicatorType', ''),
            'workMode': node_data.get('workMode'),
            'children': [],
            'unit': node_data.get('unit', ''),
            'type': node_data.get('type', ''),
            'valueMin': node_data.get('valueMin'),
            'valueMax': node_data.get('valueMax'),
            'description': node_data.get('description', ''),
            'valueCategory': node_data.get('valueCategory', ''),
            'computeRule': node_data.get('computeRule')
        }

        for child_id in parent_child.get(node_id, []):
            child_node = build_node(child_id)
            if child_node:
                result['children'].append(child_node)

        return result

    if len(root_ids) == 1:
        root = build_node(root_ids[0])
        if not root.get('indicatorType'):
            root['indicatorType'] = ''
        if root.get('workMode') is None:
            root['workMode'] = None
        return root
    else:
        return {
            'id': 'root',
            'label': '评估指标体系',
            'indicatorType': '',
            'workMode': None,
            'children': [build_node(rid) for rid in root_ids],
            'unit': '',
            'type': '',
            'valueMin': None,
            'valueMax': None,
            'description': '',
            'valueCategory': '',
            'computeRule': None
        }


# ==================== 主程序 ====================
def main():
    print("=" * 60)
    print("Step 1: 加载JSON任务数据")
    print("=" * 60)
    tasks, metadata = load_tasks_from_json(CONFIG['DATA_FILE'])

    print("\n" + "=" * 60)
    print("Step 2: 构建知识图谱")
    print("=" * 60)
    G = build_knowledge_graph_from_tasks(tasks)

    print("\n" + "=" * 60)
    print("Step 3: 训练 Node2Vec")
    print("=" * 60)
    node2vec_vectors = train_node2vec(G)

    print("\n" + "=" * 60)
    print("Step 4: 自适应随机游走采样")
    print("=" * 60)
    walks = perform_random_walks_adaptive(G)

    print("\n" + "=" * 60)
    print("Step 5: 频繁模式挖掘")
    print("=" * 60)
    domain_patterns, domain_walks = mine_frequent_patterns_adaptive(walks, G)

    print("\n" + "=" * 60)
    print("Step 6: 按任务拆分生成骨架")
    print("=" * 60)
    skeletons = run_tree_fusion_split(domain_patterns, domain_walks, tasks, node2vec_vectors)

    print("\n" + "=" * 60)
    print("Step 7: 输出最终结果")
    print("=" * 60)
    output = generate_html_output(skeletons, tasks)

    with open(CONFIG['OUTPUT_FILE'], 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    import requests
    api_url = "http://127.0.0.1:9401/skeletonBasic/receiveExcavateSkeleton"

    try:
        response = requests.post(
            api_url,
            json=output,
            headers={"Content-Type": "application/json"}
        )
        if response.status_code == 200:
            print("数据已成功发送到接口")
        else:
            print(f"发送失败: {response.status_code} - {response.text}")
    except Exception as e:
        print(f"请求异常: {e}")

    print(f"\n完成！共生成 {len(skeletons)} 个骨架")
    print(f"保存到: {CONFIG['OUTPUT_FILE']}")



if __name__ == "__main__":
    a = time.time()
    main()
    b = time.time()
    print(f"\n用时{b - a}")