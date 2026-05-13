package com.erp.module.wechat.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.wechat.dto.WechatReqDTO;
import com.erp.module.wechat.dto.WechatRespDTO;
import com.erp.module.wechat.service.SalesWechatService;
import com.erp.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sales-wechats")
public class SalesWechatController {

    @Resource
    private SalesWechatService salesWechatService;

    @GetMapping("/by-sales/{salesPersonId}")
    public ApiResponse<List<WechatRespDTO>> listBySalesPerson(@PathVariable Long salesPersonId) {
        return ApiResponse.success(salesWechatService.listBySalesPerson(salesPersonId));
    }

    @GetMapping("/my-wechats")
    public ApiResponse<List<WechatRespDTO>> listMyWechats() {
        return ApiResponse.success(salesWechatService.listMyWechats(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<WechatRespDTO> getById(@PathVariable Long id) {
        return ApiResponse.success(salesWechatService.getById(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody WechatReqDTO req) {
        salesWechatService.create(req);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody WechatReqDTO req) {
        salesWechatService.update(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        salesWechatService.delete(id);
        return ApiResponse.success();
    }
}
