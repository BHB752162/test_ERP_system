package com.erp.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.module.customer.entity.PaymentChannelType;
import com.erp.module.customer.mapper.PaymentChannelTypeMapper;
import com.erp.module.customer.service.PaymentChannelTypeService;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.security.SecurityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PaymentChannelTypeServiceImpl implements PaymentChannelTypeService {

    @Resource
    private PaymentChannelTypeMapper paymentChannelTypeMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public List<PaymentChannelType> listAll() {
        List<PaymentChannelType> list = paymentChannelTypeMapper.selectList(
                new LambdaQueryWrapper<PaymentChannelType>()
                        .ne(PaymentChannelType::getStatus, 0)
                        .orderByAsc(PaymentChannelType::getSortOrder));

        // Batch load creator/updater names
        Set<Long> userIds = list.stream()
                .flatMap(t -> Stream.of(t.getCreatedBy(), t.getUpdatedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            Map<Long, String> userMap = sysUserMapper.selectBatchIds(userIds)
                    .stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            for (PaymentChannelType t : list) {
                if (t.getCreatedBy() != null) t.setCreatedByName(userMap.get(t.getCreatedBy()));
                if (t.getUpdatedBy() != null) t.setUpdatedByName(userMap.get(t.getUpdatedBy()));
            }
        }
        return list;
    }

    @Override
    public PaymentChannelType getById(Long id) {
        PaymentChannelType type = paymentChannelTypeMapper.selectById(id);
        if (type == null) throw new BusinessException("渠道类型不存在");
        return type;
    }

    @Override
    public void create(PaymentChannelType req) {
        long count = paymentChannelTypeMapper.selectCount(
                new LambdaQueryWrapper<PaymentChannelType>()
                        .eq(PaymentChannelType::getTypeCode, req.getTypeCode()));
        if (count > 0) throw new BusinessException("类型编码已存在");
        if (req.getStatus() == null) req.setStatus(1);
        if (req.getSortOrder() == null) req.setSortOrder(0);
        req.setId(null); // 防御：防止前端传入残留id导致主键冲突
        Long userId = SecurityUtils.getCurrentUserId();
        req.setCreatedBy(userId);
        req.setUpdatedBy(userId);
        paymentChannelTypeMapper.insert(req);
    }

    @Override
    public void update(Long id, PaymentChannelType req) {
        PaymentChannelType type = paymentChannelTypeMapper.selectById(id);
        if (type == null) throw new BusinessException("渠道类型不存在");
        // Check unique code if changed
        if (!type.getTypeCode().equals(req.getTypeCode())) {
            long count = paymentChannelTypeMapper.selectCount(
                    new LambdaQueryWrapper<PaymentChannelType>()
                            .eq(PaymentChannelType::getTypeCode, req.getTypeCode()));
            if (count > 0) throw new BusinessException("类型编码已存在");
        }
        req.setId(id);
        req.setUpdatedBy(SecurityUtils.getCurrentUserId());
        paymentChannelTypeMapper.updateById(req);
    }

    @Override
    public void delete(Long id) {
        PaymentChannelType type = paymentChannelTypeMapper.selectById(id);
        if (type == null) throw new BusinessException("渠道类型不存在");
        type.setStatus(0);
        paymentChannelTypeMapper.updateById(type);
    }
}
