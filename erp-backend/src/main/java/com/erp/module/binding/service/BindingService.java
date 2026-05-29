package com.erp.module.binding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.binding.dto.BindingReqDTO;
import com.erp.module.binding.dto.BindingRespDTO;

import java.util.List;

public interface BindingService {
    IPage<BindingRespDTO> listAll(Long salesAccountId, Long customerId, Integer page, Integer pageSize);
    void create(BindingReqDTO req, Long operatorId);
    void createSelfBinding(BindingReqDTO req);
    void unbind(Long id, Long operatorId);
    List<BindingRespDTO> listBoundCustomersByAccount(Long salesAccountId);
    List<BindingRespDTO> listBoundAccountsByCustomer(Long customerId);
}
