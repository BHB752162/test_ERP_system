package com.erp.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.response.ApiResponse;
import com.erp.module.binding.entity.CustomerSalesAccountBinding;
import com.erp.module.binding.mapper.CustomerSalesAccountBindingMapper;
import com.erp.module.customer.entity.Customer;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.order.entity.SalesOrder;
import com.erp.module.order.mapper.SalesOrderMapper;
import com.erp.module.product.entity.Product;
import com.erp.module.product.mapper.ProductMapper;
import com.erp.module.salesaccount.entity.SalesAccountUserBinding;
import com.erp.module.salesaccount.mapper.SalesAccountUserBindingMapper;
import com.erp.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SalesOrderMapper salesOrderMapper;

    @Resource
    private SalesAccountUserBindingMapper salesAccountUserBindingMapper;

    @Resource
    private CustomerSalesAccountBindingMapper customerSalesAccountBindingMapper;

    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Long>> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        boolean isAdmin = "ADMIN".equals(roleCode);

        List<Long> accountIds = Collections.emptyList();
        if (!isAdmin && userId != null) {
            accountIds = salesAccountUserBindingMapper.selectList(
                    new LambdaQueryWrapper<SalesAccountUserBinding>()
                            .eq(SalesAccountUserBinding::getUserId, userId))
                    .stream().map(SalesAccountUserBinding::getSalesAccountId)
                    .collect(Collectors.toList());
        }

        Map<String, Long> stats = new HashMap<>();

        // 顾客总数: 非 ADMIN 只看自己绑定的销售账户下的顾客
        if (!isAdmin && accountIds.isEmpty()) {
            stats.put("customerCount", 0L);
        } else {
            LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<Customer>()
                    .ne(Customer::getStatus, 0);
            if (!isAdmin) {
                List<Long> customerIds = customerSalesAccountBindingMapper.selectList(
                        new LambdaQueryWrapper<CustomerSalesAccountBinding>()
                                .in(CustomerSalesAccountBinding::getSalesAccountId, accountIds))
                        .stream().map(CustomerSalesAccountBinding::getCustomerId)
                        .distinct().collect(Collectors.toList());
                if (customerIds.isEmpty()) {
                    stats.put("customerCount", 0L);
                } else {
                    customerWrapper.in(Customer::getId, customerIds);
                    stats.put("customerCount", customerMapper.selectCount(customerWrapper));
                }
            } else {
                stats.put("customerCount", customerMapper.selectCount(customerWrapper));
            }
        }

        // 产品总数: 所有人一样
        stats.put("productCount", productMapper.selectCount(
                new LambdaQueryWrapper<Product>().ne(Product::getStatus, 0)));

        // 订单总数: 非 ADMIN 只看自己绑定的销售账户下的订单
        if (!isAdmin && accountIds.isEmpty()) {
            stats.put("orderCount", 0L);
        } else {
            LambdaQueryWrapper<SalesOrder> orderWrapper = new LambdaQueryWrapper<>();
            if (!isAdmin) {
                orderWrapper.in(SalesOrder::getSalesAccountId, accountIds);
            }
            stats.put("orderCount", salesOrderMapper.selectCount(orderWrapper));
        }

        return ApiResponse.success(stats);
    }
}
