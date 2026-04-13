package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.domain.zhpg.EvalDataSource;
import com.ruoyi.domain.zhpg.dto.DataSourceTestResult;

import java.util.List;

/**
 * 评估数据源Service接口
 */
public interface IEvalDataSourceService {

    Page<EvalDataSource> selectDataSourcePage(Page<?> page, EvalDataSource query);

    List<EvalDataSource> selectDataSourceList(EvalDataSource query);

    List<EvalDataSource> selectEnabledList(EvalDataSource query);

    EvalDataSource selectDataSourceDetail(Long id);

    int insertDataSource(EvalDataSource dataSource);

    int updateDataSource(EvalDataSource dataSource);

    int deleteDataSourceByIds(Long[] ids);

    DataSourceTestResult testConnection(EvalDataSource dataSource);
}
