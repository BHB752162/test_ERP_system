package com.erp.module.product.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.product.dto.ProductCategoryReqDTO;
import com.erp.module.product.dto.ProductCategoryRespDTO;
import com.erp.module.product.service.ProductCategoryService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/product-categories")
public class ProductCategoryController {

    @Resource
    private ProductCategoryService productCategoryService;

    @GetMapping("/tree")
    public ApiResponse<List<ProductCategoryRespDTO>> listTree() {
        return ApiResponse.success(productCategoryService.listTree());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody ProductCategoryReqDTO req) {
        productCategoryService.create(req);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ProductCategoryReqDTO req) {
        productCategoryService.update(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productCategoryService.delete(id);
        return ApiResponse.success();
    }
}
