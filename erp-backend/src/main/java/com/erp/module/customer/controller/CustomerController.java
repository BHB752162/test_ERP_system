package com.erp.module.customer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResult;
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
import com.erp.module.customer.service.CustomerService;
import com.erp.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Resource
    private CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResult<Customer>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            CustomerQueryDTO query) {
        Long userId = SecurityUtils.getCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        IPage<Customer> result = customerService.listCustomers(page, pageSize, query, userId, roleCode);
        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> getById(@PathVariable Long id) {
        return ApiResponse.success(customerService.getById(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody CustomerReqDTO req) {
        Long id = customerService.create(req, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(id);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerReqDTO req) {
        customerService.update(id, req, SecurityUtils.getCurrentUserId());
        return ApiResponse.success();
    }

    @GetMapping("/{id}/audit-logs")
    public ApiResponse<List<CustomerAuditLog>> listAuditLogs(@PathVariable Long id) {
        return ApiResponse.success(customerService.listAuditLogs(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success();
    }

    // Payment Channels
    @GetMapping("/{customerId}/payment-channels")
    public ApiResponse<List<CustomerPaymentChannel>> listChannels(@PathVariable Long customerId) {
        return ApiResponse.success(customerService.listChannels(customerId));
    }

    @PostMapping("/{customerId}/payment-channels")
    public ApiResponse<Void> createChannel(@PathVariable Long customerId,
                                           @Valid @RequestBody PaymentChannelReqDTO req) {
        req.setCustomerId(customerId);
        customerService.createChannel(req);
        return ApiResponse.success();
    }

    @PutMapping("/payment-channels/{id}")
    public ApiResponse<Void> updateChannel(@PathVariable Long id,
                                           @Valid @RequestBody PaymentChannelReqDTO req) {
        customerService.updateChannel(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/payment-channels/{id}")
    public ApiResponse<Void> deleteChannel(@PathVariable Long id) {
        customerService.deleteChannel(id);
        return ApiResponse.success();
    }

    // Contacts
    @GetMapping("/{customerId}/contacts")
    public ApiResponse<List<CustomerContact>> listContacts(@PathVariable Long customerId) {
        return ApiResponse.success(customerService.listContacts(customerId));
    }

    @PostMapping("/{customerId}/contacts")
    public ApiResponse<Void> createContact(@PathVariable Long customerId,
                                           @Valid @RequestBody CustomerContactReqDTO req) {
        req.setCustomerId(customerId);
        customerService.createContact(req);
        return ApiResponse.success();
    }

    @PutMapping("/contacts/{id}")
    public ApiResponse<Void> updateContact(@PathVariable Long id,
                                           @Valid @RequestBody CustomerContactReqDTO req) {
        customerService.updateContact(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/contacts/{id}")
    public ApiResponse<Void> deleteContact(@PathVariable Long id) {
        customerService.deleteContact(id);
        return ApiResponse.success();
    }

    // Shipping addresses
    @GetMapping("/{customerId}/shipping-addresses")
    public ApiResponse<List<CustomerShippingAddress>> listShippingAddresses(@PathVariable Long customerId) {
        return ApiResponse.success(customerService.listShippingAddresses(customerId));
    }

    @PostMapping("/{customerId}/shipping-addresses")
    public ApiResponse<Void> createShippingAddress(@PathVariable Long customerId,
                                                    @Valid @RequestBody ShippingAddressReqDTO req) {
        req.setCustomerId(customerId);
        customerService.createShippingAddress(req);
        return ApiResponse.success();
    }

    @PutMapping("/shipping-addresses/{id}")
    public ApiResponse<Void> updateShippingAddress(@PathVariable Long id,
                                                    @Valid @RequestBody ShippingAddressReqDTO req) {
        customerService.updateShippingAddress(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/shipping-addresses/{id}")
    public ApiResponse<Void> deleteShippingAddress(@PathVariable Long id) {
        customerService.deleteShippingAddress(id);
        return ApiResponse.success();
    }
}
