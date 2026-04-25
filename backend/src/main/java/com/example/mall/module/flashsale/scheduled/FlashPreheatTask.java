package com.example.mall.module.flashsale.scheduled;

import com.example.mall.module.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlashPreheatTask {

    private final FlashSaleService flashSaleService;

    /**
     * 每 30 秒检查即将开始的活动并预热
     */
    @Scheduled(fixedRate = 30000)
    public void preheat() {
        flashSaleService.autoUpdateEventStatus();
    }
}
