package com.erp.module.auth.dto;

import lombok.Data;

@Data
public class LoginRespDTO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
}
