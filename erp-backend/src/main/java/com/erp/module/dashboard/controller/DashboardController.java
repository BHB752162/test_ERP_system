package com.erp.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.response.ApiResponse;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.order.entity.SalesOrder;
import com.erp.module.order.mapper.SalesOrderMapper;
import com.erp.module.product.entity.Product;
import com.erp.module.product.mapper.ProductMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("customerCount", customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().ne(Customer::getStatus, 0)));
        stats.put("productCount", productMapper.selectCount(
                new LambdaQueryWrapper<Product>().ne(Product::getStatus, 0)));
        stats.put("orderCount", salesOrderMapper.selectCount(null));
        return ApiResponse.success(stats);
    }
}
