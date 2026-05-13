package com.erp.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResult;
import com.erp.module.user.dto.UserReqDTO;
import com.erp.module.user.dto.UserRespDTO;
import com.erp.module.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserRespDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        IPage<UserRespDTO> result = userService.listUsers(page, pageSize, keyword);
        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserRespDTO> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody UserReqDTO req) {
        userService.createUser(req);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserReqDTO req) {
        userService.updateUser(id, req);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResponse.success();
    }
}
