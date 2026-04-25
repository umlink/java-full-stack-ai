package com.example.mall.module.user.dto;

import lombok.Data;

@Data
public class LoginVO {

    private String token;

    private UserVO user;
}
