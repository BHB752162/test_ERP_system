package com.erp.module.customer.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class PaymentChannelReqDTO {
    private Long customerId;

    @NotBlank(message = "渠道类型不能为空")
    private String channelType;

    private String channelAccount;

    private String accountName;

    private String bankName;

    private Integer isDefault;

    private Integer status;
}
