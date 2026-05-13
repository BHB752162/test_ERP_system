package com.erp.common.constant;

public enum OrderStatusEnum {
    DRAFT("草稿"),
    PENDING_APPROVAL("待审批"),
    APPROVED("已通过"),
    REJECTED("已驳回"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String displayName;

    OrderStatusEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static boolean canTransition(String currentStatus, String targetAction) {
        switch (currentStatus) {
            case "DRAFT":
                return "SUBMIT".equals(targetAction) || "CANCEL".equals(targetAction);
            case "PENDING_APPROVAL":
                return "APPROVE".equals(targetAction) || "REJECT".equals(targetAction) || "CANCEL".equals(targetAction);
            case "APPROVED":
                return "COMPLETE".equals(targetAction);
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
            case "COMPLETE":
                return "COMPLETED";
            default:
                throw new IllegalArgumentException("未知操作: " + action);
        }
    }
}
