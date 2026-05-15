package com.erp.module.binding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.binding.dto.BindingReqDTO;
import com.erp.module.binding.dto.BindingRespDTO;
import com.erp.module.binding.entity.CustomerWechatBinding;
import com.erp.module.binding.mapper.CustomerWechatBindingMapper;
import com.erp.module.binding.service.BindingService;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.wechat.entity.SalesWechat;
import com.erp.module.wechat.mapper.SalesWechatMapper;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BindingServiceImpl implements BindingService {

    @Resource
    private CustomerWechatBindingMapper bindingMapper;

    @Resource
    private SalesWechatMapper salesWechatMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<BindingRespDTO> listAll(Long wechatId, Long customerId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<CustomerWechatBinding> wrapper = new LambdaQueryWrapper<CustomerWechatBinding>()
                .ne(CustomerWechatBinding::getStatus, 0)
                .orderByDesc(CustomerWechatBinding::getCreatedAt);
        if (wechatId != null) {
            wrapper.eq(CustomerWechatBinding::getSalesWechatId, wechatId);
        }
        if (customerId != null) {
            wrapper.eq(CustomerWechatBinding::getCustomerId, customerId);
        }
        IPage<CustomerWechatBinding> pageResult = bindingMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return pageResult.convert(this::toRespDTO);
    }

    @Override
    public void create(BindingReqDTO req) {
        long count = bindingMapper.selectCount(
                new LambdaQueryWrapper<CustomerWechatBinding>()
                        .eq(CustomerWechatBinding::getSalesWechatId, req.getSalesWechatId())
                        .eq(CustomerWechatBinding::getCustomerId, req.getCustomerId())
                        .eq(CustomerWechatBinding::getStatus, 1));
        if (count > 0) throw new BusinessException("该绑定关系已存在");

        CustomerWechatBinding binding = new CustomerWechatBinding();
        binding.setSalesWechatId(req.getSalesWechatId());
        binding.setCustomerId(req.getCustomerId());
        binding.setStatus(1);
        bindingMapper.insert(binding);
    }

    @Override
    public void unbind(Long id) {
        CustomerWechatBinding binding = bindingMapper.selectById(id);
        if (binding == null) throw new BusinessException("绑定关系不存在");
        binding.setStatus(0);
        bindingMapper.updateById(binding);
    }

    @Override
    public List<BindingRespDTO> listBoundCustomersByWechat(Long wechatId) {
        List<CustomerWechatBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<CustomerWechatBinding>()
                        .eq(CustomerWechatBinding::getSalesWechatId, wechatId)
                        .eq(CustomerWechatBinding::getStatus, 1));
        return bindings.stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    @Override
    public List<BindingRespDTO> listBoundWechatsByCustomer(Long customerId) {
        List<CustomerWechatBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<CustomerWechatBinding>()
                        .eq(CustomerWechatBinding::getCustomerId, customerId)
                        .eq(CustomerWechatBinding::getStatus, 1));
        return bindings.stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    private BindingRespDTO toRespDTO(CustomerWechatBinding binding) {
        BindingRespDTO dto = new BindingRespDTO();
        dto.setId(binding.getId());
        dto.setSalesWechatId(binding.getSalesWechatId());
        dto.setCustomerId(binding.getCustomerId());
        dto.setStatus(binding.getStatus());
        dto.setCreatedAt(binding.getCreatedAt());

        SalesWechat wechat = salesWechatMapper.selectById(binding.getSalesWechatId());
        if (wechat != null) {
            dto.setWechatAccount(wechat.getWechatAccount());
            dto.setWechatNickname(wechat.getWechatNickname());
            SysUser user = sysUserMapper.selectById(wechat.getSalesPersonId());
            if (user != null) {
                dto.setSalesPersonName(user.getRealName());
            }
        }

        Customer customer = customerMapper.selectById(binding.getCustomerId());
        if (customer != null) {
            dto.setCustomerName(customer.getCustomerName());
        }
        return dto;
    }
}
