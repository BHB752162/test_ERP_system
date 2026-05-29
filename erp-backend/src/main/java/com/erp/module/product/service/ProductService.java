package com.erp.module.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.product.dto.ProductReqDTO;
import com.erp.module.product.entity.Product;

import java.util.List;

public interface ProductService {
    IPage<Product> listProducts(int page, int pageSize, String keyword, Integer status);
    Product getById(Long id);
    Long create(ProductReqDTO req);
    void update(Long id, ProductReqDTO req);
    void delete(Long id);
    List<Product> listSets();
    List<Product> listChildren(Long parentId);
    void updateChildren(Long parentId, List<Long> childIds);
}
