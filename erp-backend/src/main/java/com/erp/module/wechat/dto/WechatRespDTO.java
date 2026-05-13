package com.erp.module.wechat.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WechatRespDTO {
    private Long id;
    private Long salesPersonId;
    private String salesPersonName;
    private String wechatAccount;
    private String wechatNickname;
    private String qrCode;
    private Integer status;
    private LocalDateTime createdAt;
}
