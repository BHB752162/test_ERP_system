package com.erp.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResult;
import com.erp.module.order.dto.OrderCreateReqDTO;
import com.erp.module.order.dto.OrderQueryDTO;
import com.erp.module.order.dto.OrderRespDTO;
import com.erp.module.order.dto.TrackingImportReqDTO;
import com.erp.module.order.entity.OrderTracking;
import org.springframework.security.access.prepost.PreAuthorize;
import com.erp.module.order.entity.SalesOrder;
import com.erp.module.order.service.OrderService;
import com.erp.security.SecurityUtils;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<SalesOrder>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            OrderQueryDTO query) {
        Long userId = SecurityUtils.getCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        IPage<SalesOrder> result = orderService.listOrders(page, pageSize, query, userId, roleCode);
        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderRespDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.success(orderService.getOrderDetail(id));
    }

    @PostMapping
    public ApiResponse<SalesOrder> createDraft(@Valid @RequestBody OrderCreateReqDTO req) {
        SalesOrder order = orderService.createDraft(req, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(order);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody OrderCreateReqDTO req) {
        orderService.updateOrder(id, req, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        orderService.submitForApproval(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestParam(required = false) String comment) {
        orderService.approveOrder(id, SecurityUtils.getCurrentUserId(), comment);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        orderService.rejectOrder(id, SecurityUtils.getCurrentUserId(), comment);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<Void> ship(@PathVariable Long id) {
        orderService.shipOrder(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/deliver")
    public ApiResponse<Void> deliver(@PathVariable Long id) {
        orderService.deliverOrder(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<Void> refund(@PathVariable Long id, @RequestParam(required = false) String comment) {
        orderService.refundOrder(id, SecurityUtils.getCurrentUserId(), comment);
        return ApiResponse.success();
    }

    @PostMapping("/import-tracking")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> importTracking(@RequestBody List<TrackingImportReqDTO> data) {
        orderService.importTracking(data, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @GetMapping("/{id}/tracking")
    public ApiResponse<List<OrderTracking>> listTracking(@PathVariable Long id) {
        return ApiResponse.success(orderService.listTracking(id));
    }

    @DeleteMapping("/tracking/{trackingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTracking(@PathVariable Long trackingId) {
        orderService.deleteTracking(trackingId, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "order") String mode,
            OrderQueryDTO query) {
        Long userId = SecurityUtils.getCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        byte[] excelBytes = orderService.exportOrders(mode, query, userId, roleCode);

        String suffix = "product".equals(mode) ? "by_product" : "by_order";
        String filename = "orders_" + suffix + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
