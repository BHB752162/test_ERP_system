package com.erp.module.customer.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PaymentChannelReqDTO {
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "渠道类型不能为空")
    private String channelType;

    private String channelAccount;

    private String accountName;

    private String bankName;

    private Integer isDefault;

    private Integer status;
}
