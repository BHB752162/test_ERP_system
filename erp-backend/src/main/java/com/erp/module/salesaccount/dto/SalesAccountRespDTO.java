package com.erp.module.salesaccount.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalesAccountRespDTO {
    private Long id;
    private String accountName;
    private String displayName;
    private String accountType;
    private Integer status;
    private Long createdBy;
    private Long updatedBy;
    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
