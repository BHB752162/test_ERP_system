package com.erp.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResult;
import com.erp.module.order.dto.OrderCreateReqDTO;
import com.erp.module.order.dto.OrderRespDTO;
import com.erp.module.order.entity.SalesOrder;
import com.erp.module.order.service.OrderService;
import com.erp.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<SalesOrder>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        Long userId = SecurityUtils.getCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        IPage<SalesOrder> result = orderService.listOrders(page, pageSize, status, userId, roleCode);
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
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam String comment) {
        orderService.rejectOrder(id, SecurityUtils.getCurrentUserId(), comment);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        orderService.completeOrder(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }
}
