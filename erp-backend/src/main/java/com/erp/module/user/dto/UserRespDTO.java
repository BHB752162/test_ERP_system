package com.erp.module.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRespDTO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private Long createdBy;
    private Long updatedBy;
    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
