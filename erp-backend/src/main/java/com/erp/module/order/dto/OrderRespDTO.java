package com.erp.module.order.dto;

import com.erp.module.order.entity.SalesOrderItem;
import com.erp.module.order.entity.SalesOrderPayment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderRespDTO {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Long salesPersonId;
    private String salesPersonName;
    private Long salesAccountId;
    private String salesAccountName;
    private String salesAccountDisplayName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String status;
    private String statusDisplay;
    private String tag;
    private String mallOrderInfo;
    private String remark;
    private List<SalesOrderPayment> payments;
    private Long shippingAddressId;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private Long updatedBy;
    private String updatedByName;
    private List<SalesOrderItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
