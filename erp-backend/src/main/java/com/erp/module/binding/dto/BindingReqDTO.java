package com.erp.module.binding.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class BindingReqDTO {
    @NotNull(message = "微信号ID不能为空")
    private Long salesWechatId;

    @NotNull(message = "客户ID不能为空")
    private Long customerId;
}
