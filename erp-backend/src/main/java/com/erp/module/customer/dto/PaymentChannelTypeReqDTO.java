package com.erp.module.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PaymentChannelTypeReqDTO {
    @NotBlank(message = "类型编码不能为空")
    private String typeCode;

    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    private Integer sortOrder;

    private Integer status;
}
