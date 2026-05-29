package com.erp.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.binding.entity.CustomerSalesAccountBinding;
import com.erp.security.SecurityUtils;
import com.erp.module.binding.mapper.CustomerSalesAccountBindingMapper;
import com.erp.module.customer.dto.CustomerContactReqDTO;
import com.erp.module.customer.dto.CustomerQueryDTO;
import com.erp.module.customer.dto.CustomerReqDTO;
import com.erp.module.customer.dto.PaymentChannelReqDTO;
import com.erp.module.customer.dto.ShippingAddressReqDTO;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.entity.CustomerAuditLog;
import com.erp.module.customer.mapper.CustomerAuditLogMapper;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.module.customer.entity.CustomerContact;
import com.erp.module.customer.entity.CustomerPaymentChannel;
import com.erp.module.customer.entity.CustomerShippingAddress;
import com.erp.module.customer.mapper.CustomerContactMapper;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.customer.mapper.CustomerPaymentChannelMapper;
import com.erp.module.customer.mapper.CustomerShippingAddressMapper;
import com.erp.module.customer.service.CustomerService;
import com.erp.module.order.mapper.SalesOrderMapper;
import com.erp.module.salesaccount.entity.SalesAccount;
import com.erp.module.salesaccount.entity.SalesAccountUserBinding;
import com.erp.module.salesaccount.mapper.SalesAccountMapper;
import com.erp.module.salesaccount.mapper.SalesAccountUserBindingMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    private SalesAccountMapper salesAccountMapper;

    @Resource
    private SalesAccountUserBindingMapper salesAccountUserBindingMapper;

    @Resource
    private CustomerSalesAccountBindingMapper customerSalesAccountBindingMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private CustomerAuditLogMapper customerAuditLogMapper;

    @Override
    public IPage<Customer> listCustomers(int page, int pageSize, CustomerQueryDTO query, Long userId, String roleCode) {
        Page<Customer> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        // 客户名称
        if (StringUtils.isNotBlank(query.getCustomerName())) {
            wrapper.like(Customer::getCustomerName, query.getCustomerName());
        }
        // 电话
        if (StringUtils.isNotBlank(query.getPhone())) {
            wrapper.like(Customer::getPhone, query.getPhone());
        }
        // 微信号
        if (StringUtils.isNotBlank(query.getWechatAccount())) {
            wrapper.like(Customer::getWechatAccount, query.getWechatAccount());
        }
        // 创建人员 → 跨表查 sys_user
        if (StringUtils.isNotBlank(query.getCreatedByName())) {
            List<Long> creatorIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, query.getCreatedByName()))
                    .stream().map(SysUser::getId)
                    .collect(Collectors.toList());
            if (creatorIds.isEmpty()) {
                wrapper.eq(Customer::getId, -1L);
            } else {
                wrapper.in(Customer::getCreatedBy, creatorIds);
            }
        }
        // 绑定销售账户 → 先模糊查销售账户名，再查绑定关系
        if (StringUtils.isNotBlank(query.getSalesAccountName())) {
            List<Long> accountIds = salesAccountMapper.selectList(
                    new LambdaQueryWrapper<SalesAccount>()
                            .like(SalesAccount::getAccountName, query.getSalesAccountName())
                            .or().like(SalesAccount::getDisplayName, query.getSalesAccountName()))
                    .stream().map(SalesAccount::getId)
                    .collect(Collectors.toList());
            if (accountIds.isEmpty()) {
                wrapper.eq(Customer::getId, -1L);
            } else {
                List<Long> boundCustomerIds = customerSalesAccountBindingMapper.selectList(
                        new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                                .in(CustomerSalesAccountBinding::getSalesAccountId, accountIds))
                        .stream().map(CustomerSalesAccountBinding::getCustomerId)
                        .distinct()
                        .collect(Collectors.toList());
                if (boundCustomerIds.isEmpty()) {
                    wrapper.eq(Customer::getId, -1L);
                } else {
                    wrapper.in(Customer::getId, boundCustomerIds);
                }
            }
        }

        // 销售只能看到其销售账户绑定的顾客
        if (!"ADMIN".equals(roleCode)) {
            List<Long> accountIds = salesAccountUserBindingMapper.selectList(
                    new LambdaQueryWrapper<SalesAccountUserBinding>()
                            .eq(SalesAccountUserBinding::getUserId, userId))
                    .stream().map(SalesAccountUserBinding::getSalesAccountId)
                    .collect(Collectors.toList());

            if (accountIds.isEmpty()) {
                wrapper.eq(Customer::getId, -1L);
            } else {
                List<Long> customerIds = customerSalesAccountBindingMapper.selectList(
                        new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                                .in(CustomerSalesAccountBinding::getSalesAccountId, accountIds))
                        .stream().map(CustomerSalesAccountBinding::getCustomerId)
                        .distinct()
                        .collect(Collectors.toList());

                if (customerIds.isEmpty()) {
                    wrapper.eq(Customer::getId, -1L);
                } else {
                    wrapper.in(Customer::getId, customerIds);
                }
            }
        }

        wrapper.ne(Customer::getStatus, 0).orderByDesc(Customer::getCreatedAt);
        IPage<Customer> result = customerMapper.selectPage(pageParam, wrapper);

        // 批量加载创建人姓名
        Set<Long> userIds = result.getRecords().stream()
                .map(Customer::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            Map<Long, String> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            result.getRecords().forEach(c -> c.setCreatedByName(userMap.get(c.getCreatedBy())));
        }

        // 批量加载订单数量
        List<Long> customerIds = result.getRecords().stream()
                .map(Customer::getId)
                .collect(Collectors.toList());
        if (!customerIds.isEmpty()) {
            Map<Long, Long> orderCountMap = salesOrderMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.module.order.entity.SalesOrder>()
                            .select("customer_id, count(*) as cnt")
                            .in("customer_id", customerIds)
                            .groupBy("customer_id"))
                    .stream()
                    .collect(Collectors.toMap(
                            m -> (Long) m.get("customer_id"),
                            m -> (Long) m.get("cnt")));
            result.getRecords().forEach(c -> {
                Long cnt = orderCountMap.get(c.getId());
                c.setOrderCount(cnt != null ? cnt.intValue() : 0);
            });
        }

        return result;
    }

    @Override
    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BusinessException("客户不存在");
        return customer;
    }

    @Override
    @Transactional
    public Long create(CustomerReqDTO req, Long createdBy) {
        if (createdBy == null) throw new BusinessException("无法获取当前用户信息");
        if (StringUtils.isNotBlank(req.getWechatAccount())) {
            Long count = customerMapper.selectCount(
                    new LambdaQueryWrapper<Customer>()
                            .eq(Customer::getWechatAccount, req.getWechatAccount())
                            .ne(Customer::getStatus, 0));
            if (count > 0) throw new BusinessException("该微信号已被其他顾客使用: " + req.getWechatAccount());
        }
        Customer customer = new Customer();
        BeanUtils.copyProperties(req, customer);
        customer.setCreatedBy(createdBy);
        if (customer.getStatus() == null) customer.setStatus(1);
        if (customer.getLevel() == null) customer.setLevel(0);
        setAddFriendTime(customer, req.getAddFriendTime());
        try {
            customerMapper.insert(customer);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该微信号已被其他顾客使用: " + req.getWechatAccount());
        }

        // 记录创建日志
        CustomerAuditLog log = new CustomerAuditLog();
        log.setCustomerId(customer.getId());
        log.setAction("CREATE");
        log.setFieldName("全部字段");
        log.setNewValue(customer.getCustomerName());
        log.setOperatorId(createdBy);
        customerAuditLogMapper.insert(log);

        return customer.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CustomerReqDTO req, Long operatorId) {
        Customer old = customerMapper.selectById(id);
        if (old == null) throw new BusinessException("客户不存在");

        // 复制旧值用于对比
        String oldName = old.getCustomerName();
        String oldPhone = old.getPhone();
        String oldWechat = old.getWechatAccount();
        Integer oldLevel = old.getLevel();
        Integer oldStatus = old.getStatus();
        String oldRemark = old.getRemark();
        LocalDateTime oldAddFriendTime = old.getAddFriendTime();

        // 微信号：非管理员每年仅可修改一次
        if (!SecurityUtils.hasRole("ADMIN") && !Objects.equals(oldWechat, req.getWechatAccount())) {
            CustomerAuditLog lastWechatLog = customerAuditLogMapper.selectOne(
                    new LambdaQueryWrapper<CustomerAuditLog>()
                            .eq(CustomerAuditLog::getCustomerId, id)
                            .eq(CustomerAuditLog::getFieldName, "微信号")
                            .eq(CustomerAuditLog::getAction, "UPDATE")
                            .orderByDesc(CustomerAuditLog::getOperatedAt)
                            .last("LIMIT 1"));
            if (lastWechatLog != null && lastWechatLog.getOperatedAt() != null
                    && lastWechatLog.getOperatedAt().plusDays(365).isAfter(LocalDateTime.now())) {
                throw new BusinessException("微信号每年仅可修改一次，上次修改时间：" + lastWechatLog.getOperatedAt().toLocalDate());
            }
        }

        // 微信号唯一性校验：修改后不能与其他顾客的微信号重复
        if (StringUtils.isNotBlank(req.getWechatAccount())
                && !req.getWechatAccount().equals(oldWechat)) {
            Long dupCount = customerMapper.selectCount(
                    new LambdaQueryWrapper<Customer>()
                            .eq(Customer::getWechatAccount, req.getWechatAccount())
                            .ne(Customer::getStatus, 0)
                            .ne(Customer::getId, id));
            if (dupCount > 0) throw new BusinessException("该微信号已被其他顾客使用: " + req.getWechatAccount());
        }

        BeanUtils.copyProperties(req, old);
        setAddFriendTime(old, req.getAddFriendTime());
        customerMapper.updateById(old);

        // 记录变更日志
        compareAndLog(id, "客户名称", oldName, old.getCustomerName(), operatorId);
        compareAndLog(id, "联系电话", oldPhone, old.getPhone(), operatorId);
        compareAndLog(id, "微信号", oldWechat, old.getWechatAccount(), operatorId);
        String levelMap = "0:普通,1:银卡,2:金卡,3:钻石";
        compareAndLog(id, "等级", oldLevel != null ? oldLevel.toString() : null, old.getLevel() != null ? old.getLevel().toString() : null, operatorId);
        compareAndLog(id, "状态", oldStatus != null ? (oldStatus == 1 ? "启用" : "停用") : null, old.getStatus() != null ? (old.getStatus() == 1 ? "启用" : "停用") : null, operatorId);
        compareAndLog(id, "加粉时间", oldAddFriendTime != null ? oldAddFriendTime.toLocalDate().toString() : null, old.getAddFriendTime() != null ? old.getAddFriendTime().toLocalDate().toString() : null, operatorId);
        compareAndLog(id, "备注", oldRemark, old.getRemark(), operatorId);
    }

    private void compareAndLog(Long customerId, String fieldName, String oldVal, String newVal, Long operatorId) {
        if (Objects.equals(oldVal, newVal)) return;
        if (oldVal == null && newVal == null) return;
        CustomerAuditLog log = new CustomerAuditLog();
        log.setCustomerId(customerId);
        log.setAction("UPDATE");
        log.setFieldName(fieldName);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setOperatorId(operatorId);
        customerAuditLogMapper.insert(log);
    }

    @Override
    public List<CustomerAuditLog> listAuditLogs(Long customerId) {
        List<CustomerAuditLog> logs = customerAuditLogMapper.selectList(
                new LambdaQueryWrapper<CustomerAuditLog>()
                        .eq(CustomerAuditLog::getCustomerId, customerId)
                        .orderByDesc(CustomerAuditLog::getOperatedAt));
        // 批量加载操作人姓名
        Set<Long> userIds = logs.stream().map(CustomerAuditLog::getOperatorId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            Map<Long, String> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            logs.forEach(l -> l.setOperatorName(userMap.get(l.getOperatorId())));
        }
        return logs;
    }

    private void setAddFriendTime(Customer customer, String addFriendTimeStr) {
        if (StringUtils.isNotBlank(addFriendTimeStr)) {
            customer.setAddFriendTime(LocalDateTime.parse(addFriendTimeStr + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            customer.setAddFriendTime(null);
        }
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
