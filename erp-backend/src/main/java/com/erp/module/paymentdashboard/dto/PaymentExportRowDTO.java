package com.erp.module.paymentdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentExportRowDTO {
    private String orderNo;
    private String status;
    private LocalDateTime submittedAt;
    private String salesAccountName;
    private String channelTypeName;
    private BigDecimal paymentAmount;
}
