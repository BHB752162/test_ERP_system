package com.erp.module.audit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResult;
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

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditLogRespDTO>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String action) {
        IPage<AuditLogRespDTO> result = auditLogService.listAll(page, pageSize, orderNo, action);
        return ApiResponse.success(PageResult.of(result));
    }
}
