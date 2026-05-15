package com.erp.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.module.customer.entity.PaymentChannelType;
import com.erp.module.customer.mapper.PaymentChannelTypeMapper;
import com.erp.module.customer.service.PaymentChannelTypeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PaymentChannelTypeServiceImpl implements PaymentChannelTypeService {

    @Resource
    private PaymentChannelTypeMapper paymentChannelTypeMapper;

    @Override
    public List<PaymentChannelType> listAll() {
        return paymentChannelTypeMapper.selectList(
                new LambdaQueryWrapper<PaymentChannelType>()
                        .ne(PaymentChannelType::getStatus, 0)
                        .orderByAsc(PaymentChannelType::getSortOrder));
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
