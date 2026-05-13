# coding:utf-8
import numpy as np
from threading import Lock


# ======================== 模型单例 ========================

_VECTOR_MODEL = None
_VECTOR_MODEL_ERROR = None
_VECTOR_MODEL_LOCK = Lock()


def _load_vector_model():
    """Lazy-load sentence-transformer 模型（线程安全）"""
    global _VECTOR_MODEL, _VECTOR_MODEL_ERROR
    if _VECTOR_MODEL is not None:
        return _VECTOR_MODEL
    if _VECTOR_MODEL_ERROR is not None:
        raise RuntimeError(_VECTOR_MODEL_ERROR)
    with _VECTOR_MODEL_LOCK:
        if _VECTOR_MODEL is not None:
            return _VECTOR_MODEL
        if _VECTOR_MODEL_ERROR is not None:
            raise RuntimeError(_VECTOR_MODEL_ERROR)
        try:
            from sentence_transformers import SentenceTransformer
            _VECTOR_MODEL = SentenceTransformer("./m3e-small", local_files_only=True)
        except Exception as exc:
            _VECTOR_MODEL_ERROR = "模型加载失败: {0}".format(str(exc))
            raise RuntimeError(_VECTOR_MODEL_ERROR)
    return _VECTOR_MODEL


# ======================== 工具函数 ========================

def _cosine_sim(vec1, vec2):
    arr1 = np.asarray(vec1, dtype=float)
    arr2 = np.asarray(vec2, dtype=float)
    if arr1.shape != arr2.shape:
        raise ValueError("vector1 和 vector2 维度不一致")
    if arr1.size == 0:
        raise ValueError("向量不能为空")
    denominator = np.linalg.norm(arr1) * np.linalg.norm(arr2)
    if denominator == 0:
        return 0.0
    return float(np.dot(arr1, arr2) / denominator)


def _encode_nodes(nodes):
    model = _load_vector_model()
    return model.encode(nodes)


# ======================== Service 函数 ========================

def s_vector_health(data):
    """向量模型健康检查"""
    try:
        _load_vector_model()
        return True, {"status": "ok", "model_loaded": True}
    except Exception as exc:
        return True, {"status": "ok", "model_loaded": False, "model_error": str(exc)}


def s_vector_embed(data):
    """批量文本向量化"""
    try:
        nodes = data.get("nodes")
        if not isinstance(nodes, list):
            return False, {"error": "nodes 参数必须是列表"}
        if not nodes:
            return True, {"vectors": [], "dimension": 0, "count": 0}
        vectors = _encode_nodes(nodes).tolist()
        return True, {
            "vectors": vectors,
            "dimension": len(vectors[0]) if vectors else 0,
            "count": len(vectors),
        }
    except Exception as exc:
        return False, {"error": str(exc)}


def s_vector_embed_single(data):
    """单条文本向量化"""
    try:
        node = data.get("node")
        vector = _encode_nodes([node]).tolist()[0]
        return True, {"vector": vector, "dimension": len(vector)}
    except Exception as exc:
        return False, {"error": str(exc)}


def s_vector_similarity(data):
    """计算两个向量的余弦相似度"""
    try:
        vec1 = data.get("vector1")
        vec2 = data.get("vector2")
        sim = _cosine_sim(vec1, vec2)
        return True, {"similarity": sim}
    except Exception as exc:
        return False, {"error": str(exc)}


def s_vector_path_similarity(data):
    """计算两条路径的逐层相似度"""
    try:
        path1 = data.get("path1")
        path2 = data.get("path2")
        if not path1 or not path2:
            return False, {"error": "路径不能为空"}
        all_nodes = path1 + path2
        all_vectors = _encode_nodes(all_nodes)
        len1 = len(path1)
        vecs1 = all_vectors[:len1]
        vecs2 = all_vectors[len1:]
        min_len = min(len(path1), len(path2))
        layer_sims = []
        for i in range(min_len):
            layer_sims.append(round(_cosine_sim(vecs1[i], vecs2[i]), 4))
        avg_sim = sum(layer_sims) / min_len if min_len > 0 else 0
        return True, {
            "similarity": round(avg_sim, 4),
            "layer_similarities": layer_sims,
            "min_layers": min_len,
            "path1_length": len(path1),
            "path2_length": len(path2),
        }
    except Exception as exc:
        return False, {"error": str(exc)}


def s_vector_batch_similarity(data):
    """批量路径相似度计算"""
    try:
        new_path = data.get("new_path")
        skeleton_paths = data.get("skeleton_paths")
        if not new_path or not skeleton_paths:
            return False, {"error": "路径不能为空"}
        new_vecs = _encode_nodes(new_path)
        results = []
        for idx, skeleton in enumerate(skeleton_paths):
            if not skeleton:
                continue
            sk_vecs = _encode_nodes(skeleton)
            min_len = min(len(new_path), len(skeleton))
            layer_sims = []
            for i in range(min_len):
                layer_sims.append(_cosine_sim(new_vecs[i], sk_vecs[i]))
            avg_sim = sum(layer_sims) / min_len if min_len > 0 else 0
            results.append({
                "index": idx,
                "similarity": round(avg_sim, 4),
                "layer_similarities": [round(s, 4) for s in layer_sims],
                "min_layers": min_len,
                "skeleton": skeleton,
            })
        results.sort(key=lambda item: item["similarity"], reverse=True)
        return True, {
            "new_path": new_path,
            "new_path_length": len(new_path),
            "results": results,
            "best_match": results[0] if results else None,
        }
    except Exception as exc:
        return False, {"error": str(exc)}
