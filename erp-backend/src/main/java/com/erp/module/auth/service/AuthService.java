package com.erp.module.auth.service;

import com.erp.module.auth.dto.LoginReqDTO;
import com.erp.module.auth.dto.LoginRespDTO;

public interface AuthService {
    LoginRespDTO login(LoginReqDTO req);
    LoginRespDTO getUserInfo(Long userId);
}
