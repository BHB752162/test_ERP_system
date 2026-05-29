package com.erp.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.customer.entity.CustomerAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerAuditLogMapper extends BaseMapper<CustomerAuditLog> {
}
