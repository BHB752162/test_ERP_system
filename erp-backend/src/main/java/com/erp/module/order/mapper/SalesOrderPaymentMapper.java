package com.erp.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.order.entity.SalesOrderPayment;
import com.erp.module.paymentdashboard.dto.PaymentChannelSummaryDTO;
import com.erp.module.paymentdashboard.dto.PaymentExportRowDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesOrderPaymentMapper extends BaseMapper<SalesOrderPayment> {

    @Select("SELECT pct.type_name AS channelTypeName, COALESCE(SUM(sop.payment_amount), 0) AS totalAmount " +
            "FROM payment_channel_type pct " +
            "LEFT JOIN sales_order_payment sop ON sop.payment_channel_type_id = pct.id AND sop.created_at >= #{startTime} " +
            "WHERE pct.status != 0 " +
            "GROUP BY pct.id, pct.type_name " +
            "ORDER BY pct.sort_order ASC")
    List<PaymentChannelSummaryDTO> sumByChannelType(LocalDateTime startTime);

    @Select("SELECT pct.type_name AS channelTypeName, COALESCE(SUM(f.amount), 0) AS totalAmount " +
            "FROM payment_channel_type pct " +
            "LEFT JOIN (SELECT sop.payment_channel_type_id, sop.payment_amount AS amount " +
            "           FROM sales_order_payment sop " +
            "           JOIN sales_order so ON so.id = sop.order_id " +
            "           WHERE so.approved_at >= #{startTime} AND so.approved_at < #{endTime}) f " +
            "  ON f.payment_channel_type_id = pct.id " +
            "WHERE pct.status != 0 " +
            "GROUP BY pct.id, pct.type_name " +
            "ORDER BY pct.sort_order ASC")
    List<PaymentChannelSummaryDTO> sumByApprovalTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT so.order_no AS orderNo, so.status, so.submitted_at AS submittedAt, " +
            "  COALESCE(sa.display_name, sa.account_name) AS salesAccountName, " +
            "  pct.type_name AS channelTypeName, sop.payment_amount AS paymentAmount " +
            "FROM sales_order_payment sop " +
            "JOIN sales_order so ON so.id = sop.order_id " +
            "JOIN payment_channel_type pct ON pct.id = sop.payment_channel_type_id " +
            "LEFT JOIN sales_account sa ON sa.id = so.sales_account_id " +
            "WHERE so.approved_at >= #{startTime} AND so.approved_at < #{endTime} " +
            "ORDER BY so.order_no ASC, pct.sort_order ASC")
    List<PaymentExportRowDTO> listPaymentsByApprovalTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
