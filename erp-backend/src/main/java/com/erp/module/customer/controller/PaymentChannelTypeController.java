package com.erp.module.customer.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.customer.entity.PaymentChannelType;
import com.erp.module.customer.service.PaymentChannelTypeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/payment-channel-types")
public class PaymentChannelTypeController {

    @Resource
    private PaymentChannelTypeService paymentChannelTypeService;

    @GetMapping
    public ApiResponse<List<PaymentChannelType>> listAll() {
        return ApiResponse.success(paymentChannelTypeService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentChannelType> getById(@PathVariable Long id) {
        return ApiResponse.success(paymentChannelTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ApiResponse<Void> create(@Valid @RequestBody PaymentChannelType req) {
        paymentChannelTypeService.create(req);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PaymentChannelType req) {
        paymentChannelTypeService.update(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paymentChannelTypeService.delete(id);
        return ApiResponse.success();
    }
}
