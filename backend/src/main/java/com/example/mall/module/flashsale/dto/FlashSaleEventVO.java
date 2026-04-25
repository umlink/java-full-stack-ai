package com.example.mall.module.flashsale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class FlashSaleEventVO {

    private Long id;
    private String name;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private String remark;
    private Date createdAt;

    private List<FlashSaleItemVO> items;
}
