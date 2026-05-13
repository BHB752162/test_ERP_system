package com.erp.module.wechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.module.wechat.dto.WechatReqDTO;
import com.erp.module.wechat.dto.WechatRespDTO;
import com.erp.module.wechat.entity.SalesWechat;
import com.erp.module.wechat.mapper.SalesWechatMapper;
import com.erp.module.wechat.service.SalesWechatService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalesWechatServiceImpl implements SalesWechatService {

    @Resource
    private SalesWechatMapper salesWechatMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public List<WechatRespDTO> listBySalesPerson(Long salesPersonId) {
        return salesWechatMapper.selectList(
                new LambdaQueryWrapper<SalesWechat>()
                        .eq(SalesWechat::getSalesPersonId, salesPersonId)
                        .orderByDesc(SalesWechat::getCreatedAt))
                .stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    @Override
    public List<WechatRespDTO> listMyWechats(Long currentUserId) {
        return listBySalesPerson(currentUserId);
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
        salesWechatMapper.insert(wechat);
    }

    @Override
    public void update(Long id, WechatReqDTO req) {
        SalesWechat wechat = salesWechatMapper.selectById(id);
        if (wechat == null) throw new BusinessException("微信号不存在");
        BeanUtils.copyProperties(req, wechat);
        salesWechatMapper.updateById(wechat);
    }

    @Override
    public void delete(Long id) {
        salesWechatMapper.deleteById(id);
    }

    private WechatRespDTO toRespDTO(SalesWechat wechat) {
        WechatRespDTO dto = new WechatRespDTO();
        BeanUtils.copyProperties(wechat, dto);
        SysUser user = sysUserMapper.selectById(wechat.getSalesPersonId());
        if (user != null) {
            dto.setSalesPersonName(user.getRealName());
        }
        return dto;
    }
}
