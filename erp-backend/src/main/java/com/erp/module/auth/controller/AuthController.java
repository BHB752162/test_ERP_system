package com.erp.module.auth.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.auth.dto.*;
import com.erp.module.auth.service.AuthService;
import com.erp.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginRespDTO> login(@Valid @RequestBody LoginReqDTO req) {
        return ApiResponse.success(authService.login(req));
    }

    @GetMapping("/user-info")
    public ApiResponse<LoginRespDTO> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @PutMapping("/user-info")
    public ApiResponse<Void> updateUserInfo(@Valid @RequestBody UserInfoUpdateReqDTO req) {
        authService.updateUserInfo(req);
        return ApiResponse.success();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordReqDTO req) {
        authService.changePassword(req);
        return ApiResponse.success();
    }

    @PostMapping("/verify-password")
    public ApiResponse<Void> verifyPassword(@RequestBody java.util.Map<String, String> body) {
        authService.verifyPassword(body.get("oldPassword"));
        return ApiResponse.success();
    }
}
