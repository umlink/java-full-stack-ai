package com.example.mall.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private Integer status;

    private String paymentMethod;

    private Date paidAt;

    private String addressSnapshot;

    private String remark;

    private String cancelReason;

    private Date canceledAt;

    private BigDecimal refundAmount;

    private Date refundedAt;

    private Date shippedAt;

    private Date completedAt;

    private Integer isFlashSale;

    private Date createdAt;

    private Date updatedAt;
}
