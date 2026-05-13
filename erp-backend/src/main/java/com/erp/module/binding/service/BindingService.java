package com.erp.module.binding.service;

import com.erp.module.binding.dto.BindingReqDTO;
import com.erp.module.binding.dto.BindingRespDTO;

import java.util.List;

public interface BindingService {
    List<BindingRespDTO> listAll(String keyword);
    void create(BindingReqDTO req);
    void unbind(Long id);
    List<BindingRespDTO> listBoundCustomersByWechat(Long wechatId);
}
