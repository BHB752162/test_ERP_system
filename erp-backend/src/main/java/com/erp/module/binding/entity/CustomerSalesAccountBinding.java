package com.erp.module.binding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_sales_account_binding")
public class CustomerSalesAccountBinding {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private Long salesAccountId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
