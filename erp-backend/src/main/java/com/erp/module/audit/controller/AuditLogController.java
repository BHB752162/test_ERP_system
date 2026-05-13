package com.erp.module.audit.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.audit.dto.AuditLogRespDTO;
import com.erp.module.audit.service.AuditLogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuditLogController {

    @Resource
    private AuditLogService auditLogService;

    @GetMapping("/orders/{orderId}/audit-logs")
    public ApiResponse<List<AuditLogRespDTO>> listByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(auditLogService.listByOrderId(orderId));
    }
}
