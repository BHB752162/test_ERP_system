package com.erp.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.customer.dto.CustomerContactReqDTO;
import com.erp.module.customer.dto.CustomerReqDTO;
import com.erp.module.customer.dto.PaymentChannelReqDTO;
import com.erp.module.customer.dto.ShippingAddressReqDTO;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.entity.CustomerContact;
import com.erp.module.customer.entity.CustomerPaymentChannel;
import com.erp.module.customer.entity.CustomerShippingAddress;
import com.erp.module.customer.mapper.CustomerContactMapper;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.customer.mapper.CustomerPaymentChannelMapper;
import com.erp.module.customer.mapper.CustomerShippingAddressMapper;
import com.erp.module.customer.service.CustomerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private CustomerPaymentChannelMapper paymentChannelMapper;

    @Resource
    private CustomerContactMapper customerContactMapper;

    @Resource
    private CustomerShippingAddressMapper shippingAddressMapper;

    @Override
    public IPage<Customer> listCustomers(int page, int pageSize, String keyword) {
        Page<Customer> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Customer::getCustomerName, keyword)
                    .or().like(Customer::getPhone, keyword);
        }
        wrapper.ne(Customer::getStatus, 0).orderByDesc(Customer::getCreatedAt);
        return customerMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BusinessException("客户不存在");
        return customer;
    }

    @Override
    public void create(CustomerReqDTO req, Long createdBy) {
        Customer customer = new Customer();
        BeanUtils.copyProperties(req, customer);
        customer.setCreatedBy(createdBy);
        if (customer.getStatus() == null) customer.setStatus(1);
        if (customer.getLevel() == null) customer.setLevel(0);
        customerMapper.insert(customer);
    }

    @Override
    public void update(Long id, CustomerReqDTO req) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BusinessException("客户不存在");
        BeanUtils.copyProperties(req, customer);
        customerMapper.updateById(customer);
    }

    @Override
    public void delete(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BusinessException("客户不存在");
        customer.setStatus(0);
        customerMapper.updateById(customer);
    }

    @Override
    public List<CustomerPaymentChannel> listChannels(Long customerId) {
        return paymentChannelMapper.selectList(
                new LambdaQueryWrapper<CustomerPaymentChannel>()
                        .eq(CustomerPaymentChannel::getCustomerId, customerId)
                        .ne(CustomerPaymentChannel::getStatus, 0)
                        .orderByDesc(CustomerPaymentChannel::getIsDefault));
    }

    @Override
    public void createChannel(PaymentChannelReqDTO req) {
        CustomerPaymentChannel channel = new CustomerPaymentChannel();
        BeanUtils.copyProperties(req, channel);
        if (channel.getStatus() == null) channel.setStatus(1);
        paymentChannelMapper.insert(channel);
    }

    @Override
    public void updateChannel(Long id, PaymentChannelReqDTO req) {
        CustomerPaymentChannel channel = paymentChannelMapper.selectById(id);
        if (channel == null) throw new BusinessException("付款渠道不存在");
        BeanUtils.copyProperties(req, channel);
        paymentChannelMapper.updateById(channel);
    }

    @Override
    public void deleteChannel(Long id) {
        CustomerPaymentChannel channel = paymentChannelMapper.selectById(id);
        if (channel == null) throw new BusinessException("付款渠道不存在");
        channel.setStatus(0);
        paymentChannelMapper.updateById(channel);
    }

    @Override
    public List<CustomerContact> listContacts(Long customerId) {
        return customerContactMapper.selectList(
                new LambdaQueryWrapper<CustomerContact>()
                        .eq(CustomerContact::getCustomerId, customerId)
                        .orderByDesc(CustomerContact::getIsPrimary));
    }

    @Override
    public void createContact(CustomerContactReqDTO req) {
        CustomerContact contact = new CustomerContact();
        BeanUtils.copyProperties(req, contact);
        customerContactMapper.insert(contact);
    }

    @Override
    public void updateContact(Long id, CustomerContactReqDTO req) {
        CustomerContact contact = customerContactMapper.selectById(id);
        if (contact == null) throw new BusinessException("联系人不存在");
        BeanUtils.copyProperties(req, contact);
        customerContactMapper.updateById(contact);
    }

    @Override
    public void deleteContact(Long id) {
        customerContactMapper.deleteById(id);
    }

    @Override
    public List<CustomerShippingAddress> listShippingAddresses(Long customerId) {
        return shippingAddressMapper.selectList(
                new LambdaQueryWrapper<CustomerShippingAddress>()
                        .eq(CustomerShippingAddress::getCustomerId, customerId)
                        .orderByDesc(CustomerShippingAddress::getIsDefault)
                        .orderByDesc(CustomerShippingAddress::getCreatedAt));
    }

    @Override
    public void createShippingAddress(ShippingAddressReqDTO req) {
        CustomerShippingAddress address = new CustomerShippingAddress();
        BeanUtils.copyProperties(req, address);
        if (address.getStatus() == null) address.setStatus(1);
        // If set as default, clear other defaults
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(req.getCustomerId());
        }
        shippingAddressMapper.insert(address);
    }

    @Override
    public void updateShippingAddress(Long id, ShippingAddressReqDTO req) {
        CustomerShippingAddress address = shippingAddressMapper.selectById(id);
        if (address == null) throw new BusinessException("收件地址不存在");
        BeanUtils.copyProperties(req, address);
        address.setId(id);
        // If set as default, clear other defaults
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(req.getCustomerId());
        }
        shippingAddressMapper.updateById(address);
    }

    @Override
    public void deleteShippingAddress(Long id) {
        CustomerShippingAddress address = shippingAddressMapper.selectById(id);
        if (address == null) throw new BusinessException("收件地址不存在");
        address.setStatus(0);
        shippingAddressMapper.updateById(address);
    }

    private void clearDefaultAddress(Long customerId) {
        CustomerShippingAddress addr = shippingAddressMapper.selectOne(
                new LambdaQueryWrapper<CustomerShippingAddress>()
                        .eq(CustomerShippingAddress::getCustomerId, customerId)
                        .eq(CustomerShippingAddress::getIsDefault, 1));
        if (addr != null) {
            addr.setIsDefault(0);
            shippingAddressMapper.updateById(addr);
        }
    }
}
