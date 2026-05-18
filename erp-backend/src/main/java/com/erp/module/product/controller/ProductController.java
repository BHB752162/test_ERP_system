package com.erp.module.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResult;
import com.erp.module.product.dto.ProductReqDTO;
import com.erp.module.product.entity.Product;
import com.erp.module.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Resource
    private ProductService productService;

    @GetMapping
    public ApiResponse<PageResult<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        IPage<Product> result = productService.listProducts(page, pageSize, keyword);
        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getById(@PathVariable Long id) {
        return ApiResponse.success(productService.getById(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody ProductReqDTO req) {
        Long id = productService.create(req);
        return ApiResponse.success(id);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ProductReqDTO req) {
        productService.update(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/sets")
    public ApiResponse<List<Product>> listSets() {
        return ApiResponse.success(productService.listSets());
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<Product>> listChildren(@PathVariable Long id) {
        return ApiResponse.success(productService.listChildren(id));
    }

    @PutMapping("/{id}/children")
    public ApiResponse<Void> updateChildren(@PathVariable Long id, @RequestBody List<Long> childIds) {
        productService.updateChildren(id, childIds);
        return ApiResponse.success();
    }
}
