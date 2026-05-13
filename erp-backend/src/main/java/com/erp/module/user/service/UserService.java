package com.erp.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.user.dto.UserReqDTO;
import com.erp.module.user.dto.UserRespDTO;

public interface UserService {
    IPage<UserRespDTO> listUsers(int page, int pageSize, String keyword);
    UserRespDTO getUserById(Long id);
    void createUser(UserReqDTO req);
    void updateUser(Long id, UserReqDTO req);
    void updateStatus(Long id, Integer status);
}
