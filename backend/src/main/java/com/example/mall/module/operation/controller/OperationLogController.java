package com.example.mall.module.operation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.operation.entity.OperationLog;
import com.example.mall.module.operation.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OperationLogController {

    private final OperationLogMapper operationLogMapper;

    @GetMapping
    public Result<PageResult<OperationLog>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operatorName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Page<OperationLog> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt);

        if (module != null && !module.isBlank()) wrapper.eq(OperationLog::getModule, module);
        if (action != null && !action.isBlank()) wrapper.eq(OperationLog::getAction, action);
        if (operatorName != null && !operatorName.isBlank()) wrapper.eq(OperationLog::getOperatorName, operatorName);

        IPage<OperationLog> result = operationLogMapper.selectPage(p, wrapper);
        return Result.success(new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize));
    }
}
