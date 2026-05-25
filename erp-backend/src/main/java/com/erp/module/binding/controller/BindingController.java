package com.erp.module.binding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResult;
import com.erp.module.binding.dto.BindingReqDTO;
import com.erp.module.binding.dto.BindingRespDTO;
import com.erp.module.binding.service.BindingService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sales-bindings")
public class BindingController {

    @Resource
    private BindingService bindingService;

    @GetMapping
    public ApiResponse<PageResult<BindingRespDTO>> listAll(
            @RequestParam(required = false) Long salesAccountId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(PageResult.of(bindingService.listAll(salesAccountId, customerId, page, pageSize)));
    }

    @GetMapping("/bound-customers")
    public ApiResponse<List<BindingRespDTO>> listBoundCustomers(@RequestParam Long salesAccountId) {
        return ApiResponse.success(bindingService.listBoundCustomersByAccount(salesAccountId));
    }

    @GetMapping("/bound-accounts")
    public ApiResponse<List<BindingRespDTO>> listBoundAccounts(@RequestParam Long customerId) {
        return ApiResponse.success(bindingService.listBoundAccountsByCustomer(customerId));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody BindingReqDTO req) {
        bindingService.create(req);
        return ApiResponse.success();
    }

    @PostMapping("/self-service")
    public ApiResponse<Void> createSelfBinding(@Valid @RequestBody BindingReqDTO req) {
        bindingService.createSelfBinding(req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> unbind(@PathVariable Long id) {
        bindingService.unbind(id);
        return ApiResponse.success();
    }
}
