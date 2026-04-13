package com.ruoyi.system.api.model;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;


@Data
public class TaskMq implements Serializable {

    private String userId;
    private String taskSubtype;
}
