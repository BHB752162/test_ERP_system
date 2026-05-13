package com.erp.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.constant.OrderStatusEnum;
import com.erp.common.exception.BusinessException;
import com.erp.module.audit.entity.OrderAuditLog;
import com.erp.module.audit.mapper.OrderAuditLogMapper;
import com.erp.module.binding.entity.CustomerWechatBinding;
import com.erp.module.binding.mapper.CustomerWechatBindingMapper;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.order.dto.OrderCreateReqDTO;
import com.erp.module.order.dto.OrderRespDTO;
import com.erp.module.order.entity.SalesOrder;
import com.erp.module.order.entity.SalesOrderItem;
import com.erp.module.order.mapper.SalesOrderItemMapper;
import com.erp.module.order.mapper.SalesOrderMapper;
import com.erp.module.order.service.OrderService;
import com.erp.module.product.entity.Product;
import com.erp.module.product.mapper.ProductMapper;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.module.wechat.entity.SalesWechat;
import com.erp.module.wechat.mapper.SalesWechatMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private SalesOrderItemMapper salesOrderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SalesWechatMapper salesWechatMapper;

    @Resource
    private CustomerWechatBindingMapper bindingMapper;

    @Resource
    private OrderAuditLogMapper orderAuditLogMapper;

    private final AtomicInteger dailySeq = new AtomicInteger(0);
    private String currentDatePart = "";

    private synchronized String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        if (!datePart.equals(currentDatePart)) {
            currentDatePart = datePart;
            dailySeq.set(0);
        }
        int seq = dailySeq.incrementAndGet();
        return "ORD" + datePart + String.format("%06d", seq);
    }

    @Override
    public IPage<SalesOrder> listOrders(int page, int pageSize, String status, Long currentUserId, String roleCode) {
        Page<SalesOrder> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();

        if (!"ADMIN".equals(roleCode) && !"SALES_MANAGER".equals(roleCode)) {
            wrapper.eq(SalesOrder::getSalesPersonId, currentUserId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SalesOrder::getStatus, status);
        }
        wrapper.orderByDesc(SalesOrder::getCreatedAt);
        return salesOrderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public OrderRespDTO getOrderDetail(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        return toRespDTO(order);
    }

    @Override
    @Transactional
    public SalesOrder createDraft(OrderCreateReqDTO req, Long salesPersonId) {
        // Validate binding
        long bindingCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<CustomerWechatBinding>()
                        .eq(CustomerWechatBinding::getSalesWechatId, req.getSalesWechatId())
                        .eq(CustomerWechatBinding::getCustomerId, req.getCustomerId())
                        .eq(CustomerWechatBinding::getStatus, 1));
        if (bindingCount == 0) {
            throw new BusinessException("该微信号未绑定此客户，无法下单");
        }

        // Create order
        SalesOrder order = new SalesOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(req.getCustomerId());
        order.setSalesPersonId(salesPersonId);
        order.setSalesWechatId(req.getSalesWechatId());
        order.setStatus("DRAFT");
        order.setRemark(req.getRemark());

        // Calculate amounts from items
        BigDecimal total = BigDecimal.ZERO;
        for (OrderCreateReqDTO.OrderItemReqDTO itemReq : req.getItems()) {
            Product product = productMapper.selectById(itemReq.getProductId());
            if (product == null) throw new BusinessException("产品不存在: " + itemReq.getProductId());
            BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : product.getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        order.setTotalAmount(total);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(total);
        salesOrderMapper.insert(order);

        // Create order items
        for (OrderCreateReqDTO.OrderItemReqDTO itemReq : req.getItems()) {
            Product product = productMapper.selectById(itemReq.getProductId());
            BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : product.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            SalesOrderItem item = new SalesOrderItem();
            item.setOrderId(order.getId());
            item.setProductId(itemReq.getProductId());
            item.setProductName(product.getProductName());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(price);
            item.setSubtotal(subtotal);
            salesOrderItemMapper.insert(item);
        }

        return order;
    }

    @Override
    @Transactional
    public void submitForApproval(Long orderId, Long currentUserId) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"DRAFT".equals(order.getStatus())) throw new BusinessException("只有草稿状态的订单才能提交");
        if (!order.getSalesPersonId().equals(currentUserId)) throw new BusinessException("只能提交自己的订单");

        order.setStatus("PENDING_APPROVAL");
        salesOrderMapper.updateById(order);
        addAuditLog(orderId, "SUBMIT", currentUserId, null);
    }

    @Override
    @Transactional
    public void approveOrder(Long orderId, Long currentUserId, String comment) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING_APPROVAL".equals(order.getStatus())) throw new BusinessException("只有待审批的订单才能通过");

        order.setStatus("APPROVED");
        salesOrderMapper.updateById(order);
        addAuditLog(orderId, "APPROVE", currentUserId, comment);
    }

    @Override
    @Transactional
    public void rejectOrder(Long orderId, Long currentUserId, String comment) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING_APPROVAL".equals(order.getStatus())) throw new BusinessException("只有待审批的订单才能驳回");

        order.setStatus("REJECTED");
        salesOrderMapper.updateById(order);
        addAuditLog(orderId, "REJECT", currentUserId, comment);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long currentUserId) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"DRAFT".equals(order.getStatus()) && !"PENDING_APPROVAL".equals(order.getStatus())) {
            throw new BusinessException("只有草稿或待审批的订单才能取消");
        }

        order.setStatus("CANCELLED");
        salesOrderMapper.updateById(order);
        addAuditLog(orderId, "CANCEL", currentUserId, null);
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId, Long currentUserId) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"APPROVED".equals(order.getStatus())) throw new BusinessException("只有已通过的订单才能完成");

        order.setStatus("COMPLETED");
        salesOrderMapper.updateById(order);
        addAuditLog(orderId, "COMPLETE", currentUserId, null);
    }

    private void addAuditLog(Long orderId, String action, Long operatorId, String comment) {
        OrderAuditLog log = new OrderAuditLog();
        log.setOrderId(orderId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setComment(comment);
        orderAuditLogMapper.insert(log);
    }

    private OrderRespDTO toRespDTO(SalesOrder order) {
        OrderRespDTO dto = new OrderRespDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setCustomerId(order.getCustomerId());
        dto.setSalesPersonId(order.getSalesPersonId());
        dto.setSalesWechatId(order.getSalesWechatId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusDisplay(getStatusDisplay(order.getStatus()));
        dto.setRemark(order.getRemark());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer != null) dto.setCustomerName(customer.getCustomerName());

        SysUser user = sysUserMapper.selectById(order.getSalesPersonId());
        if (user != null) dto.setSalesPersonName(user.getRealName());

        SalesWechat wechat = salesWechatMapper.selectById(order.getSalesWechatId());
        if (wechat != null) {
            dto.setSalesWechatAccount(wechat.getWechatAccount());
            dto.setSalesWechatNickname(wechat.getWechatNickname());
        }

        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getOrderId, order.getId()));
        dto.setItems(items);

        return dto;
    }

    private String getStatusDisplay(String status) {
        try {
            return OrderStatusEnum.valueOf(status).getDisplayName();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }
}
