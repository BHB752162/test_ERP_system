package com.erp.module.customer.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class CustomerReqDTO {
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String phone;

    @NotBlank(message = "加粉时间不能为空")
    private String addFriendTime;

    private String birthday;

    @NotBlank(message = "微信号不能为空")
    private String wechatAccount;

    private Integer level;

    private Integer status;

    private String remark;
}
