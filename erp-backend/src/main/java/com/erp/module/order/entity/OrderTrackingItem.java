package com.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("order_tracking_item")
public class OrderTrackingItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long trackingId;
    private String productSku;
    private Integer quantity;

    @TableField(exist = false)
    private String productName;
}
