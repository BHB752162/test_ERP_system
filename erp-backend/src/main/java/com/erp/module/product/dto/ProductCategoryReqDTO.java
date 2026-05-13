package com.erp.module.product.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ProductCategoryReqDTO {
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;
}
