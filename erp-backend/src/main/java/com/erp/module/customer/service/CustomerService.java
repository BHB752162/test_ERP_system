package com.erp.module.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.customer.dto.CustomerContactReqDTO;
import com.erp.module.customer.dto.CustomerQueryDTO;
import com.erp.module.customer.dto.CustomerReqDTO;
import com.erp.module.customer.dto.PaymentChannelReqDTO;
import com.erp.module.customer.dto.ShippingAddressReqDTO;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.entity.CustomerAuditLog;
import com.erp.module.customer.entity.CustomerContact;
import com.erp.module.customer.entity.CustomerPaymentChannel;
import com.erp.module.customer.entity.CustomerShippingAddress;

import java.util.List;

public interface CustomerService {
    IPage<Customer> listCustomers(int page, int pageSize, CustomerQueryDTO query, Long userId, String roleCode);
    Customer getById(Long id);
    Long create(CustomerReqDTO req, Long createdBy);
    void update(Long id, CustomerReqDTO req, Long operatorId);
    void delete(Long id);

    // Audit logs
    List<CustomerAuditLog> listAuditLogs(Long customerId);

    // Payment channels
    List<CustomerPaymentChannel> listChannels(Long customerId);
    void createChannel(PaymentChannelReqDTO req);
    void updateChannel(Long id, PaymentChannelReqDTO req);
    void deleteChannel(Long id);

    // Contacts
    List<CustomerContact> listContacts(Long customerId);
    void createContact(CustomerContactReqDTO req);
    void updateContact(Long id, CustomerContactReqDTO req);
    void deleteContact(Long id);

    // Shipping addresses
    List<CustomerShippingAddress> listShippingAddresses(Long customerId);
    void createShippingAddress(ShippingAddressReqDTO req);
    void updateShippingAddress(Long id, ShippingAddressReqDTO req);
    void deleteShippingAddress(Long id);
}
