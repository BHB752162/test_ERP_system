package com.erp.module.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrackingImportReqDTO {
    private String orderNo;
    private String trackingNo;
    private String productSku;
    private Integer quantity;
    private BigDecimal deliveryAmount;
    private String shippingTime;
    private String shipmentType;
}
