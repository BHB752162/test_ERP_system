package com.erp.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.module.product.dto.ProductReqDTO;
import com.erp.module.product.entity.Product;
import com.erp.module.product.mapper.ProductMapper;
import com.erp.module.product.service.ProductService;
import com.erp.module.user.entity.SysUser;
import com.erp.module.user.mapper.SysUserMapper;
import com.erp.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<Product> listProducts(int page, int pageSize, String keyword) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Product::getProductName, keyword)
                    .or().like(Product::getProductCode, keyword);
        }
        wrapper.ne(Product::getStatus, 0).orderByDesc(Product::getCreatedAt);
        IPage<Product> pageResult = productMapper.selectPage(pageParam, wrapper);

        // Batch load creator/updater names
        List<Product> records = pageResult.getRecords();
        Set<Long> userIds = records.stream()
                .flatMap(p -> Stream.of(p.getCreatedBy(), p.getUpdatedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            Map<Long, String> userMap = sysUserMapper.selectBatchIds(userIds)
                    .stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            for (Product p : records) {
                if (p.getCreatedBy() != null) p.setCreatedByName(userMap.get(p.getCreatedBy()));
                if (p.getUpdatedBy() != null) p.setUpdatedByName(userMap.get(p.getUpdatedBy()));
            }
        }
        return pageResult;
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
        Long userId = SecurityUtils.getCurrentUserId();
        product.setCreatedBy(userId);
        product.setUpdatedBy(userId);
        productMapper.insert(product);
    }

    @Override
    public void update(Long id, ProductReqDTO req) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("产品不存在");
        BeanUtils.copyProperties(req, product);
        product.setUpdatedBy(SecurityUtils.getCurrentUserId());
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
