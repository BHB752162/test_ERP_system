package com.erp.module.product.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductReqDTO {
    @NotBlank(message = "产品名称不能为空")
    private String productName;

    private Long categoryId;

    private String productCode;

    private String description;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private BigDecimal costPrice;

    private Integer stockQuantity;

    private Integer status;
}
