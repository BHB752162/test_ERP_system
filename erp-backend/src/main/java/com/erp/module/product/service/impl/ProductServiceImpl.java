package com.erp.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.product.dto.ProductReqDTO;
import com.erp.module.product.entity.Product;
import com.erp.module.product.mapper.ProductMapper;
import com.erp.module.product.service.ProductService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private ProductMapper productMapper;

    @Override
    public IPage<Product> listProducts(int page, int pageSize, Long categoryId, String keyword) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Product::getProductName, keyword)
                    .or().like(Product::getProductCode, keyword);
        }
        wrapper.ne(Product::getStatus, 0).orderByDesc(Product::getCreatedAt);
        return productMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("产品不存在");
        return product;
    }

    @Override
    public void create(ProductReqDTO req) {
        Product product = new Product();
        BeanUtils.copyProperties(req, product);
        if (product.getStatus() == null) product.setStatus(1);
        if (product.getStockQuantity() == null) product.setStockQuantity(0);
        productMapper.insert(product);
    }

    @Override
    public void update(Long id, ProductReqDTO req) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("产品不存在");
        BeanUtils.copyProperties(req, product);
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("产品不存在");
        product.setStatus(0);
        productMapper.updateById(product);
    }
}
