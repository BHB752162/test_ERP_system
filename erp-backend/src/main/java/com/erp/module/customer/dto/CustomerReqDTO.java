package com.erp.module.customer.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class CustomerReqDTO {
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String phone;

    private String email;

    private String address;

    private Integer level;

    private Integer status;

    private String remark;
}
