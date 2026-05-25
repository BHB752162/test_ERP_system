package com.erp.common.constant;

public enum RoleCodeEnum {
    ADMIN("管理员"),
    SALES_PERSON("销售人员");

    private final String displayName;

    RoleCodeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
