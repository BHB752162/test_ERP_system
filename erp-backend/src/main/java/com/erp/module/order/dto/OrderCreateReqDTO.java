package com.erp.module.order.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateReqDTO {
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "销售账户ID不能为空")
    private Long salesAccountId;

    private String tag;

    private String mallOrderInfo;

    private String remark;

    @NotNull(message = "收件地址不能为空")
    private Long shippingAddressId;

    @NotNull(message = "收款信息不能为空")
    private List<PaymentItemReqDTO> payments;

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

    @Data
    public static class PaymentItemReqDTO {
        @NotNull(message = "收款渠道不能为空")
        private Long paymentChannelTypeId;

        @NotNull(message = "收款金额不能为空")
        private BigDecimal paymentAmount;
    }
}
