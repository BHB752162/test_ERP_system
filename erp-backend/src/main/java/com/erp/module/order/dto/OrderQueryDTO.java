package com.erp.module.order.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class OrderQueryDTO {
    private Long customerId;
    private String orderNo;
    private String customerName;
    private String wechatAccount;
    private String recipientPhone;
    private String recipientName;
    private String salesPersonName;
    private String tag;
    private String status;
    private String salesAccountName;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdEnd;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate submittedStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate submittedEnd;
}
