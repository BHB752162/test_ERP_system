package com.erp.module.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_audit_log")
public class OrderAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String action;

    private Long operatorId;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operatedAt;
}
