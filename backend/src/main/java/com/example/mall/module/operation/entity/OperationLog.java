package com.example.mall.module.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operatorName;

    private Integer operatorRole;

    private String module;

    private String action;

    private String description;

    private String targetId;

    private String requestIp;

    private String requestUrl;

    private String httpMethod;

    private Integer result;

    private String errorMsg;

    private Integer durationMs;

    private Date createdAt;
}
