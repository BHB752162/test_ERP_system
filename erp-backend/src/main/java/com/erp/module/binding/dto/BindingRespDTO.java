package com.erp.module.binding.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BindingRespDTO {
    private Long id;
    private Long salesWechatId;
    private String wechatAccount;
    private String wechatNickname;
    private String salesPersonName;
    private Long customerId;
    private String customerName;
    private Integer status;
    private LocalDateTime createdAt;
}
