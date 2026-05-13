package com.erp.module.audit.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogRespDTO {
    private Long id;
    private Long orderId;
    private String orderNo;
    private String action;
    private String actionDisplay;
    private Long operatorId;
    private String operatorName;
    private String comment;
    private LocalDateTime operatedAt;
}
