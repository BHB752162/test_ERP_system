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
import com.erp.module.customer.entity.CustomerShippingAddress;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.customer.mapper.CustomerShippingAddressMapper;
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
    private SalesWechatMapper salesWechatMapper;

    @Resource
    private CustomerWechatBindingMapper bindingMapper;

    @Resource
    private CustomerShippingAddressMapper shippingAddressMapper;

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
        return "B" + datePart + String.format("%06d", seq);
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
        if (req.getShippingAddressId() != null) {
            CustomerShippingAddress addr = shippingAddressMapper.selectById(req.getShippingAddressId());
            if (addr != null) {
                order.setShippingAddressId(addr.getId());
                order.setRecipientName(addr.getRecipientName());
                order.setRecipientPhone(addr.getRecipientPhone());
                order.setRecipientAddress(addr.getAddress());
            }
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
        order.setSubmittedAt(LocalDateTime.now());
        order.setUpdatedBy(currentUserId);
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
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdatedBy(currentUserId);
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
        order.setUpdatedBy(currentUserId);
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
        order.setUpdatedBy(currentUserId);
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
        order.setUpdatedBy(currentUserId);
        salesOrderMapper.updateById(order);
        addAuditLog(orderId, "COMPLETE", currentUserId, null);
    }

    @Override
    public byte[] exportOrders(String mode, String keyword, String status, Long currentUserId, String roleCode) {
        List<SalesOrder> orders = queryFilteredOrders(keyword, status, currentUserId, roleCode);
        if (orders.isEmpty()) {
            throw new BusinessException("没有匹配的订单数据可导出");
        }

        // Batch-load related data
        Set<Long> customerIds = orders.stream().map(SalesOrder::getCustomerId).collect(Collectors.toSet());
        Set<Long> userIds = orders.stream().map(SalesOrder::getSalesPersonId).collect(Collectors.toSet());
        Set<Long> wechatIds = orders.stream().map(SalesOrder::getSalesWechatId).collect(Collectors.toSet());
        List<Long> orderIds = orders.stream().map(SalesOrder::getId).collect(Collectors.toList());

        Map<Long, String> customerNames = loadCustomerNames(customerIds);
        Map<Long, String> userNames = loadUserNames(userIds);
        Map<Long, SalesWechat> wechats = loadWechats(wechatIds);
        Map<Long, List<SalesOrderItem>> itemsByOrder = loadItemsByOrderIds(orderIds);

        // Generate Excel
        Workbook wb = "product".equals(mode)
                ? createProductExportWorkbook(orders, customerNames, userNames, itemsByOrder)
                : createOrderExportWorkbook(orders, customerNames, userNames, wechats, itemsByOrder);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (wb) {
            wb.write(baos);
        } catch (IOException e) {
            throw new BusinessException("导出Excel失败");
        }
        return baos.toByteArray();
    }

    private List<SalesOrder> queryFilteredOrders(String keyword, String status, Long currentUserId, String roleCode) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
        if (!"ADMIN".equals(roleCode) && !"SALES_MANAGER".equals(roleCode)) {
            wrapper.eq(SalesOrder::getSalesPersonId, currentUserId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SalesOrder::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SalesOrder::getOrderNo, keyword);
        }
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

    private Map<Long, SalesWechat> loadWechats(Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return salesWechatMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SalesWechat::getId, w -> w));
    }

    private Map<Long, List<SalesOrderItem>> loadItemsByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Collections.emptyMap();
        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>().in(SalesOrderItem::getOrderId, orderIds));
        return items.stream().collect(Collectors.groupingBy(SalesOrderItem::getOrderId));
    }

    private Workbook createOrderExportWorkbook(List<SalesOrder> orders,
            Map<Long, String> customerNames, Map<Long, String> userNames,
            Map<Long, SalesWechat> wechats, Map<Long, List<SalesOrderItem>> itemsByOrder) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("订单导出");

        String[] headers = {"订单号", "客户名称", "销售姓名", "微信账号", "微信昵称",
            "商品总数", "总金额", "折扣金额", "成交金额", "状态", "标签", "备注",
            "创建时间", "提交时间", "审批时间"};

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
            SalesWechat wechat = wechats.get(order.getSalesWechatId());

            row.createCell(0).setCellValue(order.getOrderNo());
            row.createCell(1).setCellValue(customerNames.getOrDefault(order.getCustomerId(), ""));
            row.createCell(2).setCellValue(userNames.getOrDefault(order.getSalesPersonId(), ""));
            row.createCell(3).setCellValue(wechat != null ? wechat.getWechatAccount() : "");
            row.createCell(4).setCellValue(wechat != null ? wechat.getWechatNickname() : "");
            row.createCell(5).setCellValue(totalQty);
            row.createCell(6).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
            row.createCell(7).setCellValue(order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0.0);
            row.createCell(8).setCellValue(order.getFinalAmount() != null ? order.getFinalAmount().doubleValue() : 0.0);
            row.createCell(9).setCellValue(getStatusDisplay(order.getStatus()));
            row.createCell(10).setCellValue(order.getTag() != null ? order.getTag() : "");
            row.createCell(11).setCellValue(order.getRemark() != null ? order.getRemark() : "");
            row.createCell(12).setCellValue(formatDateTime(order.getCreatedAt()));
            row.createCell(13).setCellValue(formatDateTime(order.getSubmittedAt()));
            row.createCell(14).setCellValue(formatDateTime(order.getApprovedAt()));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        return wb;
    }

    private Workbook createProductExportWorkbook(List<SalesOrder> orders,
            Map<Long, String> customerNames, Map<Long, String> userNames,
            Map<Long, List<SalesOrderItem>> itemsByOrder) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("产品导出");

        String[] headers = {"订单号", "客户名称", "销售姓名", "产品名称", "数量", "单价", "小计", "订单状态", "创建时间"};

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
                row.createCell(7).setCellValue(getStatusDisplay(order.getStatus()));
                row.createCell(8).setCellValue(formatDateTime(order.getCreatedAt()));
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
                    row.createCell(7).setCellValue(getStatusDisplay(order.getStatus()));
                    row.createCell(8).setCellValue(formatDateTime(order.getCreatedAt()));
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

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer != null) dto.setCustomerName(customer.getCustomerName());

        SysUser user = sysUserMapper.selectById(order.getSalesPersonId());
        if (user != null) dto.setSalesPersonName(user.getRealName());

        if (order.getUpdatedBy() != null) {
            SysUser updater = sysUserMapper.selectById(order.getUpdatedBy());
            if (updater != null) dto.setUpdatedByName(updater.getRealName());
        }

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
