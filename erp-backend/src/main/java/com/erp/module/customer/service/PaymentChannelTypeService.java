package com.erp.module.customer.service;

import com.erp.module.customer.entity.PaymentChannelType;

import java.util.List;

public interface PaymentChannelTypeService {
    List<PaymentChannelType> listAll();
    PaymentChannelType getById(Long id);
    void create(PaymentChannelType req);
    void update(Long id, PaymentChannelType req);
    void delete(Long id);
}
