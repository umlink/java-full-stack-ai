package com.example.mall.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_address")
public class UserAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String receiverName;

    private String receiverPhone;

    private String province;

    private String city;

    private String district;

    private String detail;

    /**
     * 是否默认地址: 1是 0否
     */
    private Boolean isDefault;

    /**
     * 1正常 0删除
     */
    @TableLogic(value = "1", delval = "0")
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
