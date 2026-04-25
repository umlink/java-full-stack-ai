package com.example.mall.module.user.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String phone;

    private String email;

    private String avatar;

    private Integer role;

    private Integer status;

    private Date createdAt;
}
