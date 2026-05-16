package com.erp.module.auth.service;

import com.erp.module.auth.dto.*;

public interface AuthService {
    LoginRespDTO login(LoginReqDTO req);
    LoginRespDTO getUserInfo(Long userId);
    void updateUserInfo(UserInfoUpdateReqDTO req);
    void changePassword(ChangePasswordReqDTO req);
    void verifyPassword(String oldPassword);
}
