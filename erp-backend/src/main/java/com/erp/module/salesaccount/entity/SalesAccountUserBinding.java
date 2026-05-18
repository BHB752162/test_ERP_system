package com.erp.module.salesaccount.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sales_account_user_binding")
public class SalesAccountUserBinding {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long salesAccountId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
