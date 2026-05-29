package com.erp.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.constant.OrderStatusEnum;
import com.erp.common.exception.BusinessException;
import com.erp.module.audit.entity.OrderAuditLog;
import com.erp.module.audit.mapper.OrderAuditLogMapper;
import com.erp.module.binding.entity.CustomerSalesAccountBinding;
import com.erp.module.binding.mapper.CustomerSalesAccountBindingMapper;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.entity.CustomerShippingAddress;
import com.erp.module.customer.entity.PaymentChannelType;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.customer.mapper.CustomerShippingAddressMapper;
import com.erp.module.customer.mapper.PaymentChannelTypeMapper;
import com.erp.module.order.dto.OrderCreateReqDTO;
import com.erp.module.order.dto.OrderQueryDTO;
import com.erp.module.order.dto.OrderRespDTO;
import com.erp.module.order.dto.TrackingImportReqDTO;
import com.erp.module.order.entity.OrderTracking;
import com.erp.module.order.entity.OrderTrackingItem;
import com.erp.module.order.entity.SalesOrder;
import com.erp.module.order.entity.SalesOrderItem;
import com.erp.module.order.entity.SalesOrderPayment;
import com.erp.module.order.mapper.OrderTrackingItemMapper;
import com.erp.module.order.mapper.OrderTrackingMapper;
import com.erp.module.order.mapper.SalesOrderItemMapper;
import com.erp.module.order.mapper.SalesOrderMapper;
import com.erp.module.order.mapper.SalesOrderPaymentMapper;
import com.erp.module.order.service.OrderService;
import com.erp.module.product.entity.Product;
import com.erp.security.SecurityUtils;
import com.erp.module.product.mapper.ProductMapper;
import com.erp.module.salesaccount.entity.SalesAccount;
import com.erp.module.salesaccount.entity.SalesAccountUserBinding;
import com.erp.module.salesaccount.mapper.SalesAccountMapper;
import com.erp.module.salesaccount.mapper.SalesAccountUserBindingMapper;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    private SalesAccountMapper salesAccountMapper;

    @Resource
    private SalesAccountUserBindingMapper salesAccountUserBindingMapper;

    @Resource
    private CustomerSalesAccountBindingMapper bindingMapper;

    @Resource
    private CustomerShippingAddressMapper shippingAddressMapper;

    @Resource
    private OrderAuditLogMapper orderAuditLogMapper;

    @Resource
    private PaymentChannelTypeMapper paymentChannelTypeMapper;

    @Resource
    private SalesOrderPaymentMapper salesOrderPaymentMapper;

    @Resource
    private OrderTrackingMapper orderTrackingMapper;

    @Resource
    private OrderTrackingItemMapper orderTrackingItemMapper;

    private final AtomicInteger dailySeq = new AtomicInteger(0);
    private String currentDatePart = "";

    private synchronized String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        if (!datePart.equals(currentDatePart)) {
            currentDatePart = datePart;
            dailySeq.set(0);
        }
        int seq = dailySeq.incrementAndGet();
        return "B" + datePart + String.format("%06d", seq);
    }

    /**
     * 乐观锁更新订单，0行受影响表示并发冲突
     */
    private void updateOrderWithVersion(SalesOrder order) {
        int rows = salesOrderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException("订单已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    public IPage<SalesOrder> listOrders(int page, int pageSize, OrderQueryDTO query, Long currentUserId, String roleCode) {
        Page<SalesOrder> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SalesOrder> wrapper = buildOrderQueryWrapper(query, currentUserId, roleCode);
        wrapper.orderByDesc(SalesOrder::getCreatedAt);
        IPage<SalesOrder> result = salesOrderMapper.selectPage(pageParam, wrapper);

        // 批量加载关联名称
        List<SalesOrder> orders = result.getRecords();
        if (!orders.isEmpty()) {
            Set<Long> customerIds = orders.stream().map(SalesOrder::getCustomerId).collect(Collectors.toSet());
            Set<Long> userIds = orders.stream().map(SalesOrder::getSalesPersonId).collect(Collectors.toSet());
            Set<Long> accountIds = orders.stream().map(SalesOrder::getSalesAccountId).filter(Objects::nonNull).collect(Collectors.toSet());

            Map<Long, String> customerNames = loadCustomerNames(customerIds);
            Map<Long, String> userNames = loadUserNames(userIds);
            Map<Long, SalesAccount> accounts = loadSalesAccounts(accountIds);
            List<Long> orderIdsForPayments = orders.stream().map(SalesOrder::getId).collect(Collectors.toList());
            Map<Long, List<OrderTracking>> trackingsByOrder = loadTrackingsByOrderIds(orderIdsForPayments);
            Map<Long, List<SalesOrderPayment>> paymentsByOrder = loadPaymentsByOrderIds(orderIdsForPayments);
            Set<Long> channelTypeIds = paymentsByOrder.values().stream()
                    .flatMap(List::stream)
                    .map(SalesOrderPayment::getPaymentChannelTypeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, String> channelTypeNames = loadChannelTypeNames(channelTypeIds);

            for (SalesOrder order : orders) {
                order.setCustomerName(customerNames.get(order.getCustomerId()));
                order.setSalesPersonName(userNames.get(order.getSalesPersonId()));
                if (order.getSalesAccountId() != null) {
                    SalesAccount account = accounts.get(order.getSalesAccountId());
                    if (account != null) {
                        order.setSalesAccountName(account.getAccountName());
                        order.setSalesAccountDisplayName(account.getDisplayName());
                    }
                }
                order.setStatusDisplay(getStatusDisplay(order.getStatus()));
                order.setTrackings(trackingsByOrder.getOrDefault(order.getId(), Collections.emptyList()));
            }
            populatePaymentDisplay(orders, paymentsByOrder, channelTypeNames);
        }

        return result;
    }

    @Override
    public OrderRespDTO getOrderDetail(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        OrderRespDTO dto = buildRespDTO(order);
        dto.setItems(loadItemsByOrderIds(Collections.singletonList(id)).getOrDefault(id, Collections.emptyList()));
        return dto;
    }

    @Override
    @Transactional
    public SalesOrder createDraft(OrderCreateReqDTO req, Long salesPersonId) {
        // Validate sales account-customer binding
        long bindingCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                        .eq(CustomerSalesAccountBinding::getSalesAccountId, req.getSalesAccountId())
                        .eq(CustomerSalesAccountBinding::getCustomerId, req.getCustomerId()));
        if (bindingCount == 0) {
            throw new BusinessException("该销售账户未绑定此客户，无法下单");
        }

        // Create order
        SalesOrder order = new SalesOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(req.getCustomerId());
        order.setSalesPersonId(salesPersonId);
        order.setSalesAccountId(req.getSalesAccountId());
        order.setStatus("SAVED");
        order.setTag(req.getTag() != null ? req.getTag() : "");
        order.setMallOrderInfo(req.getMallOrderInfo() != null ? req.getMallOrderInfo() : "");
        order.setRemark(req.getRemark());
        order.setUpdatedBy(salesPersonId);

        // Calculate amounts from items
        BigDecimal total = BigDecimal.ZERO;
        for (OrderCreateReqDTO.OrderItemReqDTO itemReq : req.getItems()) {
            Product product = productMapper.selectById(itemReq.getProductId());
            if (product == null) throw new BusinessException("产品不存在: " + itemReq.getProductId());
            BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : product.getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        // Set shipping address snapshot
        CustomerShippingAddress addr = shippingAddressMapper.selectById(req.getShippingAddressId());
        if (addr == null) throw new BusinessException("收件地址不存在");
        order.setShippingAddressId(addr.getId());
        order.setRecipientName(addr.getRecipientName());
        order.setRecipientPhone(addr.getRecipientPhone());
        order.setRecipientAddress(addr.getAddress());

        order.setTotalAmount(total);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(total);
        salesOrderMapper.insert(order);

        // Create payment records
        for (OrderCreateReqDTO.PaymentItemReqDTO paymentReq : req.getPayments()) {
            SalesOrderPayment payment = new SalesOrderPayment();
            payment.setOrderId(order.getId());
            payment.setPaymentChannelTypeId(paymentReq.getPaymentChannelTypeId());
            payment.setPaymentAmount(paymentReq.getPaymentAmount());
            salesOrderPaymentMapper.insert(payment);
        }

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
        if (!"SAVED".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) throw new BusinessException("只有已保存或已驳回的订单才能提交");
        if (!order.getSalesPersonId().equals(currentUserId)) {
            // 管理员可代提交
            String roleCode = SecurityUtils.getCurrentRoleCode();
            if (!"ADMIN".equals(roleCode)) {
                throw new BusinessException("只能提交自己的订单");
            }
        }

        order.setStatus("PENDING_APPROVAL");
        order.setSubmittedAt(LocalDateTime.now());
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);
        addAuditLog(orderId, "SUBMIT", currentUserId, null);
    }

    @Override
    @Transactional
    public void approveOrder(Long orderId, Long currentUserId, String comment) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING_APPROVAL".equals(order.getStatus())) throw new BusinessException("只有待审批的订单才能通过");

        order.setStatus("APPROVED");
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);
        addAuditLog(orderId, "APPROVE", currentUserId, comment);
    }

    @Override
    @Transactional
    public void rejectOrder(Long orderId, Long currentUserId, String comment) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING_APPROVAL".equals(order.getStatus())) throw new BusinessException("只有待审批的订单才能驳回");

        order.setStatus("REJECTED");
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);
        addAuditLog(orderId, "REJECT", currentUserId, comment);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long currentUserId) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"SAVED".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) {
            throw new BusinessException("只有已保存或已驳回的订单才能取消");
        }

        order.setStatus("CANCELLED");
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);
        addAuditLog(orderId, "CANCEL", currentUserId, null);
    }

    @Override
    @Transactional
    public void shipOrder(Long orderId, Long currentUserId) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"APPROVED".equals(order.getStatus())) throw new BusinessException("只有已审批的订单才能发货");

        order.setStatus("SHIPPED");
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);
        addAuditLog(orderId, "SHIP", currentUserId, null);
    }

    @Override
    @Transactional
    public void deliverOrder(Long orderId, Long currentUserId) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"SHIPPED".equals(order.getStatus())) throw new BusinessException("只有已发货的订单才能确认妥投");

        // 校验妥投金额与收款金额一致
        List<OrderTracking> trackings = orderTrackingMapper.selectList(
                new LambdaQueryWrapper<OrderTracking>().eq(OrderTracking::getOrderId, orderId));
        BigDecimal totalDelivery = trackings.stream()
                .map(t -> t.getDeliveryAmount() != null ? t.getDeliveryAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SalesOrderPayment> payments = salesOrderPaymentMapper.selectList(
                new LambdaQueryWrapper<SalesOrderPayment>().eq(SalesOrderPayment::getOrderId, orderId));
        BigDecimal totalPayment = payments.stream()
                .map(p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDelivery.compareTo(totalPayment) != 0) {
            throw new BusinessException("运单号妥投金额总数(" + totalDelivery + ")与收款金额(" + totalPayment + ")不一致，无法确认妥投");
        }

        order.setStatus("DELIVERED");
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);
        addAuditLog(orderId, "DELIVER", currentUserId, null);
    }

    @Override
    @Transactional
    public void refundOrder(Long orderId, Long currentUserId, String comment) {
        SalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"APPROVED".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
            throw new BusinessException("只有已审批、已发货或已妥投的订单才能退款");
        }

        order.setStatus("REFUNDED");
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);
        addAuditLog(orderId, "REFUND", currentUserId, comment);
    }

    @Override
    @Transactional
    public void updateOrder(Long id, OrderCreateReqDTO req, Long currentUserId) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!SecurityUtils.hasRole("ADMIN") && !"SAVED".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) {
            throw new BusinessException("只有已保存或已驳回的订单才能编辑");
        }

        // 验证账户-客户绑定关系
        long bindingCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                        .eq(CustomerSalesAccountBinding::getSalesAccountId, req.getSalesAccountId())
                        .eq(CustomerSalesAccountBinding::getCustomerId, req.getCustomerId()));
        if (bindingCount == 0) {
            throw new BusinessException("该销售账户未绑定此客户，无法下单");
        }

        // 删除旧的订单明细和收款记录
        salesOrderItemMapper.delete(new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id));
        salesOrderPaymentMapper.delete(new LambdaQueryWrapper<SalesOrderPayment>().eq(SalesOrderPayment::getOrderId, id));

        // 计算金额
        BigDecimal total = BigDecimal.ZERO;
        for (OrderCreateReqDTO.OrderItemReqDTO itemReq : req.getItems()) {
            Product product = productMapper.selectById(itemReq.getProductId());
            if (product == null) throw new BusinessException("产品不存在: " + itemReq.getProductId());
            BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : product.getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        // 收件地址快照
        CustomerShippingAddress addr = shippingAddressMapper.selectById(req.getShippingAddressId());
        if (addr == null) throw new BusinessException("收件地址不存在");
        order.setShippingAddressId(addr.getId());
        order.setRecipientName(addr.getRecipientName());
        order.setRecipientPhone(addr.getRecipientPhone());
        order.setRecipientAddress(addr.getAddress());

        // 更新主表字段
        order.setCustomerId(req.getCustomerId());
        order.setSalesAccountId(req.getSalesAccountId());
        order.setTag(req.getTag() != null ? req.getTag() : "");
        order.setMallOrderInfo(req.getMallOrderInfo() != null ? req.getMallOrderInfo() : "");
        order.setRemark(req.getRemark());
        order.setTotalAmount(total);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(total);
        order.setUpdatedBy(currentUserId);
        updateOrderWithVersion(order);

        // 插入新的收款记录
        for (OrderCreateReqDTO.PaymentItemReqDTO paymentReq : req.getPayments()) {
            SalesOrderPayment payment = new SalesOrderPayment();
            payment.setOrderId(order.getId());
            payment.setPaymentChannelTypeId(paymentReq.getPaymentChannelTypeId());
            payment.setPaymentAmount(paymentReq.getPaymentAmount());
            salesOrderPaymentMapper.insert(payment);
        }

        // 插入新的订单明细
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

        // 如果已有运单号，校验妥投金额不超过新收款金额，并调整状态
        List<OrderTracking> editTrackings = orderTrackingMapper.selectList(
                new LambdaQueryWrapper<OrderTracking>().eq(OrderTracking::getOrderId, id));
        if (!editTrackings.isEmpty()) {
            BigDecimal totalDelivery = editTrackings.stream()
                    .map(t -> t.getDeliveryAmount() != null ? t.getDeliveryAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<SalesOrderPayment> newPayments = salesOrderPaymentMapper.selectList(
                    new LambdaQueryWrapper<SalesOrderPayment>().eq(SalesOrderPayment::getOrderId, id));
            BigDecimal totalPayment = newPayments.stream()
                    .map(p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalDelivery.compareTo(totalPayment) > 0) {
                throw new BusinessException("运单号妥投金额总数(" + totalDelivery
                        + ")超过修改后的收款金额(" + totalPayment + ")，请先删除对应运单号或调整收款金额");
            }

            String previousStatus = order.getStatus();
            if (totalDelivery.compareTo(totalPayment) == 0) {
                if (!"DELIVERED".equals(order.getStatus())) {
                    order.setStatus("DELIVERED");
                }
            } else {
                if ("DELIVERED".equals(order.getStatus())) {
                    order.setStatus("SHIPPED");
                }
            }
            if (!order.getStatus().equals(previousStatus)) {
                order.setUpdatedBy(currentUserId);
                updateOrderWithVersion(order);
            }
        }

        addAuditLog(order.getId(), "EDIT", currentUserId, null);
    }

    @Override
    @Transactional
    public void importTracking(List<TrackingImportReqDTO> data, Long currentUserId) {
        if (data == null || data.isEmpty()) {
            throw new BusinessException("导入数据不能为空");
        }

        // 校验必填字段
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < data.size(); i++) {
            TrackingImportReqDTO row = data.get(i);
            int lineNo = i + 1;
            if (row.getOrderNo() == null || row.getOrderNo().trim().isEmpty()) {
                throw new BusinessException("第" + lineNo + "行：订单号不能为空");
            }
            if (row.getTrackingNo() == null || row.getTrackingNo().trim().isEmpty()) {
                throw new BusinessException("第" + lineNo + "行：运单号不能为空");
            }
            if (row.getProductSku() == null || row.getProductSku().trim().isEmpty()) {
                throw new BusinessException("第" + lineNo + "行：产品SKU不能为空");
            }
            if (row.getQuantity() == null || row.getQuantity() <= 0) {
                throw new BusinessException("第" + lineNo + "行：数量必须大于0");
            }
            if (row.getDeliveryAmount() == null) {
                row.setDeliveryAmount(BigDecimal.ZERO);
            }
        }

        // 校验所有 SKU 是否在系统中存在
        Set<String> importSkus = data.stream()
                .map(r -> r.getProductSku().trim())
                .collect(Collectors.toSet());
        Set<String> existingSkus = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getProductCode, importSkus))
                .stream().map(Product::getProductCode).collect(Collectors.toSet());
        importSkus.removeAll(existingSkus);
        if (!importSkus.isEmpty()) {
            throw new BusinessException("以下产品SKU在系统中不存在，请检查：\n" + String.join("、", importSkus));
        }

        // 按 (orderNo, trackingNo) 分组
        Map<String, Map<String, List<TrackingImportReqDTO>>> grouped = data.stream()
                .collect(Collectors.groupingBy(
                        TrackingImportReqDTO::getOrderNo,
                        Collectors.groupingBy(TrackingImportReqDTO::getTrackingNo)));

        Set<String> orderNos = grouped.keySet();

        // 批量查询订单
        List<SalesOrder> orders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>().in(SalesOrder::getOrderNo, orderNos));
        Map<String, SalesOrder> orderMap = orders.stream()
                .collect(Collectors.toMap(SalesOrder::getOrderNo, o -> o));

        // 第一轮：校验订单存在 + 状态合法
        for (String orderNo : orderNos) {
            SalesOrder order = orderMap.get(orderNo);
            if (order == null) {
                throw new BusinessException("订单不存在: " + orderNo);
            }
            if (!"APPROVED".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())
                    && !"DELIVERED".equals(order.getStatus())) {
                throw new BusinessException("订单 " + orderNo + " 状态不允许导入运单号（仅已审批/已发货/已妥投）");
            }
        }

        // 查询已有运单号的妥投金额合计 + 收款金额合计，用于金额校验
        List<Long> orderIdList = orders.stream().map(SalesOrder::getId).collect(Collectors.toList());

        List<OrderTracking> existingTrackings = orderTrackingMapper.selectList(
                new LambdaQueryWrapper<OrderTracking>().in(OrderTracking::getOrderId, orderIdList));
        Map<Long, BigDecimal> existingDeliveryByOrder = existingTrackings.stream()
                .collect(Collectors.groupingBy(OrderTracking::getOrderId,
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> t.getDeliveryAmount() != null ? t.getDeliveryAmount() : BigDecimal.ZERO,
                                BigDecimal::add)));

        // 校验运单号是否已存在（防止 uk_order_tracking 约束冲突）
        Map<Long, Set<String>> existingTrackingNosByOrder = existingTrackings.stream()
                .collect(Collectors.groupingBy(OrderTracking::getOrderId,
                        Collectors.mapping(OrderTracking::getTrackingNo, Collectors.toSet())));
        for (String orderNo : orderNos) {
            SalesOrder order = orderMap.get(orderNo);
            Set<String> existingNos = existingTrackingNosByOrder.getOrDefault(order.getId(), Collections.emptySet());
            for (String trackingNo : grouped.get(orderNo).keySet()) {
                if (existingNos.contains(trackingNo)) {
                    throw new BusinessException("订单 " + orderNo + " 已存在运单号: " + trackingNo);
                }
            }
        }

        List<SalesOrderPayment> allPayments = salesOrderPaymentMapper.selectList(
                new LambdaQueryWrapper<SalesOrderPayment>().in(SalesOrderPayment::getOrderId, orderIdList));
        Map<Long, BigDecimal> paymentByOrder = allPayments.stream()
                .collect(Collectors.groupingBy(SalesOrderPayment::getOrderId,
                        Collectors.reducing(BigDecimal.ZERO,
                                p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : BigDecimal.ZERO,
                                BigDecimal::add)));

        // 计算本次导入的新增妥投金额
        Map<Long, BigDecimal> newDeliveryByOrder = new HashMap<>();
        for (String orderNo : orderNos) {
            SalesOrder order = orderMap.get(orderNo);
            Map<String, List<TrackingImportReqDTO>> trackingGroups = grouped.get(orderNo);
            BigDecimal newDelivery = BigDecimal.ZERO;
            for (List<TrackingImportReqDTO> rows : trackingGroups.values()) {
                BigDecimal amt = rows.get(0).getDeliveryAmount();
                if (amt != null) {
                    newDelivery = newDelivery.add(amt);
                }
            }
            newDeliveryByOrder.put(order.getId(), newDelivery);
        }

        // 金额校验：妥投总额不能超过收款金额
        for (String orderNo : orderNos) {
            SalesOrder order = orderMap.get(orderNo);
            BigDecimal existingDelivery = existingDeliveryByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);
            BigDecimal newDelivery = newDeliveryByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);
            BigDecimal totalPayment = paymentByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);
            BigDecimal newTotal = existingDelivery.add(newDelivery);
            if (newTotal.compareTo(totalPayment) > 0) {
                throw new BusinessException("订单 " + orderNo + " 的运单号妥投金额总数(" + newTotal
                        + ")超过收款金额(" + totalPayment + ")，无法导入");
            }
        }

        // 第二轮：创建运单号记录并更新状态
        for (String orderNo : orderNos) {
            SalesOrder order = orderMap.get(orderNo);
            Map<String, List<TrackingImportReqDTO>> trackingGroups = grouped.get(orderNo);
            for (Map.Entry<String, List<TrackingImportReqDTO>> entry : trackingGroups.entrySet()) {
                String trackingNo = entry.getKey();
                List<TrackingImportReqDTO> rows = entry.getValue();

                // 解析发货时间：优先用导入数据，未填则用系统时间
                LocalDateTime shippingTime = LocalDateTime.now();
                String shippingTimeStr = rows.get(0).getShippingTime();
                if (shippingTimeStr != null && !shippingTimeStr.trim().isEmpty()) {
                    try {
                        shippingTime = LocalDateTime.parse(shippingTimeStr.trim(), dateFormatter);
                    } catch (Exception e) {
                        throw new BusinessException("订单 " + orderNo + " 运单号 " + trackingNo + " 的发货时间格式错误，请使用 yyyy-MM-dd HH:mm:ss");
                    }
                }

                // 创建运单号主记录
                OrderTracking tracking = new OrderTracking();
                tracking.setOrderId(order.getId());
                tracking.setTrackingNo(trackingNo);
                tracking.setShipmentType(rows.get(0).getShipmentType());
                tracking.setShippingTime(shippingTime);
                tracking.setDeliveryAmount(rows.get(0).getDeliveryAmount());
                orderTrackingMapper.insert(tracking);

                // 创建 SKU 明细
                for (TrackingImportReqDTO row : rows) {
                    OrderTrackingItem item = new OrderTrackingItem();
                    item.setTrackingId(tracking.getId());
                    item.setProductSku(row.getProductSku().trim());
                    item.setQuantity(row.getQuantity());
                    orderTrackingItemMapper.insert(item);
                }
            }

            // 根据妥投金额 vs 收款金额更新状态
            BigDecimal existingDelivery = existingDeliveryByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);
            BigDecimal newDelivery = newDeliveryByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);
            BigDecimal totalDelivery = existingDelivery.add(newDelivery);
            BigDecimal totalPayment = paymentByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);

            String previousStatus = order.getStatus();
            if (totalDelivery.compareTo(totalPayment) == 0) {
                if (!"DELIVERED".equals(order.getStatus())) {
                    order.setStatus("DELIVERED");
                }
            } else if ("APPROVED".equals(order.getStatus())) {
                order.setStatus("SHIPPED");
            } else if ("DELIVERED".equals(order.getStatus())) {
                order.setStatus("SHIPPED");
            }
            if (!order.getStatus().equals(previousStatus)) {
                order.setUpdatedBy(currentUserId);
                updateOrderWithVersion(order);
            }
        }

        // 审计日志
        for (String orderNo : orderNos) {
            SalesOrder o = orderMap.get(orderNo);
            String trackingNos = grouped.get(orderNo).keySet().stream()
                    .collect(Collectors.joining(", "));
            addAuditLog(o.getId(), "SHIP", currentUserId, "导入运单号: " + trackingNos);
        }
    }

    @Override
    public List<OrderTracking> listTracking(Long orderId) {
        List<OrderTracking> trackings = orderTrackingMapper.selectList(
                new LambdaQueryWrapper<OrderTracking>().eq(OrderTracking::getOrderId, orderId));
        if (!trackings.isEmpty()) {
            List<Long> trackingIds = trackings.stream()
                    .map(OrderTracking::getId).collect(Collectors.toList());
            List<OrderTrackingItem> allItems = orderTrackingItemMapper.selectList(
                    new LambdaQueryWrapper<OrderTrackingItem>().in(OrderTrackingItem::getTrackingId, trackingIds));
            Map<Long, List<OrderTrackingItem>> itemsMap = allItems.stream()
                    .collect(Collectors.groupingBy(OrderTrackingItem::getTrackingId));

            // 回查产品名称
            Set<String> skus = allItems.stream().map(OrderTrackingItem::getProductSku).collect(Collectors.toSet());
            Map<String, String> skuNameMap = Collections.emptyMap();
            if (!skus.isEmpty()) {
                skuNameMap = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getProductCode, skus))
                        .stream().collect(Collectors.toMap(Product::getProductCode, Product::getProductName, (a, b) -> a));
            }
            for (OrderTrackingItem item : allItems) {
                item.setProductName(skuNameMap.getOrDefault(item.getProductSku(), item.getProductSku()));
            }

            for (OrderTracking t : trackings) {
                t.setItems(itemsMap.getOrDefault(t.getId(), Collections.emptyList()));
            }
        }
        return trackings;
    }

    @Override
    @Transactional
    public void deleteTracking(Long trackingId, Long currentUserId) {
        OrderTracking tracking = orderTrackingMapper.selectById(trackingId);
        if (tracking == null) {
            throw new BusinessException("运单号记录不存在");
        }

        SalesOrder order = salesOrderMapper.selectById(tracking.getOrderId());
        if (order == null) {
            throw new BusinessException("关联订单不存在");
        }

        // 删除运单号（级联删除 SKU 明细）
        orderTrackingMapper.deleteById(trackingId);

        // 重新计算妥投金额 vs 收款金额，确定订单状态
        List<OrderTracking> remainingTrackings = orderTrackingMapper.selectList(
                new LambdaQueryWrapper<OrderTracking>().eq(OrderTracking::getOrderId, order.getId()));

        if (remainingTrackings.isEmpty()) {
            if ("SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
                order.setStatus("APPROVED");
                order.setUpdatedBy(currentUserId);
                updateOrderWithVersion(order);
            }
        } else {
            BigDecimal totalDelivery = remainingTrackings.stream()
                    .map(t -> t.getDeliveryAmount() != null ? t.getDeliveryAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<SalesOrderPayment> payments = salesOrderPaymentMapper.selectList(
                    new LambdaQueryWrapper<SalesOrderPayment>().eq(SalesOrderPayment::getOrderId, order.getId()));
            BigDecimal totalPayment = payments.stream()
                    .map(p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String previousStatus = order.getStatus();
            if (totalDelivery.compareTo(totalPayment) == 0) {
                if (!"DELIVERED".equals(order.getStatus())) {
                    order.setStatus("DELIVERED");
                }
            } else {
                if ("DELIVERED".equals(order.getStatus())) {
                    order.setStatus("SHIPPED");
                }
            }
            if (!order.getStatus().equals(previousStatus)) {
                order.setUpdatedBy(currentUserId);
                updateOrderWithVersion(order);
            }
        }

        addAuditLog(order.getId(), "DELETE_TRACKING", currentUserId, "删除运单号: " + tracking.getTrackingNo());
    }

    @Override
    public byte[] exportOrders(String mode, OrderQueryDTO query, Long currentUserId, String roleCode) {
        List<SalesOrder> orders = queryFilteredOrders(query, currentUserId, roleCode);
        if (orders.isEmpty()) {
            throw new BusinessException("没有匹配的订单数据可导出");
        }

        // Batch-load related data
        Set<Long> customerIds = orders.stream().map(SalesOrder::getCustomerId).collect(Collectors.toSet());
        Set<Long> userIds = orders.stream().map(SalesOrder::getSalesPersonId).collect(Collectors.toSet());
        Set<Long> accountIds = orders.stream().map(SalesOrder::getSalesAccountId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> orderIds = orders.stream().map(SalesOrder::getId).collect(Collectors.toList());

        Map<Long, String> customerNames = loadCustomerNames(customerIds);
        Map<Long, String> userNames = loadUserNames(userIds);
        Map<Long, SalesAccount> accounts = loadSalesAccounts(accountIds);
        Map<Long, List<SalesOrderPayment>> paymentsByOrderExport = loadPaymentsByOrderIds(orderIds);
        Set<Long> channelTypeIds = paymentsByOrderExport.values().stream()
                .flatMap(List::stream)
                .map(SalesOrderPayment::getPaymentChannelTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> channelTypeNames = loadChannelTypeNames(channelTypeIds);
        Map<Long, List<SalesOrderItem>> itemsByOrder = loadItemsByOrderIds(orderIds);

        // Populate channel type names on payment records for export
        for (List<SalesOrderPayment> plist : paymentsByOrderExport.values()) {
            for (SalesOrderPayment p : plist) {
                p.setPaymentChannelTypeName(channelTypeNames.get(p.getPaymentChannelTypeId()));
            }
        }

        // Generate Excel
        Workbook wb = "product".equals(mode)
                ? createProductExportWorkbook(orders, customerNames, userNames, channelTypeNames, paymentsByOrderExport, itemsByOrder)
                : createOrderExportWorkbook(orders, customerNames, userNames, accounts, channelTypeNames, paymentsByOrderExport, itemsByOrder);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (wb) {
            wb.write(baos);
        } catch (IOException e) {
            throw new BusinessException("导出Excel失败");
        }
        return baos.toByteArray();
    }

    private LambdaQueryWrapper<SalesOrder> buildOrderQueryWrapper(OrderQueryDTO query, Long currentUserId, String roleCode) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();

        // 权限过滤：非管理员只看自己销售账户的订单
        if (!"ADMIN".equals(roleCode)) {
            List<Long> accountIds = salesAccountUserBindingMapper.selectList(
                    new LambdaQueryWrapper<SalesAccountUserBinding>()
                            .eq(SalesAccountUserBinding::getUserId, currentUserId))
                    .stream().map(SalesAccountUserBinding::getSalesAccountId)
                    .collect(Collectors.toList());
            if (accountIds.isEmpty()) {
                wrapper.eq(SalesOrder::getId, -1L);
            } else {
                wrapper.in(SalesOrder::getSalesAccountId, accountIds);
            }
        }

        // 顾客ID精确匹配（从顾客页路由过来的筛选）
        if (query.getCustomerId() != null) {
            wrapper.eq(SalesOrder::getCustomerId, query.getCustomerId());
        }

        // 订单号模糊查询
        if (query.getOrderNo() != null && !query.getOrderNo().isEmpty()) {
            wrapper.like(SalesOrder::getOrderNo, query.getOrderNo());
        }

        // 客户名 → 先查 customer 表获取匹配的 customerId
        if (query.getCustomerName() != null && !query.getCustomerName().isEmpty()) {
            List<Long> customerIds = customerMapper.selectList(
                    new LambdaQueryWrapper<Customer>()
                            .like(Customer::getCustomerName, query.getCustomerName())
                            .select(Customer::getId))
                    .stream().map(Customer::getId).collect(Collectors.toList());
            if (customerIds.isEmpty()) {
                wrapper.eq(SalesOrder::getId, -1L);
            } else {
                wrapper.in(SalesOrder::getCustomerId, customerIds);
            }
        }

        // 微信号 → 先查 customer 表
        if (query.getWechatAccount() != null && !query.getWechatAccount().isEmpty()) {
            List<Long> customerIds = customerMapper.selectList(
                    new LambdaQueryWrapper<Customer>()
                            .like(Customer::getWechatAccount, query.getWechatAccount())
                            .select(Customer::getId))
                    .stream().map(Customer::getId).collect(Collectors.toList());
            if (customerIds.isEmpty()) {
                wrapper.eq(SalesOrder::getId, -1L);
            } else {
                wrapper.in(SalesOrder::getCustomerId, customerIds);
            }
        }

        // 销售姓名 → 先查 sys_user 表
        if (query.getSalesPersonName() != null && !query.getSalesPersonName().isEmpty()) {
            List<Long> userIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, query.getSalesPersonName())
                            .select(SysUser::getId))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                wrapper.eq(SalesOrder::getId, -1L);
            } else {
                wrapper.in(SalesOrder::getSalesPersonId, userIds);
            }
        }

        // 销售账户 → 先模糊查销售账户名，再过滤
        if (query.getSalesAccountName() != null && !query.getSalesAccountName().isEmpty()) {
            List<Long> accountIds = salesAccountMapper.selectList(
                    new LambdaQueryWrapper<SalesAccount>()
                            .and(w -> w.like(SalesAccount::getAccountName, query.getSalesAccountName())
                                      .or().like(SalesAccount::getDisplayName, query.getSalesAccountName())))
                    .stream().map(SalesAccount::getId)
                    .collect(Collectors.toList());
            if (accountIds.isEmpty()) {
                wrapper.eq(SalesOrder::getId, -1L);
            } else {
                wrapper.in(SalesOrder::getSalesAccountId, accountIds);
            }
        }

        // 订单状态（精确匹配）
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(SalesOrder::getStatus, query.getStatus());
        }

        // 订单标签（精确匹配）
        if (query.getTag() != null && !query.getTag().isEmpty()) {
            wrapper.eq(SalesOrder::getTag, query.getTag());
        }

        // 收件人姓名（模糊查询，直接在 sales_order 上）
        if (query.getRecipientName() != null && !query.getRecipientName().isEmpty()) {
            wrapper.like(SalesOrder::getRecipientName, query.getRecipientName());
        }

        // 收件人电话（模糊查询）
        if (query.getRecipientPhone() != null && !query.getRecipientPhone().isEmpty()) {
            wrapper.like(SalesOrder::getRecipientPhone, query.getRecipientPhone());
        }

        // 创建时间范围
        if (query.getCreatedStart() != null) {
            wrapper.ge(SalesOrder::getCreatedAt, query.getCreatedStart().atStartOfDay());
        }
        if (query.getCreatedEnd() != null) {
            wrapper.le(SalesOrder::getCreatedAt, query.getCreatedEnd().plusDays(1).atStartOfDay());
        }

        // 提交审批时间范围
        if (query.getSubmittedStart() != null) {
            wrapper.ge(SalesOrder::getSubmittedAt, query.getSubmittedStart().atStartOfDay());
        }
        if (query.getSubmittedEnd() != null) {
            wrapper.le(SalesOrder::getSubmittedAt, query.getSubmittedEnd().plusDays(1).atStartOfDay());
        }

        return wrapper;
    }

    private List<SalesOrder> queryFilteredOrders(OrderQueryDTO query, Long currentUserId, String roleCode) {
        LambdaQueryWrapper<SalesOrder> wrapper = buildOrderQueryWrapper(query, currentUserId, roleCode);
        wrapper.orderByDesc(SalesOrder::getCreatedAt);
        return salesOrderMapper.selectList(wrapper);
    }

    private Map<Long, String> loadCustomerNames(Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getCustomerName));
    }

    private Map<Long, String> loadUserNames(Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
    }

    private Map<Long, SalesAccount> loadSalesAccounts(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return salesAccountMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SalesAccount::getId, a -> a));
    }

    private Map<Long, String> loadChannelTypeNames(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return paymentChannelTypeMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(PaymentChannelType::getId, PaymentChannelType::getTypeName));
    }

    private Map<Long, List<SalesOrderItem>> loadItemsByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Collections.emptyMap();
        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>().in(SalesOrderItem::getOrderId, orderIds));
        return items.stream().collect(Collectors.groupingBy(SalesOrderItem::getOrderId));
    }

    private Map<Long, List<OrderTracking>> loadTrackingsByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Collections.emptyMap();
        List<OrderTracking> trackings = orderTrackingMapper.selectList(
                new LambdaQueryWrapper<OrderTracking>().in(OrderTracking::getOrderId, orderIds));
        if (trackings.isEmpty()) return Collections.emptyMap();
        List<Long> trackingIds = trackings.stream().map(OrderTracking::getId).collect(Collectors.toList());
        List<OrderTrackingItem> allItems = orderTrackingItemMapper.selectList(
                new LambdaQueryWrapper<OrderTrackingItem>().in(OrderTrackingItem::getTrackingId, trackingIds));
        Map<Long, List<OrderTrackingItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderTrackingItem::getTrackingId));

        // 回查产品名称
        Set<String> skus = allItems.stream().map(OrderTrackingItem::getProductSku).collect(Collectors.toSet());
        Map<String, String> skuNameMap = Collections.emptyMap();
        if (!skus.isEmpty()) {
            skuNameMap = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().in(Product::getProductCode, skus))
                    .stream().collect(Collectors.toMap(Product::getProductCode, Product::getProductName, (a, b) -> a));
        }

        for (OrderTrackingItem item : allItems) {
            item.setProductName(skuNameMap.getOrDefault(item.getProductSku(), item.getProductSku()));
        }

        for (OrderTracking t : trackings) {
            t.setItems(itemsMap.getOrDefault(t.getId(), Collections.emptyList()));
        }
        return trackings.stream().collect(Collectors.groupingBy(OrderTracking::getOrderId));
    }

    private Map<Long, List<SalesOrderPayment>> loadPaymentsByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Collections.emptyMap();
        List<SalesOrderPayment> payments = salesOrderPaymentMapper.selectList(
                new LambdaQueryWrapper<SalesOrderPayment>().in(SalesOrderPayment::getOrderId, orderIds));
        return payments.stream().collect(Collectors.groupingBy(SalesOrderPayment::getOrderId));
    }

    private void populatePaymentDisplay(List<SalesOrder> orders,
            Map<Long, List<SalesOrderPayment>> paymentsByOrder,
            Map<Long, String> channelTypeNames) {
        for (SalesOrder order : orders) {
            List<SalesOrderPayment> payments = paymentsByOrder.getOrDefault(order.getId(), Collections.emptyList());
            if (!payments.isEmpty()) {
                for (SalesOrderPayment p : payments) {
                    p.setPaymentChannelTypeName(channelTypeNames.get(p.getPaymentChannelTypeId()));
                }
                String names = payments.stream()
                        .map(p -> p.getPaymentChannelTypeName() != null ? p.getPaymentChannelTypeName() : "")
                        .filter(n -> !n.isEmpty())
                        .collect(Collectors.joining(" / "));
                order.setPaymentChannelTypeName(names);
                BigDecimal totalPayment = payments.stream()
                        .map(SalesOrderPayment::getPaymentAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                order.setPaymentAmountDisplay(totalPayment);
            }
        }
    }

    private Workbook createOrderExportWorkbook(List<SalesOrder> orders,
            Map<Long, String> customerNames, Map<Long, String> userNames,
            Map<Long, SalesAccount> accounts, Map<Long, String> channelTypeNames,
            Map<Long, List<SalesOrderPayment>> paymentsByOrder,
            Map<Long, List<SalesOrderItem>> itemsByOrder) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("订单导出");

        String[] headers = {"订单号", "客户名称", "销售姓名", "销售账户",
            "商品总数", "总金额", "折扣金额", "成交金额", "状态", "标签", "备注",
            "收款渠道", "收款金额", "创建时间", "提交时间", "审批时间"};

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (SalesOrder order : orders) {
            Row row = sheet.createRow(rowIdx++);
            List<SalesOrderItem> items = itemsByOrder.getOrDefault(order.getId(), Collections.emptyList());
            int totalQty = items.stream().mapToInt(SalesOrderItem::getQuantity).sum();
            SalesAccount account = order.getSalesAccountId() != null ? accounts.get(order.getSalesAccountId()) : null;

            row.createCell(0).setCellValue(order.getOrderNo());
            row.createCell(1).setCellValue(customerNames.getOrDefault(order.getCustomerId(), ""));
            row.createCell(2).setCellValue(userNames.getOrDefault(order.getSalesPersonId(), ""));
            row.createCell(3).setCellValue(account != null ? account.getDisplayName() + "(" + account.getAccountName() + ")" : "");
            row.createCell(4).setCellValue(totalQty);
            row.createCell(5).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
            row.createCell(6).setCellValue(order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0.0);
            row.createCell(7).setCellValue(order.getFinalAmount() != null ? order.getFinalAmount().doubleValue() : 0.0);
            row.createCell(8).setCellValue(getStatusDisplay(order.getStatus()));
            row.createCell(9).setCellValue(order.getTag() != null ? order.getTag() : "");
            row.createCell(10).setCellValue(order.getRemark() != null ? order.getRemark() : "");
            List<SalesOrderPayment> payments = paymentsByOrder.getOrDefault(order.getId(), Collections.emptyList());
            String payChannels = payments.stream()
                    .map(p -> p.getPaymentChannelTypeName() != null ? p.getPaymentChannelTypeName() : "")
                    .filter(n -> !n.isEmpty())
                    .collect(Collectors.joining(" / "));
            double paySum = payments.stream()
                    .map(p -> p.getPaymentAmount() != null ? p.getPaymentAmount().doubleValue() : 0.0)
                    .mapToDouble(Double::doubleValue).sum();
            row.createCell(11).setCellValue(payChannels);
            row.createCell(12).setCellValue(paySum);
            row.createCell(13).setCellValue(formatDateTime(order.getCreatedAt()));
            row.createCell(14).setCellValue(formatDateTime(order.getSubmittedAt()));
            row.createCell(15).setCellValue(formatDateTime(order.getApprovedAt()));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        return wb;
    }

    private Workbook createProductExportWorkbook(List<SalesOrder> orders,
            Map<Long, String> customerNames, Map<Long, String> userNames,
            Map<Long, String> channelTypeNames,
            Map<Long, List<SalesOrderPayment>> paymentsByOrder,
            Map<Long, List<SalesOrderItem>> itemsByOrder) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("产品导出");

        String[] headers = {"订单号", "客户名称", "销售姓名", "产品名称", "数量", "单价", "小计", "订单状态", "收款渠道", "收款金额", "创建时间"};

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (SalesOrder order : orders) {
            List<SalesOrderItem> items = itemsByOrder.getOrDefault(order.getId(), Collections.emptyList());
            if (items.isEmpty()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getOrderNo());
                row.createCell(1).setCellValue(customerNames.getOrDefault(order.getCustomerId(), ""));
                row.createCell(2).setCellValue(userNames.getOrDefault(order.getSalesPersonId(), ""));
                row.createCell(3).setCellValue("(无产品)");
                row.createCell(4).setCellValue(0);
                row.createCell(5).setCellValue(0.0);
                row.createCell(6).setCellValue(0.0);
                List<SalesOrderPayment> payments = paymentsByOrder.getOrDefault(order.getId(), Collections.emptyList());
                String payChannels = payments.stream()
                        .map(p -> p.getPaymentChannelTypeName() != null ? p.getPaymentChannelTypeName() : "")
                        .filter(n -> !n.isEmpty())
                        .collect(Collectors.joining(" / "));
                double paySum = payments.stream()
                        .map(p -> p.getPaymentAmount() != null ? p.getPaymentAmount().doubleValue() : 0.0)
                        .mapToDouble(Double::doubleValue).sum();
                row.createCell(7).setCellValue(getStatusDisplay(order.getStatus()));
                row.createCell(8).setCellValue(payChannels);
                row.createCell(9).setCellValue(paySum);
                row.createCell(10).setCellValue(formatDateTime(order.getCreatedAt()));
            } else {
                for (SalesOrderItem item : items) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(order.getOrderNo());
                    row.createCell(1).setCellValue(customerNames.getOrDefault(order.getCustomerId(), ""));
                    row.createCell(2).setCellValue(userNames.getOrDefault(order.getSalesPersonId(), ""));
                    row.createCell(3).setCellValue(item.getProductName());
                    row.createCell(4).setCellValue(item.getQuantity());
                    row.createCell(5).setCellValue(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
                    row.createCell(6).setCellValue(item.getSubtotal() != null ? item.getSubtotal().doubleValue() : 0.0);
                    List<SalesOrderPayment> payments = paymentsByOrder.getOrDefault(order.getId(), Collections.emptyList());
                    String payChannels = payments.stream()
                            .map(p -> p.getPaymentChannelTypeName() != null ? p.getPaymentChannelTypeName() : "")
                            .filter(n -> !n.isEmpty())
                            .collect(Collectors.joining(" / "));
                    double paySum = payments.stream()
                            .map(p -> p.getPaymentAmount() != null ? p.getPaymentAmount().doubleValue() : 0.0)
                            .mapToDouble(Double::doubleValue).sum();
                    row.createCell(7).setCellValue(getStatusDisplay(order.getStatus()));
                    row.createCell(8).setCellValue(payChannels);
                    row.createCell(9).setCellValue(paySum);
                    row.createCell(10).setCellValue(formatDateTime(order.getCreatedAt()));
                }
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        return wb;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void addAuditLog(Long orderId, String action, Long operatorId, String comment) {
        OrderAuditLog log = new OrderAuditLog();
        log.setOrderId(orderId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setComment(comment);
        orderAuditLogMapper.insert(log);
    }

    private OrderRespDTO buildRespDTO(SalesOrder order) {
        OrderRespDTO dto = new OrderRespDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setCustomerId(order.getCustomerId());
        dto.setSalesPersonId(order.getSalesPersonId());
        dto.setSalesAccountId(order.getSalesAccountId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusDisplay(getStatusDisplay(order.getStatus()));
        dto.setTag(order.getTag());
        dto.setMallOrderInfo(order.getMallOrderInfo());
        dto.setRemark(order.getRemark());
        dto.setShippingAddressId(order.getShippingAddressId());
        dto.setRecipientName(order.getRecipientName());
        dto.setRecipientPhone(order.getRecipientPhone());
        dto.setRecipientAddress(order.getRecipientAddress());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setSubmittedAt(order.getSubmittedAt());
        dto.setApprovedAt(order.getApprovedAt());
        dto.setUpdatedBy(order.getUpdatedBy());

        // 批量风格加载：单条订单也统一走 Map 查询，与导出/列表共用同一代码路径
        Set<Long> customerIds = order.getCustomerId() != null ? Set.of(order.getCustomerId()) : Collections.emptySet();
        Set<Long> userIds = new HashSet<>();
        if (order.getSalesPersonId() != null) userIds.add(order.getSalesPersonId());
        if (order.getUpdatedBy() != null) userIds.add(order.getUpdatedBy());
        Set<Long> accountIds = order.getSalesAccountId() != null ? Set.of(order.getSalesAccountId()) : Collections.emptySet();

        Map<Long, String> customerNames = loadCustomerNames(customerIds);
        Map<Long, String> userNames = loadUserNames(userIds);
        Map<Long, SalesAccount> accounts = loadSalesAccounts(accountIds);

        dto.setCustomerName(customerNames.get(order.getCustomerId()));
        dto.setSalesPersonName(userNames.get(order.getSalesPersonId()));
        if (order.getUpdatedBy() != null) dto.setUpdatedByName(userNames.get(order.getUpdatedBy()));
        if (order.getSalesAccountId() != null) {
            SalesAccount account = accounts.get(order.getSalesAccountId());
            if (account != null) {
                dto.setSalesAccountName(account.getAccountName());
                dto.setSalesAccountDisplayName(account.getDisplayName());
            }
        }
        Map<Long, List<SalesOrderPayment>> paymentsByOrder = loadPaymentsByOrderIds(Collections.singletonList(order.getId()));
        List<SalesOrderPayment> payments = paymentsByOrder.getOrDefault(order.getId(), Collections.emptyList());
        if (!payments.isEmpty()) {
            Set<Long> paymentChannelIds = payments.stream()
                    .map(SalesOrderPayment::getPaymentChannelTypeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, String> channelNames = loadChannelTypeNames(paymentChannelIds);
            for (SalesOrderPayment p : payments) {
                p.setPaymentChannelTypeName(channelNames.get(p.getPaymentChannelTypeId()));
            }
        }
        dto.setPayments(payments);

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
