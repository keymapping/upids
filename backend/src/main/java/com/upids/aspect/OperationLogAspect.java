package com.upids.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upids.entity.OperationLog;
import com.upids.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面
 * AOP 记录所有关键操作到 operation_log 表
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 切点：Controller层所有方法
     */
    @Pointcut("execution(* com.upids.controller.*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();

            if (shouldLog(className, methodName)) {
                saveOperationLog(joinPoint, result, errorMsg, startTime);
            }
        }
    }

    /**
     * 判断是否需要记录日志
     */
    private boolean shouldLog(String className, String methodName) {
        // 排除健康检查等不需要记录的方法
        if (className.equals("HealthController")) {
            return false;
        }
        // 登录、注册、数据导入、上传、报告生成、预警变更、用户管理等需要记录
        return methodName.contains("login") ||
                methodName.contains("register") ||
                methodName.contains("import") ||
                methodName.contains("upload") ||
                methodName.contains("generate") ||
                methodName.contains("create") ||
                methodName.contains("update") ||
                methodName.contains("delete") ||
                methodName.contains("mark") ||
                methodName.contains("reset") ||
                methodName.contains("status");
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(ProceedingJoinPoint joinPoint, Object result,
                                  String errorMsg, long startTime) {
        try {
            HttpServletRequest request = getRequest();
            if (request == null) return;

            OperationLog operationLog = new OperationLog();
            Long userId = getCurrentUserId();
            operationLog.setUserId(userId);
            operationLog.setUsername(getCurrentUsername());
            operationLog.setModule(joinPoint.getTarget().getClass().getSimpleName());
            operationLog.setOperation(joinPoint.getSignature().getName());
            operationLog.setRequestUri(request.getRequestURI());
            operationLog.setRequestParams(getRequestParams(joinPoint, request));
            operationLog.setResult(errorMsg == null ? "success" : "error");
            operationLog.setErrorMsg(truncate(errorMsg, 500));
            operationLog.setIpAddress(getIpAddress(request));
            operationLog.setCreatedAt(LocalDateTime.now());

            operationLogMapper.insert(operationLog);

            log.debug("Operation log saved: {} - {} ({}ms)",
                    operationLog.getModule(), operationLog.getOperation(),
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("Failed to save operation log", e);
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            // 从 details 获取用户名
            Object details = authentication.getDetails();
            if (details instanceof String) {
                return (String) details;
            }
            // 如果 details 不是用户名，尝试 getName()
            String name = authentication.getName();
            if (name != null && !name.matches("\\d+")) {
                return name;
            }
        }
        return null;
    }

    /**
     * 获取请求参数：GET 请求记录 QueryString，POST/PUT 请求记录 Body
     */
    private String getRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        try {
            if (HttpMethod.GET.matches(request.getMethod())) {
                return request.getQueryString();
            }
            // POST/PUT 请求序列化方法参数（排除 HttpServletRequest 等不可序列化对象）
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg != null && !isWebObject(arg)) {
                        return truncate(objectMapper.writeValueAsString(arg), 1000);
                    }
                }
            }
            return request.getQueryString();
        } catch (Exception e) {
            log.debug("Failed to serialize request params", e);
            return null;
        }
    }

    /**
     * 判断是否为 Web 框架对象（不可直接序列化）
     */
    private boolean isWebObject(Object arg) {
        return arg instanceof HttpServletRequest ||
                arg instanceof jakarta.servlet.http.HttpServletResponse;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
