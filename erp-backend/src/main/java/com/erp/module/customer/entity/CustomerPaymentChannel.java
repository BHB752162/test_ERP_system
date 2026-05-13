package com.erp.module.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_payment_channel")
public class CustomerPaymentChannel {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String channelType;

    private String channelAccount;

    private String accountName;

    private String bankName;

    private Integer isDefault;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
