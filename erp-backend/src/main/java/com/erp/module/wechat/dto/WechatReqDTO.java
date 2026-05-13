package com.erp.module.wechat.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class WechatReqDTO {
    @NotNull(message = "所属销售不能为空")
    private Long salesPersonId;

    @NotBlank(message = "微信号不能为空")
    private String wechatAccount;

    private String wechatNickname;

    private String qrCode;

    private Integer status;
}
