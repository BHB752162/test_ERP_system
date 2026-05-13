package com.erp.module.binding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_wechat_binding")
public class CustomerWechatBinding {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long salesWechatId;

    private Long customerId;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
