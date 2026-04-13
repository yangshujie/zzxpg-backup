package com.ruoyi.mapper.zhpg;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 跨 schema 查询 xxl-job-admin 的执行器组ID
 */
@Mapper
public interface XxlJobAdminMapper {

    @Select("SELECT id FROM ryjob.xxl_job_group WHERE app_name = #{appName} ORDER BY id LIMIT 1")
    Integer selectJobGroupIdByAppName(String appName);

    @Select("SELECT id FROM ryjob.xxl_job_info WHERE task_name = #{taskName} ORDER BY id LIMIT 1")
    Integer selectJobIdByTaskName(String taskName);
}
