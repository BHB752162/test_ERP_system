package com.erp.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_audit_log")
public class CustomerAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String action;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private Long operatorId;

    @TableField(exist = false)
    private String operatorName;

    private LocalDateTime operatedAt;
}
