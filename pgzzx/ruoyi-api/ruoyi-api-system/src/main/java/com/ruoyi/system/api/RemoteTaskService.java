package com.ruoyi.system.api;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.factory.RemoteUserFallbackFactory;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.system.api.model.TaskManage;
import com.ruoyi.system.api.model.TaskMq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import org.w3c.dom.ls.LSOutput;
import java.util.List;


@FeignClient(contextId = "remoteTaskService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteUserFallbackFactory.class)
//为每个FeignClient手动指定不同的contextId，contextId的作用是用来区分FeignClient实例
//value对应nacos里的服务名
public interface RemoteTaskService
{


    /**
     * 根据任务编号获取详细信息 controller line51
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/taskmanage/list")
    public R<List<TaskManage>> getTaskInfo(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);




    /**
     * insert// controller line 135
     *
     * @param source 请求来源
     * @return 结果
     */
    @PostMapping("/taskmanage/addTaskManage")
    public R<TaskManage> insertTaskInfo(@RequestBody TaskManage task, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);




    /**
     * task-userid
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/taskmq/taskuser/list")
    public R<List<TaskMq>> getTaskUserInfo(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);




    /**
     * task-userid
     *
     * @param taskManage 请求来源
     * @return 结果
     */
    @PostMapping("/taskmq/taskuser/test11")
     public R<TaskManage> insertTaskManage(@RequestBody TaskManage taskManage, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);



    /**
     * task-userid
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/taskuser/test22")
    public R<Void> insertMqTaskUser(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);












//1219
    /**
     * task-userid
     *
     * @param taskManage 请求来源
     * @return 结果
     */

    @PostMapping("/taskmq/progress/update1219")
    public R<TaskManage> updateTaskManage(@RequestBody TaskManage taskManage, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
//1219



}

