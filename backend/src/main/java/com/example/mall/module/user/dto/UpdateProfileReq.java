package com.example.mall.module.user.dto;

import lombok.Data;

@Data
public class UpdateProfileReq {

    private String phone;

    private String email;

    private String avatar;
}
