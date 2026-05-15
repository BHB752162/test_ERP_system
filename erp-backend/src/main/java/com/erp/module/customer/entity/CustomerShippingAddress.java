package com.erp.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_shipping_address")
public class CustomerShippingAddress {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String recipientName;

    private String recipientPhone;

    private String address;

    private Integer isDefault;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
