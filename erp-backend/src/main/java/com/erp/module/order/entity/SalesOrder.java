package com.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sales_order")
public class SalesOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long customerId;

    private Long salesPersonId;

    private Long salesAccountId;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private String status;

    private String tag;

    private String mallOrderInfo;

    private String remark;

    private Long shippingAddressId;

    private String recipientName;

    private String recipientPhone;

    private String recipientAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private Long updatedBy;

    @Version
    private Integer version;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String salesPersonName;

    @TableField(exist = false)
    private String salesAccountName;

    @TableField(exist = false)
    private String salesAccountDisplayName;

    @TableField(exist = false)
    private String statusDisplay;

    @TableField(exist = false)
    private String paymentChannelTypeName;

    @TableField(exist = false)
    private BigDecimal paymentAmountDisplay;

    @TableField(exist = false)
    private List<OrderTracking> trackings;
}
