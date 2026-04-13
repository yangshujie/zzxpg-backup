package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;


/**
 * job info
 *
 * @author xuxueli 2016-1-12 18:03:45
 */
@Mapper
public interface XxlJobInfoDao {

    public List<XxlJobInfo> pageList(@Param("offset") int offset,
                                     @Param("pagesize") int pagesize,
                                     @Param("jobGroup") int jobGroup,
                                     @Param("triggerStatus") int triggerStatus,
                                     @Param("jobDesc") String jobDesc,
                                     @Param("executorHandler") String executorHandler,
                                     @Param("author") String author,
                                     @Param("taskExecType") Integer taskExecType,
                                     @Param("firstLevel") String firstLevel);

    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("jobGroup") int jobGroup,
                             @Param("triggerStatus") int triggerStatus,
                             @Param("jobDesc") String jobDesc,
                             @Param("executorHandler") String executorHandler,
                             @Param("author") String author,
                             @Param("taskExecType") Integer taskExecType,
                             @Param("firstLevel") String firstLevel);

    public int save(XxlJobInfo info);

    public XxlJobInfo loadById(@Param("id") int id);

    public int update(XxlJobInfo xxlJobInfo);

    public int delete(@Param("id") long id);

    public List<XxlJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

    public int findAllCount();

    public List<XxlJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);

    public int scheduleUpdate(XxlJobInfo xxlJobInfo);

    @Select("select first_level from xxl_job_info where first_level != 'null'")
    public List<String> getTaskType();

    @Select("select count(id) from xxl_job_info where trigger_status = 1")
    public int getRoundTaskNum();

    @Select("select job_desc, trigger_next_time FROM xxl_job_info WHERE trigger_status=1 ORDER BY trigger_next_time")
    public List<Map<String, Object>> getNextTriggerTime();

    @Select("select * from xxl_job_info where task_name='${arg0}'")
    public List<XxlJobInfo> selectJobByName(String name);

    @Select("select * from xxl_job_info where task_name='${arg0}' and id != ${arg1}")
    public List<XxlJobInfo> selectJobByNameAndId(String name, Integer id);
}
