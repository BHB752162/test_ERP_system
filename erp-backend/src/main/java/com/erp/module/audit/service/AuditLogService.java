package com.erp.module.audit.service;

import com.erp.module.audit.dto.AuditLogRespDTO;

import java.util.List;

public interface AuditLogService {
    List<AuditLogRespDTO> listByOrderId(Long orderId);
}
