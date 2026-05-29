package com.erp.module.paymentdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentChannelSummaryDTO {
    private String channelTypeName;
    private BigDecimal totalAmount;
}
