package com.erp.module.wechat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sales_wechat")
public class SalesWechat {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long salesPersonId;

    private String wechatAccount;

    private String wechatNickname;

    private String qrCode;

    private Integer status;

    private Long createdBy;

    private Long updatedBy;

    @TableField(exist = false)
    private String createdByName;

    @TableField(exist = false)
    private String updatedByName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
