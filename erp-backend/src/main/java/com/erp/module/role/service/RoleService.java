package com.erp.module.role.service;

import com.erp.module.role.dto.RoleRespDTO;

import java.util.List;

public interface RoleService {
    List<RoleRespDTO> listAll();
    void delete(Long id);
}
