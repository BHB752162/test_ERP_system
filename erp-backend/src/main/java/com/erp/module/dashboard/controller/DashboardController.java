package com.erp.module.dashboard.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.customer.mapper.CustomerMapper;
import com.erp.module.order.mapper.SalesOrderMapper;
import com.erp.module.product.mapper.ProductMapper;
import com.erp.module.wechat.mapper.SalesWechatMapper;
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

    @Resource
    private SalesWechatMapper salesWechatMapper;

    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("customerCount", customerMapper.selectCount(null));
        stats.put("productCount", productMapper.selectCount(null));
        stats.put("orderCount", salesOrderMapper.selectCount(null));
        stats.put("wechatCount", salesWechatMapper.selectCount(null));
        return ApiResponse.success(stats);
    }
}
