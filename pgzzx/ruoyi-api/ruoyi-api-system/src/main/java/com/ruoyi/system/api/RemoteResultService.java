//package com.ruoyi.system.api;
//
//import com.ruoyi.common.core.web.domain.AjaxResult;
//import com.ruoyi.system.api.domain.SysFile;
//import com.ruoyi.system.api.domain.SysUser;
//import com.ruoyi.system.api.factory.RemoteUserFallbackFactory;
//import com.ruoyi.system.api.model.LoginUser;
//import com.ruoyi.system.api.model.TaskManage;
//import com.ruoyi.system.api.model.TaskMq;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.http.converter.json.GsonBuilderUtils;
//import org.springframework.web.bind.annotation.*;
//import com.ruoyi.common.core.constant.SecurityConstants;
//import com.ruoyi.common.core.constant.ServiceNameConstants;
//import com.ruoyi.common.core.domain.R;
//import org.springframework.web.multipart.MultipartFile;
//import org.w3c.dom.ls.LSOutput;
//import java.util.List;
//
//
//@FeignClient(contextId = "remoteResultService", value = ServiceNameConstants.FILE_SERVICE, fallbackFactory = RemoteUserFallbackFactory.class)
////为每个FeignClient手动指定不同的contextId，contextId的作用是用来区分FeignClient实例
////value对应nacos里的服务名
//
//
//public interface RemoteResultService {
//
//    /**
//     * 根据任务编号获取详细信息
//     *
//     * @param file 请求来源
//     * @return 结果
//     */
////    @GetMapping("/taskmanage/list")
////    public R<List<TaskManage>> getTaskInfo(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
//
//    @PostMapping("upload")
//    public R<SysFile> upload(@RequestParam(value = "file") @RequestPart MultipartFile file,
//                             @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
//
//}
