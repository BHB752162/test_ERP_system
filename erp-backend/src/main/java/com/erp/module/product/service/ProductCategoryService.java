package com.erp.module.product.service;

import com.erp.module.product.dto.ProductCategoryReqDTO;
import com.erp.module.product.dto.ProductCategoryRespDTO;

import java.util.List;

public interface ProductCategoryService {
    List<ProductCategoryRespDTO> listTree();
    void create(ProductCategoryReqDTO req);
    void update(Long id, ProductCategoryReqDTO req);
    void delete(Long id);
}
