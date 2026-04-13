package com.ruoyi.system.api.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true) //@EqualsAndHashCode注解的作用就是自动实现model类的equals方法和hashcode方法
@NoArgsConstructor
@ToString           //自动override toString方法
@Accessors(chain = true)  //允许链式访问，User user=new User().setAge(31).setName("pollyduan");//返回对象
@TableName("task_manage")

public class TaskManage extends BaseEntity implements Serializable {

    private long taskId;
    private String taskName;
    private String taskType;
    private int taskScheduled;
    private String taskDescription;
    private String taskStatus;
    private String relatedReport;
    private String dataSource;
    private String dataSourceType;
    private long indicatorSet;
    private Long progress;
    private String userId;
    private String taskSubtype;


}
