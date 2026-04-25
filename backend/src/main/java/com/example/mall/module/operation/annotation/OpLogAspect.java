package com.example.mall.module.operation.annotation;

import com.example.mall.module.operation.entity.OperationLog;
import com.example.mall.module.operation.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        String errorMsg = null;
        int result = 1; // 成功

        Object ret;
        try {
            ret = joinPoint.proceed();
        } catch (Throwable e) {
            result = 0;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            saveLog(opLog, result, errorMsg, duration);
        }

        return ret;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(OpLog opLog, int result, String errorMsg, long duration) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        OperationLog log = new OperationLog();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            log.setOperatorId((Long) auth.getPrincipal());
            log.setOperatorName(auth.getName());
            log.setOperatorRole(1); // default
        }

        log.setModule(opLog.module());
        log.setAction(opLog.action());
        log.setDescription(opLog.description());
        log.setResult(result);
        log.setErrorMsg(errorMsg);
        log.setDurationMs((int) duration);

        HttpServletRequest request = getRequest();
        if (request != null) {
            log.setRequestIp(request.getRemoteAddr());
            log.setRequestUrl(request.getRequestURI());
            log.setHttpMethod(request.getMethod());
        }

        operationLogMapper.insert(log);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
