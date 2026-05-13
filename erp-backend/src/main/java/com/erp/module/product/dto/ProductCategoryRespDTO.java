package com.erp.module.product.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductCategoryRespDTO {
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private List<ProductCategoryRespDTO> children;
}
