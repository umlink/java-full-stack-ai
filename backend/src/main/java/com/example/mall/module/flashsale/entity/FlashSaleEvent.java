package com.example.mall.module.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("flash_sale_event")
public class FlashSaleEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Date startTime;

    private Date endTime;

    private Integer status;

    private String remark;

    private Date createdAt;

    private Date updatedAt;
}
