package com.example.mall.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReq {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度应在2-32字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9]+$", message = "仅支持字母、数字、中文")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少8位")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
    private String email;
}
