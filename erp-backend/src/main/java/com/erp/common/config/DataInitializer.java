package com.erp.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.module.role.entity.SysRole;
import com.erp.module.role.mapper.SysRoleMapper;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin user if not exists
        SysUser admin = sysUserMapper.selectByUsername("admin");
        if (admin == null) {
            SysRole adminRole = sysRoleMapper.selectOne(
                    new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ADMIN"));
            if (adminRole != null) {
                SysUser user = new SysUser();
                user.setUsername("admin");
                user.setPassword(passwordEncoder.encode("admin123"));
                user.setRealName("系统管理员");
                user.setPhone("13800000000");
                user.setStatus(1);
                user.setRoleId(adminRole.getId());
                sysUserMapper.insert(user);
                log.info("默认管理员账号已创建: admin / admin123");
            }
        }
    }
}
