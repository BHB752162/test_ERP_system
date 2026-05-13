package com.erp.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.order.dto.OrderCreateReqDTO;
import com.erp.module.order.dto.OrderRespDTO;
import com.erp.module.order.entity.SalesOrder;

public interface OrderService {
    IPage<SalesOrder> listOrders(int page, int pageSize, String status, Long currentUserId, String roleCode);
    OrderRespDTO getOrderDetail(Long id);
    SalesOrder createDraft(OrderCreateReqDTO req, Long salesPersonId);
    void submitForApproval(Long orderId, Long currentUserId);
    void approveOrder(Long orderId, Long currentUserId, String comment);
    void rejectOrder(Long orderId, Long currentUserId, String comment);
    void cancelOrder(Long orderId, Long currentUserId);
    void completeOrder(Long orderId, Long currentUserId);
}
