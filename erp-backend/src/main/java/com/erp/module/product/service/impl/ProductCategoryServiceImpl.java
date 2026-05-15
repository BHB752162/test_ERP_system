package com.erp.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.module.product.dto.ProductCategoryReqDTO;
import com.erp.module.product.dto.ProductCategoryRespDTO;
import com.erp.module.product.entity.ProductCategory;
import com.erp.module.product.mapper.ProductCategoryMapper;
import com.erp.module.product.mapper.ProductMapper;
import com.erp.module.product.service.ProductCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    @Resource
    private ProductMapper productMapper;

    @Override
    public List<ProductCategoryRespDTO> listTree() {
        List<ProductCategory> all = productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .ne(ProductCategory::getStatus, 0)
                        .orderByAsc(ProductCategory::getSortOrder));

        Map<Long, List<ProductCategory>> parentMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(ProductCategory::getParentId));

        return buildTree(all.stream().filter(c -> c.getParentId() == null).collect(Collectors.toList()), parentMap);
    }

    private List<ProductCategoryRespDTO> buildTree(List<ProductCategory> roots,
                                                    Map<Long, List<ProductCategory>> parentMap) {
        List<ProductCategoryRespDTO> result = new ArrayList<>();
        for (ProductCategory cat : roots) {
            ProductCategoryRespDTO dto = new ProductCategoryRespDTO();
            BeanUtils.copyProperties(cat, dto);
            List<ProductCategory> children = parentMap.get(cat.getId());
            if (children != null) {
                dto.setChildren(buildTree(children, parentMap));
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public void create(ProductCategoryReqDTO req) {
        ProductCategory category = new ProductCategory();
        BeanUtils.copyProperties(req, category);
        if (category.getStatus() == null) category.setStatus(1);
        if (category.getSortOrder() == null) category.setSortOrder(0);
        productCategoryMapper.insert(category);
    }

    @Override
    public void update(Long id, ProductCategoryReqDTO req) {
        ProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) throw new BusinessException("分类不存在");
        BeanUtils.copyProperties(req, category);
        productCategoryMapper.updateById(category);
    }

    @Override
    public void delete(Long id) {
        Long count = productCategoryMapper.selectCount(
                new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, id));
        if (count > 0) throw new BusinessException("该分类下有子分类，无法删除");

        Long productCount = productMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.erp.module.product.entity.Product>()
                        .eq(com.erp.module.product.entity.Product::getCategoryId, id));
        if (productCount > 0) throw new BusinessException("该分类下有产品，无法删除");

        ProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) throw new BusinessException("分类不存在");
        category.setStatus(0);
        productCategoryMapper.updateById(category);
    }
}
