package com.erp.module.binding.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.binding.dto.BindingReqDTO;
import com.erp.module.binding.dto.BindingRespDTO;
import com.erp.module.binding.service.BindingService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bindings")
public class BindingController {

    @Resource
    private BindingService bindingService;

    @GetMapping
    public ApiResponse<List<BindingRespDTO>> listAll(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(bindingService.listAll(keyword));
    }

    @GetMapping("/bound-customers")
    public ApiResponse<List<BindingRespDTO>> listBoundCustomers(@RequestParam Long wechatId) {
        return ApiResponse.success(bindingService.listBoundCustomersByWechat(wechatId));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody BindingReqDTO req) {
        bindingService.create(req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> unbind(@PathVariable Long id) {
        bindingService.unbind(id);
        return ApiResponse.success();
    }
}
