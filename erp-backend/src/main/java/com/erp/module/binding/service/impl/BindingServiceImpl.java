package com.erp.module.binding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.binding.dto.BindingReqDTO;
import com.erp.module.binding.dto.BindingRespDTO;
import com.erp.module.binding.entity.CustomerSalesAccountBinding;
import com.erp.module.binding.mapper.CustomerSalesAccountBindingMapper;
import com.erp.module.binding.service.BindingService;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.salesaccount.entity.SalesAccount;
import com.erp.module.salesaccount.entity.SalesAccountUserBinding;
import com.erp.module.salesaccount.mapper.SalesAccountMapper;
import com.erp.module.salesaccount.mapper.SalesAccountUserBindingMapper;
import com.erp.security.SecurityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BindingServiceImpl implements BindingService {

    @Resource
    private CustomerSalesAccountBindingMapper bindingMapper;

    @Resource
    private SalesAccountMapper salesAccountMapper;

    @Resource
    private SalesAccountUserBindingMapper salesAccountUserBindingMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Override
    public IPage<BindingRespDTO> listAll(Long salesAccountId, Long customerId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<CustomerSalesAccountBinding> wrapper = new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                .orderByDesc(CustomerSalesAccountBinding::getCreatedAt);
        if (salesAccountId != null) {
            wrapper.eq(CustomerSalesAccountBinding::getSalesAccountId, salesAccountId);
        }
        if (customerId != null) {
            wrapper.eq(CustomerSalesAccountBinding::getCustomerId, customerId);
        }
        IPage<CustomerSalesAccountBinding> pageResult = bindingMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return pageResult.convert(this::toRespDTO);
    }

    @Override
    public void create(BindingReqDTO req) {
        // Check duplicate
        Long count = bindingMapper.selectCount(
                new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                        .eq(CustomerSalesAccountBinding::getSalesAccountId, req.getSalesAccountId())
                        .eq(CustomerSalesAccountBinding::getCustomerId, req.getCustomerId()));
        if (count > 0) throw new BusinessException("该顾客已绑定此销售账户");

        // Verify sales account exists
        SalesAccount account = salesAccountMapper.selectById(req.getSalesAccountId());
        if (account == null) throw new BusinessException("销售账户不存在");

        // Verify customer exists
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) throw new BusinessException("顾客不存在");

        CustomerSalesAccountBinding binding = new CustomerSalesAccountBinding();
        binding.setSalesAccountId(req.getSalesAccountId());
        binding.setCustomerId(req.getCustomerId());
        bindingMapper.insert(binding);
    }

    @Override
    public void createSelfBinding(BindingReqDTO req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException("未登录");

        // ADMIN can bind any account; non-ADMIN is restricted to their own accounts
        if (!SecurityUtils.hasRole("ADMIN")) {
            Long bindingCount = salesAccountUserBindingMapper.selectCount(
                    new LambdaQueryWrapper<SalesAccountUserBinding>()
                            .eq(SalesAccountUserBinding::getSalesAccountId, req.getSalesAccountId())
                            .eq(SalesAccountUserBinding::getUserId, currentUserId));
            if (bindingCount == 0) throw new BusinessException("您没有权限操作此销售账户");
        }

        // Delegate to regular create
        create(req);
    }

    @Override
    public void unbind(Long id) {
        if (bindingMapper.deleteById(id) == 0) throw new BusinessException("绑定关系不存在");
    }

    @Override
    public List<BindingRespDTO> listBoundCustomersByAccount(Long salesAccountId) {
        List<CustomerSalesAccountBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                        .eq(CustomerSalesAccountBinding::getSalesAccountId, salesAccountId));
        return bindings.stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    @Override
    public List<BindingRespDTO> listBoundAccountsByCustomer(Long customerId) {
        List<CustomerSalesAccountBinding> bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                        .eq(CustomerSalesAccountBinding::getCustomerId, customerId));
        return bindings.stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    private BindingRespDTO toRespDTO(CustomerSalesAccountBinding binding) {
        BindingRespDTO dto = new BindingRespDTO();
        dto.setId(binding.getId());
        dto.setSalesAccountId(binding.getSalesAccountId());
        dto.setCustomerId(binding.getCustomerId());
        dto.setCreatedAt(binding.getCreatedAt());

        SalesAccount account = salesAccountMapper.selectById(binding.getSalesAccountId());
        if (account != null) {
            dto.setSalesAccountName(account.getAccountName());
            dto.setSalesAccountDisplayName(account.getDisplayName());
        }

        Customer customer = customerMapper.selectById(binding.getCustomerId());
        if (customer != null) {
            dto.setCustomerName(customer.getCustomerName());
        }
        return dto;
    }
}
