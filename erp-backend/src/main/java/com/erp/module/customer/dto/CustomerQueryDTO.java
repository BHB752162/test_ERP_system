package com.erp.module.customer.dto;

import lombok.Data;

@Data
public class CustomerQueryDTO {
    private String customerName;
    private String phone;
    private String wechatAccount;
    private String createdByName;
    private String salesAccountName;
}
