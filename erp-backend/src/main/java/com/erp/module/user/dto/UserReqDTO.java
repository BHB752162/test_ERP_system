package com.erp.module.user.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserReqDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    private Integer status;
}
