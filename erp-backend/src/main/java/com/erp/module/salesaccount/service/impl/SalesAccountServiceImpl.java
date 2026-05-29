package com.erp.module.salesaccount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.module.salesaccount.dto.SalesAccountReqDTO;
import com.erp.module.salesaccount.dto.SalesAccountRespDTO;
import com.erp.module.salesaccount.entity.SalesAccount;
import com.erp.module.salesaccount.entity.SalesAccountUserBinding;
import com.erp.module.salesaccount.mapper.SalesAccountMapper;
import com.erp.module.salesaccount.mapper.SalesAccountUserBindingMapper;
import com.erp.module.salesaccount.service.SalesAccountService;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SalesAccountServiceImpl implements SalesAccountService {

    @Resource
    private SalesAccountMapper salesAccountMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SalesAccountUserBindingMapper salesAccountUserBindingMapper;

    @Override
    public List<SalesAccountRespDTO> listAll(String keyword, String accountName, String displayName) {
        LambdaQueryWrapper<SalesAccount> wrapper = new LambdaQueryWrapper<SalesAccount>()
                .ne(SalesAccount::getStatus, 0)
                .orderByDesc(SalesAccount::getCreatedAt);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SalesAccount::getAccountName, keyword)
                    .or().like(SalesAccount::getDisplayName, keyword));
        }
        if (StringUtils.isNotBlank(accountName)) {
            wrapper.like(SalesAccount::getAccountName, accountName);
        }
        if (StringUtils.isNotBlank(displayName)) {
            wrapper.like(SalesAccount::getDisplayName, displayName);
        }
        List<SalesAccount> list = salesAccountMapper.selectList(wrapper);
        return convertList(list);
    }

    @Override
    public SalesAccountRespDTO getById(Long id) {
        SalesAccount account = salesAccountMapper.selectById(id);
        if (account == null) throw new BusinessException("销售账户不存在");
        return toRespDTO(account);
    }

    @Override
    @Transactional
    public Long create(SalesAccountReqDTO req) {
        if (StringUtils.isNotBlank(req.getAccountName())) {
            Long count = salesAccountMapper.selectCount(
                    new LambdaQueryWrapper<SalesAccount>()
                            .eq(SalesAccount::getAccountName, req.getAccountName())
                            .ne(SalesAccount::getStatus, 0));
            if (count > 0) throw new BusinessException("销售账户名已存在: " + req.getAccountName());
        }
        SalesAccount account = new SalesAccount();
        BeanUtils.copyProperties(req, account);
        if (account.getStatus() == null) account.setStatus(1);
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException("无法获取当前用户信息");
        account.setCreatedBy(userId);
        account.setUpdatedBy(userId);
        try {
            salesAccountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("销售账户名已存在: " + req.getAccountName());
        }
        return account.getId();
    }

    @Override
    public void update(Long id, SalesAccountReqDTO req) {
        SalesAccount account = salesAccountMapper.selectById(id);
        if (account == null) throw new BusinessException("销售账户不存在");
        if (StringUtils.isNotBlank(req.getAccountName())
                && !req.getAccountName().equals(account.getAccountName())) {
            Long count = salesAccountMapper.selectCount(
                    new LambdaQueryWrapper<SalesAccount>()
                            .eq(SalesAccount::getAccountName, req.getAccountName())
                            .ne(SalesAccount::getStatus, 0));
            if (count > 0) throw new BusinessException("销售账户名已存在: " + req.getAccountName());
        }
        BeanUtils.copyProperties(req, account);
        account.setUpdatedBy(SecurityUtils.getCurrentUserId());
        salesAccountMapper.updateById(account);
    }

    @Override
    public void delete(Long id) {
        SalesAccount account = salesAccountMapper.selectById(id);
        if (account == null) throw new BusinessException("销售账户不存在");
        account.setStatus(0);
        salesAccountMapper.updateById(account);
    }

    private SalesAccountRespDTO toRespDTO(SalesAccount account) {
        SalesAccountRespDTO dto = new SalesAccountRespDTO();
        BeanUtils.copyProperties(account, dto);
        if (account.getCreatedBy() != null) {
            SysUser user = sysUserMapper.selectById(account.getCreatedBy());
            if (user != null) dto.setCreatedByName(user.getRealName());
        }
        if (account.getUpdatedBy() != null) {
            SysUser user = sysUserMapper.selectById(account.getUpdatedBy());
            if (user != null) dto.setUpdatedByName(user.getRealName());
        }
        return dto;
    }

    @Override
    public List<Long> getBoundUserIds(Long accountId) {
        return salesAccountUserBindingMapper.selectList(
                new LambdaQueryWrapper<SalesAccountUserBinding>()
                        .eq(SalesAccountUserBinding::getSalesAccountId, accountId))
                .stream().map(SalesAccountUserBinding::getUserId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void bindUser(Long accountId, Long userId) {
        SalesAccount account = salesAccountMapper.selectById(accountId);
        if (account == null) throw new BusinessException("销售账户不存在");
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        Long count = salesAccountUserBindingMapper.selectCount(
                new LambdaQueryWrapper<SalesAccountUserBinding>()
                        .eq(SalesAccountUserBinding::getSalesAccountId, accountId)
                        .eq(SalesAccountUserBinding::getUserId, userId));
        if (count > 0) throw new BusinessException("该用户已绑定此账户");

        SalesAccountUserBinding binding = new SalesAccountUserBinding();
        binding.setSalesAccountId(accountId);
        binding.setUserId(userId);
        salesAccountUserBindingMapper.insert(binding);
    }

    @Override
    @Transactional
    public void unbindUser(Long accountId, Long userId) {
        salesAccountUserBindingMapper.delete(
                new LambdaQueryWrapper<SalesAccountUserBinding>()
                        .eq(SalesAccountUserBinding::getSalesAccountId, accountId)
                        .eq(SalesAccountUserBinding::getUserId, userId));
    }

    @Override
    public List<SalesAccountRespDTO> listMyAccounts(Long userId) {
        List<SalesAccountUserBinding> bindings = salesAccountUserBindingMapper.selectList(
                new LambdaQueryWrapper<SalesAccountUserBinding>()
                        .eq(SalesAccountUserBinding::getUserId, userId));
        if (bindings.isEmpty()) return Collections.emptyList();
        List<Long> accountIds = bindings.stream()
                .map(SalesAccountUserBinding::getSalesAccountId)
                .collect(Collectors.toList());
        List<SalesAccount> accounts = salesAccountMapper.selectBatchIds(accountIds);
        return convertList(accounts);
    }

    private List<SalesAccountRespDTO> convertList(List<SalesAccount> list) {
        Set<Long> userIds = list.stream()
                .flatMap(a -> Stream.of(a.getCreatedBy(), a.getUpdatedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        return list.stream().map(a -> {
            SalesAccountRespDTO dto = new SalesAccountRespDTO();
            BeanUtils.copyProperties(a, dto);
            dto.setCreatedByName(userMap.get(a.getCreatedBy()));
            dto.setUpdatedByName(userMap.get(a.getUpdatedBy()));
            return dto;
        }).collect(Collectors.toList());
    }
}
