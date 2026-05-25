package com.erp.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.order.dto.OrderCreateReqDTO;
import com.erp.module.order.dto.OrderRespDTO;
import com.erp.module.order.dto.TrackingImportReqDTO;
import com.erp.module.order.entity.OrderTracking;
import com.erp.module.order.entity.SalesOrder;

import java.util.List;

public interface OrderService {
    IPage<SalesOrder> listOrders(int page, int pageSize, String status, Long currentUserId, String roleCode);
    OrderRespDTO getOrderDetail(Long id);
    SalesOrder createDraft(OrderCreateReqDTO req, Long salesPersonId);
    void submitForApproval(Long orderId, Long currentUserId);
    void approveOrder(Long orderId, Long currentUserId, String comment);
    void rejectOrder(Long orderId, Long currentUserId, String comment);
    void cancelOrder(Long orderId, Long currentUserId);
    void shipOrder(Long orderId, Long currentUserId);
    void deliverOrder(Long orderId, Long currentUserId);
    void refundOrder(Long orderId, Long currentUserId, String comment);
    void updateOrder(Long id, OrderCreateReqDTO req, Long currentUserId);
    byte[] exportOrders(String mode, String keyword, String status, Long currentUserId, String roleCode);
    void importTracking(List<TrackingImportReqDTO> data, Long currentUserId);
    List<OrderTracking> listTracking(Long orderId);
}
