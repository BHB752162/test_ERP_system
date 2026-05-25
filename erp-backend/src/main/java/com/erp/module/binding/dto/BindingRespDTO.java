package com.erp.module.binding.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BindingRespDTO {
    private Long id;
    private Long salesAccountId;
    private String salesAccountName;
    private String salesAccountDisplayName;
    private Long customerId;
    private String customerName;
    private LocalDateTime createdAt;
}
