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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public IPage<Product> listProducts(int page, int pageSize, String keyword, Integer status) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Product::getProductName, keyword)
                    .or().like(Product::getProductCode, keyword);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        } else {
            wrapper.ne(Product::getStatus, 0);
        }
        wrapper.orderByDesc(Product::getCreatedAt);
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

        // Batch load parent names for SINGLE products
        Set<Long> parentIds = records.stream()
                .map(Product::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!parentIds.isEmpty()) {
            Map<Long, String> parentMap = productMapper.selectBatchIds(parentIds)
                    .stream().collect(Collectors.toMap(Product::getId, Product::getProductName));
            for (Product p : records) {
                if (p.getParentId() != null) p.setParentName(parentMap.get(p.getParentId()));
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
    @Transactional
    public Long create(ProductReqDTO req) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException("无法获取当前用户信息");
        if (StringUtils.isNotBlank(req.getProductCode())) {
            Long count = productMapper.selectCount(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getProductCode, req.getProductCode())
                            .ne(Product::getStatus, 0));
            if (count > 0) throw new BusinessException("产品编码已存在: " + req.getProductCode());
        }
        if (req.getParentId() != null) {
            Product parent = productMapper.selectById(req.getParentId());
            if (parent == null) throw new BusinessException("父产品不存在: " + req.getParentId());
        }
        Product product = new Product();
        BeanUtils.copyProperties(req, product);
        if (product.getStatus() == null) product.setStatus(1);
        if (product.getStockQuantity() == null) product.setStockQuantity(0);
        product.setCreatedBy(userId);
        product.setUpdatedBy(userId);
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("产品编码已存在: " + req.getProductCode());
        }
        return product.getId();
    }

    @Override
    public void update(Long id, ProductReqDTO req) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("产品不存在");
        if (StringUtils.isNotBlank(req.getProductCode())
                && !req.getProductCode().equals(product.getProductCode())) {
            Long count = productMapper.selectCount(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getProductCode, req.getProductCode())
                            .ne(Product::getStatus, 0));
            if (count > 0) throw new BusinessException("产品编码已存在: " + req.getProductCode());
        }
        if (req.getParentId() != null && !req.getParentId().equals(product.getParentId())) {
            Product parent = productMapper.selectById(req.getParentId());
            if (parent == null) throw new BusinessException("父产品不存在: " + req.getParentId());
        }
        BeanUtils.copyProperties(req, product);
        product.setUpdatedBy(SecurityUtils.getCurrentUserId());
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("产品不存在");
        // 如果有子产品（单品引用了该套装），解除引用
        Product update = new Product();
        update.setParentId(null);
        productMapper.update(update, new LambdaQueryWrapper<Product>().eq(Product::getParentId, id));
        product.setStatus(0);
        productMapper.updateById(product);
    }

    @Override
    public List<Product> listSets() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getProductType, "SET")
                .ne(Product::getStatus, 0)
                .orderByDesc(Product::getCreatedAt);
        return productMapper.selectList(wrapper);
    }

    @Override
    public List<Product> listChildren(Long parentId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getParentId, parentId)
                .ne(Product::getStatus, 0)
                .orderByDesc(Product::getCreatedAt);
        return productMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void updateChildren(Long parentId, List<Long> childIds) {
        Product parent = productMapper.selectById(parentId);
        if (parent == null) throw new BusinessException("父产品不存在");

        // 清除所有原有引用到此父产品的子产品
        Product clear = new Product();
        clear.setParentId(null);
        productMapper.update(clear, new LambdaQueryWrapper<Product>().eq(Product::getParentId, parentId));

        // 设置新的子产品
        if (childIds != null && !childIds.isEmpty()) {
            for (Long childId : childIds) {
                Product child = productMapper.selectById(childId);
                if (child == null) throw new BusinessException("子产品ID " + childId + " 不存在");
                child.setParentId(parentId);
                productMapper.updateById(child);
            }
        }
    }
}
