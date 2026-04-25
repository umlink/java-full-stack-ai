package com.example.mall.module.flashsale.controller;

import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.flashsale.dto.CreateFlashEventReq;
import com.example.mall.module.flashsale.dto.FlashSaleEventVO;
import com.example.mall.module.flashsale.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/flash-sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
public class AdminFlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping
    public Result<PageResult<FlashSaleEventVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(flashSaleService.pageEvents(page, pageSize, status));
    }

    @GetMapping("/{id}")
    public Result<FlashSaleEventVO> detail(@PathVariable Long id) {
        return Result.success(flashSaleService.getEventDetail(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateFlashEventReq req) {
        flashSaleService.createEvent(req);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        flashSaleService.updateEventStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        flashSaleService.deleteEvent(id);
        return Result.success();
    }
}
