package com.erp.module.order.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateReqDTO {
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "微信号不能为空")
    private Long salesWechatId;

    private String remark;

    @NotNull(message = "订单项不能为空")
    private List<OrderItemReqDTO> items;

    @Data
    public static class OrderItemReqDTO {
        @NotNull(message = "产品ID不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;

        private BigDecimal unitPrice;
    }
}
