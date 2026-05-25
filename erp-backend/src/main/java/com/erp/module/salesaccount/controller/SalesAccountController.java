package com.erp.module.salesaccount.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.salesaccount.dto.SalesAccountReqDTO;
import com.erp.module.salesaccount.dto.SalesAccountRespDTO;
import com.erp.module.salesaccount.service.SalesAccountService;
import com.erp.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-accounts")
public class SalesAccountController {

    @Resource
    private SalesAccountService salesAccountService;

    @GetMapping
    public ApiResponse<List<SalesAccountRespDTO>> listAll(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(salesAccountService.listAll(keyword));
    }

    @GetMapping("/my-accounts")
    public ApiResponse<List<SalesAccountRespDTO>> listMyAccounts() {
        return ApiResponse.success(salesAccountService.listMyAccounts(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesAccountRespDTO> getById(@PathVariable Long id) {
        return ApiResponse.success(salesAccountService.getById(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody SalesAccountReqDTO req) {
        Long id = salesAccountService.create(req);
        return ApiResponse.success(id);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody SalesAccountReqDTO req) {
        salesAccountService.update(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        salesAccountService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/users")
    public ApiResponse<List<Long>> getBoundUserIds(@PathVariable Long id) {
        return ApiResponse.success(salesAccountService.getBoundUserIds(id));
    }

    @PostMapping("/{id}/users")
    public ApiResponse<Void> bindUser(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        salesAccountService.bindUser(id, body.get("userId"));
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ApiResponse<Void> unbindUser(@PathVariable Long id, @PathVariable Long userId) {
        salesAccountService.unbindUser(id, userId);
        return ApiResponse.success();
    }
}
