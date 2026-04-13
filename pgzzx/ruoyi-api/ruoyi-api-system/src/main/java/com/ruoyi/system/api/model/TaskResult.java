//package com.ruoyi.system.api.model;
//
//import com.baomidou.mybatisplus.annotation.TableName;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//import lombok.experimental.Accessors;
//
//@Data
//@EqualsAndHashCode() //@EqualsAndHashCode注解的作用就是自动实现model类的equals方法和hashcode方法
//@NoArgsConstructor
//@ToString           //自动override toString方法
//@Accessors(chain = true)  //允许链式访问，User user=new User().setAge(31).setName("pollyduan");//返回对象
//@TableName("task_result")
//
//public class TaskResult {
//
//    private long reportId;
//
//    private String reportTask;
//
//    private String createBy;
//
//    private String userId;
//
//    private String relatedReport;
//
//}
