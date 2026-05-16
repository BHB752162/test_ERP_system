package com.erp.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.role.entity.SysRole;
import com.erp.module.role.mapper.SysRoleMapper;
import com.erp.module.user.dto.UserReqDTO;
import com.erp.module.user.dto.UserRespDTO;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.module.user.service.UserService;
import com.erp.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public IPage<UserRespDTO> listUsers(int page, int pageSize, String keyword) {
        Page<SysUser> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword);
        }
        wrapper.ne(SysUser::getStatus, 0).orderByDesc(SysUser::getCreatedAt);

        IPage<SysUser> userPage = sysUserMapper.selectPage(pageParam, wrapper);
        List<UserRespDTO> records = userPage.getRecords().stream().map(this::toRespDTO).collect(Collectors.toList());

        // 批量加载创建人/更新人姓名
        Set<Long> userIds = new java.util.HashSet<>();
        for (UserRespDTO dto : records) {
            if (dto.getCreatedBy() != null) userIds.add(dto.getCreatedBy());
            if (dto.getUpdatedBy() != null) userIds.add(dto.getUpdatedBy());
        }
        if (!userIds.isEmpty()) {
            Map<Long, String> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            for (UserRespDTO dto : records) {
                if (dto.getCreatedBy() != null) dto.setCreatedByName(userMap.get(dto.getCreatedBy()));
                if (dto.getUpdatedBy() != null) dto.setUpdatedByName(userMap.get(dto.getUpdatedBy()));
            }
        }

        Page<UserRespDTO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public UserRespDTO getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return toRespDTO(user);
    }

    @Override
    public void createUser(UserReqDTO req) {
        SysUser existing = sysUserMapper.selectByUsername(req.getUsername());
        if (existing != null) throw new BusinessException("用户名已存在");

        SysUser user = new SysUser();
        BeanUtils.copyProperties(req, user);
        if (StringUtils.isBlank(req.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        if (user.getStatus() == null) user.setStatus(1);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        user.setCreatedBy(currentUserId);
        user.setUpdatedBy(currentUserId);
        sysUserMapper.insert(user);
    }

    @Override
    public void updateUser(Long id, UserReqDTO req) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");

        if (StringUtils.isNotBlank(req.getUsername()) && !req.getUsername().equals(user.getUsername())) {
            SysUser existing = sysUserMapper.selectByUsername(req.getUsername());
            if (existing != null) throw new BusinessException("用户名已存在");
        }

        BeanUtils.copyProperties(req, user);
        if (StringUtils.isNotBlank(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        user.setUpdatedBy(SecurityUtils.getCurrentUserId());
        sysUserMapper.updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(0);
        sysUserMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setUpdatedBy(SecurityUtils.getCurrentUserId());
        sysUserMapper.updateById(user);
    }

    private UserRespDTO toRespDTO(SysUser user) {
        UserRespDTO dto = new UserRespDTO();
        BeanUtils.copyProperties(user, dto);
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        if (role != null) {
            dto.setRoleName(role.getRoleName());
            dto.setRoleCode(role.getRoleCode());
        }
        return dto;
    }
}
