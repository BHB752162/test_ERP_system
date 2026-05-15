package com.erp.module.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
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
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .ne(SysRole::getStatus, 0))
                .stream().map(this::toRespDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        role.setStatus(0);
        sysRoleMapper.updateById(role);
    }

    private RoleRespDTO toRespDTO(SysRole role) {
        RoleRespDTO dto = new RoleRespDTO();
        BeanUtils.copyProperties(role, dto);
        return dto;
    }
}
