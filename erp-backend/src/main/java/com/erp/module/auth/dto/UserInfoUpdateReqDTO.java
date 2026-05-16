package com.erp.module.auth.dto;

import lombok.Data;

@Data
public class UserInfoUpdateReqDTO {
    private String realName;
    private String phone;
    private String email;
}
