package com.erp.module.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.module.role.dto.RoleRespDTO;
import com.erp.module.role.entity.SysRole;
import com.erp.module.role.mapper.SysRoleMapper;
import com.erp.module.role.service.RoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Override
    public List<RoleRespDTO> listAll() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<>())
                .stream().map(this::toRespDTO)
                .collect(Collectors.toList());
    }

    private RoleRespDTO toRespDTO(SysRole role) {
        RoleRespDTO dto = new RoleRespDTO();
        BeanUtils.copyProperties(role, dto);
        return dto;
    }
}
