package com.erp.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_contact")
public class CustomerContact {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String contactName;

    private String phone;

    private String email;

    private String position;

    private Integer isPrimary;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
