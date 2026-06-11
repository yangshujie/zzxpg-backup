from algs.dataProcess.data_bfill import data_bfill
from algs.dataProcess.data_ffill import data_ffill
from algs.dataProcess.dbscan_outlier import dbscan_outlier
from algs.dataProcess.full_deduplicate import full_deduplicate
from algs.dataProcess.iqr_outlier import iqr_outlier
from algs.dataProcess.mad_outlier import mad_outlier
from algs.dataProcess.regression_outlier import regression_outlier
from algs.dataProcess.smooth_zscore import smooth_zscore
from algs.dataProcess.three_sigma import three_sigma
from algs.dataProcess.time_range import time_range
from algs.dataProcess.timestamp_deduplicate import timestamp_deduplicate
from algs.dataProcess.two_way_smooth_zscore import two_way_smooth_zscore
from algs.dataProcess.zscore_standard import zscore_standard

func_mapping = {"3sigma剔野":"three_sigma",
        "Z-score剔野":"smooth_zscore",
        "IQR离群点剔野":"iqr_outlier",
                "MAD离群点剔野":"mad_outlier",
              "回归残差剔野":"regression_outlier",
                "DBSCAN离群点剔野":"dbscan_outlier",
                "双向Z-score剔野":"two_way_smooth_zscore",
        "数据值完全去重":"full_deduplicate",
        "数据值时间去重":"timestamp_deduplicate",
        "标准化":"zscore_standard",
        "时间范围选择":"time_range",
        "后向填充":"data_bfill",
        "前向填充":"data_ffill"}