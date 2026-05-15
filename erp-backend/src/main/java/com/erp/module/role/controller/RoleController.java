package com.erp.module.role.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.role.dto.RoleRespDTO;
import com.erp.module.role.service.RoleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleRespDTO>> listAll() {
        return ApiResponse.success(roleService.listAll());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }
}
