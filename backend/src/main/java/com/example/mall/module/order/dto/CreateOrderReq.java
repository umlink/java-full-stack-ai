package com.example.mall.module.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderReq {

    @NotEmpty(message = "请选择要购买的商品")
    private List<Long> cartItemIds;

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    private String remark;
}
