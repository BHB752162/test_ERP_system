package com.erp.module.paymentdashboard.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaymentDashboardRespDTO {
    private List<PaymentChannelSummaryDTO> today;
    private List<PaymentChannelSummaryDTO> last7Days;
    private List<PaymentChannelSummaryDTO> last30Days;
}
