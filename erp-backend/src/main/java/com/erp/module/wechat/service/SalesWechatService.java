package com.erp.module.wechat.service;

import com.erp.module.wechat.dto.WechatReqDTO;
import com.erp.module.wechat.dto.WechatRespDTO;

import java.util.List;

public interface SalesWechatService {
    List<WechatRespDTO> listBySalesPerson(Long salesPersonId);
    List<WechatRespDTO> listMyWechats(Long currentUserId);
    WechatRespDTO getById(Long id);
    void create(WechatReqDTO req);
    void update(Long id, WechatReqDTO req);
    void delete(Long id);
}
