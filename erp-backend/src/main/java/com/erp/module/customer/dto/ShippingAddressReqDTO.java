package com.erp.module.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ShippingAddressReqDTO {
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "收件人姓名不能为空")
    private String recipientName;

    @NotBlank(message = "收件人电话不能为空")
    private String recipientPhone;

    @NotBlank(message = "收件地址不能为空")
    private String address;

    private Integer isDefault;

    private Integer status;
}
