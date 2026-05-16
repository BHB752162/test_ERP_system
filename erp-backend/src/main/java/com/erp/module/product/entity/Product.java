package com.erp.module.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private String productCode;

    private String productType;

    private String description;

    private BigDecimal price;

    private BigDecimal costPrice;

    private Integer stockQuantity;

    private Integer status;

    private Long createdBy;

    private Long updatedBy;

    @TableField(exist = false)
    private String createdByName;

    @TableField(exist = false)
    private String updatedByName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
