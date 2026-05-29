package com.erp.module.paymentdashboard.controller;

import com.erp.common.response.ApiResponse;
import com.erp.module.order.mapper.SalesOrderPaymentMapper;
import com.erp.module.paymentdashboard.dto.PaymentChannelSummaryDTO;
import com.erp.module.paymentdashboard.dto.PaymentDashboardRespDTO;
import com.erp.module.paymentdashboard.dto.PaymentExportRowDTO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PaymentDashboardController {

    @Resource
    private SalesOrderPaymentMapper salesOrderPaymentMapper;

    @GetMapping("/payment-dashboard/summary")
    public ApiResponse<PaymentDashboardRespDTO> getPaymentSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime sevenDaysAgo = now.minusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime thirtyDaysAgo = now.minusDays(30).toLocalDate().atStartOfDay();

        PaymentDashboardRespDTO resp = new PaymentDashboardRespDTO();
        resp.setToday(salesOrderPaymentMapper.sumByChannelType(todayStart));
        resp.setLast7Days(salesOrderPaymentMapper.sumByChannelType(sevenDaysAgo));
        resp.setLast30Days(salesOrderPaymentMapper.sumByChannelType(thirtyDaysAgo));
        return ApiResponse.success(resp);
    }

    @GetMapping("/payment-dashboard/by-approval-time")
    public ApiResponse<List<PaymentChannelSummaryDTO>> getByApprovalTime(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        return ApiResponse.success(salesOrderPaymentMapper.sumByApprovalTimeRange(startTime, endTime));
    }

    @GetMapping("/payment-dashboard/export-by-approval-time")
    public ResponseEntity<byte[]> exportByApprovalTime(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) throws Exception {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        List<PaymentExportRowDTO> rows = salesOrderPaymentMapper.listPaymentsByApprovalTimeRange(startTime, endTime);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("收款明细");

        // Create center-aligned style
        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row header = sheet.createRow(0);
        String[] headers = {"订单号", "状态", "提交审批时间", "销售账号", "支付渠道", "支付金额"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            header.getCell(i).setCellStyle(centerStyle);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < rows.size(); i++) {
            PaymentExportRowDTO r = rows.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getOrderNo());
            row.createCell(1).setCellValue(statusToChinese(r.getStatus()));
            row.createCell(2).setCellValue(r.getSubmittedAt() != null ? r.getSubmittedAt().format(fmt) : "");
            row.createCell(3).setCellValue(r.getSalesAccountName() != null ? r.getSalesAccountName() : "");
            row.createCell(4).setCellValue(r.getChannelTypeName());
            row.createCell(5).setCellValue(r.getPaymentAmount() != null ? r.getPaymentAmount().doubleValue() : 0);
            for (int j = 0; j < headers.length; j++) {
                row.getCell(j).setCellStyle(centerStyle);
            }
        }

        // Auto-size + padding
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // add padding to auto-sized width
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(width + 2048, 12 * 256));
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        String filename = "payment_export_" + startDate + "_to_" + endDate + ".xlsx";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return ResponseEntity.ok().headers(httpHeaders).body(bos.toByteArray());
    }

    private String statusToChinese(String status) {
        if (status == null) return "";
        switch (status) {
            case "SAVED": return "已保存";
            case "PENDING_APPROVAL": return "待审批";
            case "APPROVED": return "已审批";
            case "SHIPPED": return "已发货";
            case "DELIVERED": return "已妥投";
            case "REJECTED": return "已驳回";
            case "CANCELLED": return "已取消";
            case "REFUNDED": return "已退款";
            default: return status;
        }
    }
}
