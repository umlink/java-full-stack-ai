package com.example.mall.module.flashsale.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CreateFlashEventReq {

    @NotBlank(message = "活动名称不能为空")
    private String name;

    @NotNull(message = "请选择开始时间")
    @Future(message = "开始时间必须在未来")
    private Date startTime;

    @NotNull(message = "请选择结束时间")
    private Date endTime;

    private String remark;

    private List<FlashItemReq> items;

    @Data
    public static class FlashItemReq {
        @NotNull(message = "请选择商品")
        private Long productId;

        private Long skuId;

        @NotNull(message = "请输入秒杀价")
        private java.math.BigDecimal flashPrice;

        @NotNull(message = "请输入秒杀库存")
        private Integer flashStock;

        @NotNull(message = "请输入每人限购数")
        private Integer limitPerUser;
    }
}
