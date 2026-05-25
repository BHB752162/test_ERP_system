package com.erp.module.customer.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class CustomerContactReqDTO {
    private Long customerId;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    private String phone;

    private String email;

    private String position;

    private Integer isPrimary;

    private String remark;
}
