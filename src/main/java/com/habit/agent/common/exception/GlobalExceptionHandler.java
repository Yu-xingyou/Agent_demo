package com.habit.agent.common.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.ConstraintViolationException;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.result.Result;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

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
     * 方法参数校验失败（@Validated + @NotNull 等注解触发，如 @PathVariable/@RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("方法参数校验失败: {}", errors);
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

    /**
     * 数据库完整性约束冲突（如唯一键重复、外键约束等）
     * 兜底处理，避免遗漏的约束异常暴露为 500 系统异常。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        log.warn("数据完整性冲突: {}", msg);
        // 重复键冲突优先映射为"重复目标"业务错误，便于前端友好提示
        if (msg.contains("Duplicate") || msg.contains("duplicate") || msg.contains("UNIQUE")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.error(AgentConstants.CODE_DUPLICATE_GOAL, "数据重复，请检查是否已存在相同记录"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error(AgentConstants.CODE_DB_ERROR, "数据冲突，请检查输入是否合法"));
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

    // ===== 404 静态资源不存在（如 favicon.ico） =====

    /**
     * 静态资源不存在（浏览器自动请求 favicon.ico 等）
     * 不记录 ERROR 日志，直接返回 404
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.debug("静态资源不存在: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // ===== 500 系统异常 =====

    /**
     * 兜底：未知系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleSystem(Exception ex,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        log.error("系统异常", ex);
        if (response.isCommitted()) {
            log.warn("响应已提交，无法返回错误JSON");
            return null;
        }
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
