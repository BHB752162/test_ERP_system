package com.erp.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT su.* FROM sys_user su WHERE su.username = #{username}")
    SysUser selectByUsername(String username);

    @Select("SELECT sr.role_code FROM sys_user su " +
            "JOIN sys_role sr ON su.role_id = sr.id " +
            "WHERE su.id = #{userId}")
    String selectRoleCodeByUserId(Long userId);
}
