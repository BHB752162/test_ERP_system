package com.erp.module.salesaccount.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SalesAccountReqDTO {
    @NotBlank(message = "销售账户不能为空")
    private String accountName;

    private String displayName;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    private Integer status;
}
