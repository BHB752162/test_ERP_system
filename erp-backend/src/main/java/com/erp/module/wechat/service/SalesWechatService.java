package com.erp.module.wechat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.wechat.dto.WechatReqDTO;
import com.erp.module.wechat.dto.WechatRespDTO;

import java.util.List;

public interface SalesWechatService {
    IPage<WechatRespDTO> listAll(String keyword, Integer page, Integer pageSize);
    List<WechatRespDTO> listBySalesPerson(Long salesPersonId);
    List<WechatRespDTO> listMyWechats(Long currentUserId);
    WechatRespDTO getById(Long id);
    void create(WechatReqDTO req);
    void update(Long id, WechatReqDTO req);
    void delete(Long id);
}
