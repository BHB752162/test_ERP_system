package com.erp.common.constant;

public enum OrderStatusEnum {
    SAVED("已保存"),
    PENDING_APPROVAL("待审批"),
    APPROVED("已审批"),
    SHIPPED("已发货"),
    DELIVERED("已妥投"),
    REJECTED("已驳回"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String displayName;

    OrderStatusEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static boolean canTransition(String currentStatus, String targetAction) {
        switch (currentStatus) {
            case "SAVED":
                return "SUBMIT".equals(targetAction) || "CANCEL".equals(targetAction);
            case "PENDING_APPROVAL":
                return "APPROVE".equals(targetAction) || "REJECT".equals(targetAction) || "CANCEL".equals(targetAction);
            case "APPROVED":
                return "SHIP".equals(targetAction) || "REFUND".equals(targetAction);
            case "SHIPPED":
                return "DELIVER".equals(targetAction) || "REFUND".equals(targetAction);
            case "DELIVERED":
                return "REFUND".equals(targetAction);
            default:
                return false;
        }
    }

    public static String getTargetStatus(String currentStatus, String action) {
        switch (action) {
            case "SUBMIT":
                return "PENDING_APPROVAL";
            case "APPROVE":
                return "APPROVED";
            case "REJECT":
                return "REJECTED";
            case "CANCEL":
                return "CANCELLED";
            case "SHIP":
                return "SHIPPED";
            case "DELIVER":
                return "DELIVERED";
            case "REFUND":
                return "REFUNDED";
            default:
                throw new IllegalArgumentException("未知操作: " + action);
        }
    }
}
