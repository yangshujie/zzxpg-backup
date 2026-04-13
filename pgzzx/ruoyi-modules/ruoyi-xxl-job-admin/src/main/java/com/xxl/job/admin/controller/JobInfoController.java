package com.xxl.job.admin.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import io.swagger.annotations.ApiOperation;
import org.junit.Test;
import org.python.antlr.ast.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * index controller
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {
    private static Logger logger = LoggerFactory.getLogger(JobInfoController.class);

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;
    @Resource
    private XxlJobService xxlJobService;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "-1") int jobGroup) {

        // 枚举-字典
        model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());        // 路由策略-列表
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());                                // Glue类型-字典
        model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());        // 阻塞处理策略-字典
        model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values());                        // 调度类型
        model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values());                    // 调度过期策略

        // 执行器列表
        List<XxlJobGroup> jobGroupList_all = xxlJobGroupDao.findAll();

        // filter group
        List<XxlJobGroup> jobGroupList = filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList == null || jobGroupList.size() == 0) {
            throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("jobGroup", jobGroup);

        return "jobinfo/jobinfo.index";
    }

    public static List<XxlJobGroup> filterJobGroupByRole(HttpServletRequest request, List<XxlJobGroup> jobGroupList_all) {
        List<XxlJobGroup> jobGroupList = new ArrayList<>();
        if (jobGroupList_all != null && jobGroupList_all.size() > 0) {
            XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
//            if (loginUser.getRole() == 1) {
                jobGroupList = jobGroupList_all;
//            } else {
//                List<String> groupIdStrs = new ArrayList<>();
//                if (loginUser.getPermission() != null && loginUser.getPermission().trim().length() > 0) {
//                    groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
//                }
//                for (XxlJobGroup groupItem : jobGroupList_all) {
//                    if (groupIdStrs.contains(String.valueOf(groupItem.getId()))) {
//                        jobGroupList.add(groupItem);
//                    }
//                }
//            }
        }
        return jobGroupList;
    }

    public static void validPermission(HttpServletRequest request, int jobGroup) {
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (!loginUser.validPermission(jobGroup)) {
            throw new RuntimeException(I18nUtil.getString("system_permission_limit") + "[username=" + loginUser.getUsername() + "]");
        }
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author, Integer taskExecType, String firstLevel) {

        return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author, taskExecType, firstLevel);
    }

    @RequestMapping("/add")
    @Log(title = "评估模板", businessType = BusinessType.INSERT)
    @ResponseBody
    public ReturnT<String> add(@RequestBody XxlJobInfo jobInfo) {
        ReturnT<String> returnT = new ReturnT<>();
        List<XxlJobInfo> jobList = xxlJobService.selectJobByName(jobInfo.getTaskName());
        if (jobList.size() != 0){
            returnT.setCode(601);
            returnT.setMsg("任务名称不能重复！");
            return returnT;
        }
        return xxlJobService.add(jobInfo);
    }

    @RequestMapping("/addOneMore")
    @Log(title = "评估模板", businessType = BusinessType.INSERT)
    @ApiOperation("批量创建任务")
    @ResponseBody
    public ReturnT<String> addOneMore(@RequestBody XxlJobInfo jobInfo) {
        ReturnT<String> returnT = new ReturnT<>();
        try {
            jobInfo.setIp(IpUtils.getIpAddr(ServletUtils.getRequest()));

            String[] selectList = jobInfo.getSelectList().split(",");
            String name = jobInfo.getTaskName();
            for (int i = 0, size = selectList.length/3; i < size; i++) {
//                JSONArray select = selectList.getJSONArray(i);
                jobInfo.setIndicatorSystemId(Long.parseLong(selectList[i*3+2]));
                jobInfo.setAssessObject(selectList[3*i+1]);
//                jobInfo.setAssessObjectType(select.getString(0));
                String taskName = name + selectList[3*i+1] + UUID.randomUUID();
                if (selectList.length!=3) {
                    jobInfo.setTaskName(taskName);
                    jobInfo.setJobDesc(taskName);
                }
                ReturnT<String> ret = xxlJobService.add(jobInfo);
                if (ret.getCode() == 500){
                    return ret;
                }
            }
            returnT.setCode(200);
            returnT.setMsg("创建成功");
            return returnT;
        }catch (Exception e){
            returnT.setCode(500);
            returnT.setMsg("创建失败");
            returnT.setContent(e.getMessage());
            return returnT;
        }
    }

    @RequestMapping("/update")
    @Log(title = "评估模板", businessType = BusinessType.UPDATE)
    @ResponseBody
    public ReturnT<String> update(@RequestBody XxlJobInfo jobInfo) {
        ReturnT<String> returnT = new ReturnT<>();
        jobInfo.setIp(IpUtils.getIpAddr(ServletUtils.getRequest()));
        List<XxlJobInfo> jobList = xxlJobService.selectJobByNameAndId(jobInfo.getTaskName(), jobInfo.getId());
        if (jobList.size() != 0){
            returnT.setCode(601);
            returnT.setMsg("任务名称不能重复！");
            return returnT;
        }
        return xxlJobService.update(jobInfo);
    }

    @RequestMapping("/remove")
    @Log(title = "评估模板", businessType = BusinessType.DELETE)
    @ResponseBody
    public ReturnT<String> remove(int id) {
        return xxlJobService.remove(id);
    }

    @RequestMapping("/stop")
    @Log(title = "评估模板", businessType = BusinessType.STOP)
    @ResponseBody
    public ReturnT<String> pause(int id) {
        return xxlJobService.stop(id);
    }

    @RequestMapping("/start")
    @Log(title = "评估模板", businessType = BusinessType.START)
    @ResponseBody
    public ReturnT<String> start(int id) {
        return xxlJobService.start(id);
    }

    @RequestMapping("/trigger")
    @Log(title = "评估模板", businessType = BusinessType.TRIGGER)
    @ResponseBody
    //@PermissionLimit(limit = false)
    public ReturnT<String> triggerJob(int id, String executorParam, String addressList) {
        // force cover job param
        if (executorParam == null) {
            executorParam = "";
        }

        JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {

        XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<List<String>>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<List<String>>(result);

    }

    @ApiOperation("获取所有任务类型")
    @RequestMapping("/getTaskType")
    @ResponseBody
    public AjaxResult getTaskType(){
        try{
            return AjaxResult.success("查询成功",xxlJobService.getTaskType());
        }catch (Exception e){
            return AjaxResult.error("查询失败");
        }
    }

    //---------------------统计周期执行中任务--------------------------
    @ApiOperation("获取周期任务数")
    @GetMapping("/getRoundtaskNum")
    @ResponseBody
    public AjaxResult getRoundtaskNum(){
        try{
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("value",xxlJobService.getRoundTaskNum());
            return AjaxResult.success(jsonObject);
        }catch (Exception e){
            return AjaxResult.error("查询失败");
        }
    }

    @ApiOperation("获取下次执行任务")
    @GetMapping("/getNextTriggerTime")
    @ResponseBody
    public AjaxResult getNextTriggerTime(){
        try{
            return AjaxResult.success(xxlJobService.getNextTriggerTime());
        }catch (Exception e){
            return AjaxResult.error("查询失败");
        }
    }

}
