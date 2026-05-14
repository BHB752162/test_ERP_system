package com.erp.module.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.module.audit.dto.AuditLogRespDTO;
import com.erp.module.audit.entity.OrderAuditLog;
import com.erp.module.audit.mapper.OrderAuditLogMapper;
import com.erp.module.audit.service.AuditLogService;
import com.erp.module.order.entity.SalesOrder;
import com.erp.module.order.mapper.SalesOrderMapper;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private OrderAuditLogMapper orderAuditLogMapper;

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public List<AuditLogRespDTO> listByOrderId(Long orderId) {
        return orderAuditLogMapper.selectList(
                new LambdaQueryWrapper<OrderAuditLog>()
                        .eq(OrderAuditLog::getOrderId, orderId)
                        .orderByAsc(OrderAuditLog::getOperatedAt))
                .stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    @Override
    public IPage<AuditLogRespDTO> listAll(int page, int pageSize, String orderNo, String action) {
        // Resolve order IDs if orderNo is provided
        List<Long> orderIds = null;
        if (orderNo != null && !orderNo.isEmpty()) {
            orderIds = salesOrderMapper.selectList(
                    new LambdaQueryWrapper<SalesOrder>()
                            .like(SalesOrder::getOrderNo, orderNo)
                            .select(SalesOrder::getId))
                    .stream().map(SalesOrder::getId).collect(Collectors.toList());
            if (orderIds.isEmpty()) {
                return new Page<>(page, pageSize);
            }
        }

        // Query audit logs
        Page<OrderAuditLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<OrderAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (orderIds != null) {
            wrapper.in(OrderAuditLog::getOrderId, orderIds);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(OrderAuditLog::getAction, action);
        }
        wrapper.orderByDesc(OrderAuditLog::getOperatedAt);

        Page<OrderAuditLog> result = orderAuditLogMapper.selectPage(pageParam, wrapper);
        Page<AuditLogRespDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::toRespDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    private AuditLogRespDTO toRespDTO(OrderAuditLog log) {
        AuditLogRespDTO dto = new AuditLogRespDTO();
        dto.setId(log.getId());
        dto.setOrderId(log.getOrderId());
        dto.setAction(log.getAction());
        dto.setActionDisplay(getActionDisplay(log.getAction()));
        dto.setOperatorId(log.getOperatorId());
        dto.setComment(log.getComment());
        dto.setOperatedAt(log.getOperatedAt());

        SalesOrder order = salesOrderMapper.selectById(log.getOrderId());
        if (order != null) dto.setOrderNo(order.getOrderNo());

        SysUser user = sysUserMapper.selectById(log.getOperatorId());
        if (user != null) dto.setOperatorName(user.getRealName());

        return dto;
    }

    private String getActionDisplay(String action) {
        switch (action) {
            case "SUBMIT": return "提交审批";
            case "APPROVE": return "审批通过";
            case "REJECT": return "审批驳回";
            case "CANCEL": return "取消订单";
            case "COMPLETE": return "完成订单";
            default: return action;
        }
    }
}
