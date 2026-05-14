package com.erp.module.audit.service;

import com.erp.module.audit.dto.AuditLogRespDTO;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface AuditLogService {
    List<AuditLogRespDTO> listByOrderId(Long orderId);
    IPage<AuditLogRespDTO> listAll(int page, int pageSize, String orderNo, String action);
}
