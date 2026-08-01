package com.habit.agent.common.exception;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（子模块 2-2，19 个错误码映射）
 *
 * 统一捕获各类异常并转换为标准 Result 响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 400 参数校验异常 =====

    /**
     * 参数校验失败（@Valid 注解触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(AgentConstants.CODE_PARAM_ERROR, "参数校验失败: " + errors));
    }

    /**
     * 枚举值异常（类型转换失败）
     */
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleEnum(IllegalArgumentException ex) {
        log.warn("枚举值异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(AgentConstants.CODE_ENUM_ERROR, "枚举值不合法: " + ex.getMessage()));
    }

    // ===== 业务异常（400/404/409） =====

    /**
     * 业务异常（日期范围/记录不存在/重复目标等）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        int code = ex.getCode();
        HttpStatus status = resolveHttpStatus(code);
        log.warn("业务异常: code={}, message={}", code, ex.getMessage());
        return ResponseEntity.status(status).body(Result.error(code, ex.getMessage()));
    }

    // ===== 503 AI 调用异常 =====

    /**
     * AI 调用异常
     */
    @ExceptionHandler(AiCallException.class)
    public ResponseEntity<Result<Void>> handleAiCall(AiCallException ex) {
        log.error("AI调用异常: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(ex.getCode(), ex.getMessage()));
    }

    // ===== 500 系统异常 =====

    /**
     * 兜底：未知系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleSystem(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(AgentConstants.CODE_SYSTEM_ERROR, "系统内部错误: " + ex.getMessage()));
    }

    /**
     * 根据错误码推断 HTTP 状态码
     */
    private HttpStatus resolveHttpStatus(int code) {
        if (code >= 40000 && code < 40100) return HttpStatus.BAD_REQUEST;      // 400xx → 400
        if (code >= 40400 && code < 40500) return HttpStatus.NOT_FOUND;         // 404xx → 404
        if (code >= 40900 && code < 41000) return HttpStatus.CONFLICT;          // 409xx → 409
        if (code >= 50000 && code < 50100) return HttpStatus.INTERNAL_SERVER_ERROR; // 500xx → 500
        if (code >= 50300 && code < 50400) return HttpStatus.SERVICE_UNAVAILABLE;   // 503xx → 503
        return HttpStatus.BAD_REQUEST;
    }
}
