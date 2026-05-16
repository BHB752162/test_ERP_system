package com.erp.module.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.module.product.dto.ProductReqDTO;
import com.erp.module.product.entity.Product;

public interface ProductService {
    IPage<Product> listProducts(int page, int pageSize, String keyword);
    Product getById(Long id);
    void create(ProductReqDTO req);
    void update(Long id, ProductReqDTO req);
    void delete(Long id);
}
