package com.erp.module.salesaccount.service;

import com.erp.module.salesaccount.dto.SalesAccountReqDTO;
import com.erp.module.salesaccount.dto.SalesAccountRespDTO;

import java.util.List;

public interface SalesAccountService {
    List<SalesAccountRespDTO> listAll(String keyword);
    SalesAccountRespDTO getById(Long id);
    void create(SalesAccountReqDTO req);
    void update(Long id, SalesAccountReqDTO req);
    void delete(Long id);

    List<Long> getBoundUserIds(Long accountId);
    void bindUser(Long accountId, Long userId);
    void unbindUser(Long accountId, Long userId);
}
