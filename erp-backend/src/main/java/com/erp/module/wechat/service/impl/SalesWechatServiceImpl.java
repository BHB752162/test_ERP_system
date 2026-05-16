package com.erp.module.wechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.module.wechat.dto.WechatReqDTO;
import com.erp.module.wechat.dto.WechatRespDTO;
import com.erp.module.wechat.entity.SalesWechat;
import com.erp.module.wechat.mapper.SalesWechatMapper;
import com.erp.module.wechat.service.SalesWechatService;
import com.erp.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SalesWechatServiceImpl implements SalesWechatService {

    @Resource
    private SalesWechatMapper salesWechatMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public List<WechatRespDTO> listBySalesPerson(Long salesPersonId) {
        List<SalesWechat> list = salesWechatMapper.selectList(
                new LambdaQueryWrapper<SalesWechat>()
                        .eq(SalesWechat::getSalesPersonId, salesPersonId)
                        .ne(SalesWechat::getStatus, 0)
                        .orderByDesc(SalesWechat::getCreatedAt));
        return convertList(list);
    }

    @Override
    public List<WechatRespDTO> listMyWechats(Long currentUserId) {
        return listBySalesPerson(currentUserId);
    }

    @Override
    public IPage<WechatRespDTO> listAll(String keyword, Integer page, Integer pageSize) {
        LambdaQueryWrapper<SalesWechat> wrapper = new LambdaQueryWrapper<SalesWechat>()
                .ne(SalesWechat::getStatus, 0)
                .orderByDesc(SalesWechat::getCreatedAt);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SalesWechat::getWechatAccount, keyword)
                    .or().like(SalesWechat::getWechatNickname, keyword));
        }
        IPage<SalesWechat> pageResult = salesWechatMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<WechatRespDTO> dtoList = convertList(pageResult.getRecords());
        IPage<WechatRespDTO> result = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        result.setRecords(dtoList);
        return result;
    }

    @Override
    public WechatRespDTO getById(Long id) {
        SalesWechat wechat = salesWechatMapper.selectById(id);
        if (wechat == null) throw new BusinessException("微信号不存在");
        return toRespDTO(wechat);
    }

    @Override
    public void create(WechatReqDTO req) {
        SalesWechat wechat = new SalesWechat();
        BeanUtils.copyProperties(req, wechat);
        if (wechat.getStatus() == null) wechat.setStatus(1);
        Long userId = SecurityUtils.getCurrentUserId();
        wechat.setCreatedBy(userId);
        wechat.setUpdatedBy(userId);
        salesWechatMapper.insert(wechat);
    }

    @Override
    public void update(Long id, WechatReqDTO req) {
        SalesWechat wechat = salesWechatMapper.selectById(id);
        if (wechat == null) throw new BusinessException("微信号不存在");
        BeanUtils.copyProperties(req, wechat);
        wechat.setUpdatedBy(SecurityUtils.getCurrentUserId());
        salesWechatMapper.updateById(wechat);
    }

    @Override
    public void delete(Long id) {
        SalesWechat wechat = salesWechatMapper.selectById(id);
        if (wechat == null) throw new BusinessException("微信号不存在");
        wechat.setStatus(0);
        salesWechatMapper.updateById(wechat);
    }

    private WechatRespDTO toRespDTO(SalesWechat wechat) {
        WechatRespDTO dto = new WechatRespDTO();
        BeanUtils.copyProperties(wechat, dto);
        if (wechat.getSalesPersonId() != null) {
            SysUser user = sysUserMapper.selectById(wechat.getSalesPersonId());
            if (user != null) dto.setSalesPersonName(user.getRealName());
        }
        if (wechat.getCreatedBy() != null) {
            SysUser user = sysUserMapper.selectById(wechat.getCreatedBy());
            if (user != null) dto.setCreatedByName(user.getRealName());
        }
        if (wechat.getUpdatedBy() != null) {
            SysUser user = sysUserMapper.selectById(wechat.getUpdatedBy());
            if (user != null) dto.setUpdatedByName(user.getRealName());
        }
        return dto;
    }

    private List<WechatRespDTO> convertList(List<SalesWechat> list) {
        Set<Long> userIds = list.stream()
                .flatMap(w -> Stream.of(w.getSalesPersonId(), w.getCreatedBy(), w.getUpdatedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        return list.stream().map(w -> {
            WechatRespDTO dto = new WechatRespDTO();
            BeanUtils.copyProperties(w, dto);
            dto.setSalesPersonName(userMap.get(w.getSalesPersonId()));
            dto.setCreatedByName(userMap.get(w.getCreatedBy()));
            dto.setUpdatedByName(userMap.get(w.getUpdatedBy()));
            return dto;
        }).collect(Collectors.toList());
    }
}
