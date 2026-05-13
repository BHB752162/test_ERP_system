package com.erp.module.auth.service.impl;

import com.erp.common.exception.BusinessException;
import com.erp.module.auth.dto.LoginReqDTO;
import com.erp.module.auth.dto.LoginRespDTO;
import com.erp.module.auth.service.AuthService;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginRespDTO login(LoginReqDTO req) {
        SysUser user = sysUserMapper.selectByUsername(req.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String roleCode = sysUserMapper.selectRoleCodeByUserId(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);

        LoginRespDTO resp = new LoginRespDTO();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRoleCode(roleCode);
        resp.setRoleName(getRoleName(roleCode));
        return resp;
    }

    @Override
    public LoginRespDTO getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String roleCode = sysUserMapper.selectRoleCodeByUserId(userId);

        LoginRespDTO resp = new LoginRespDTO();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRoleCode(roleCode);
        resp.setRoleName(getRoleName(roleCode));
        return resp;
    }

    private String getRoleName(String roleCode) {
        switch (roleCode) {
            case "ADMIN": return "管理员";
            case "SALES_MANAGER": return "销售经理";
            case "SALES_PERSON": return "销售人员";
            default: return "未知角色";
        }
    }
}
