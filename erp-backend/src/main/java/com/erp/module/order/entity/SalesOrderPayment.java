package com.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sales_order_payment")
public class SalesOrderPayment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long paymentChannelTypeId;

    private BigDecimal paymentAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String paymentChannelTypeName;
}
