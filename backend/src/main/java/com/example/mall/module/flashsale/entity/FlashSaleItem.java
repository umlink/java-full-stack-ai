package com.example.mall.module.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("flash_sale_item")
public class FlashSaleItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private Long productId;

    private BigDecimal flashPrice;

    private Integer flashStock;

    private Integer limitPerUser;

    /** 1启用 0禁用 */
    @TableLogic(value = "1", delval = "0")
    private Integer status;

    @Version
    private Integer version;

    private Date createdAt;
}
