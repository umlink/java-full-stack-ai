package com.example.mall.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String phone;

    private String email;

    private String avatar;

    /**
     * 角色: 1普通用户 2运营 3管理员
     */
    private Integer role;

    /**
     * 状态: 1正常 0禁用
     */
    @TableLogic(value = "1", delval = "0")
    private Integer status;

    private Integer loginAttempts;

    private Date lockUntil;

    private Date createdAt;

    private Date updatedAt;
}
