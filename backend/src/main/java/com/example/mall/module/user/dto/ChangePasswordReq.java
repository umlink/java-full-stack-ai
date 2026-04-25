package com.example.mall.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordReq {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 8)
    private String newPassword;
}
