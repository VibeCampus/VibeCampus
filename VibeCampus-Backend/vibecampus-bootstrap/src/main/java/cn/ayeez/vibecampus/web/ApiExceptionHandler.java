package cn.ayeez.vibecampus.web;

import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全站 REST API 异常体格式：统一返回 {@code { "message": "..." }}，
 * 与前端 {@code VibeCampus-Frontend/src/api/index.js} 中
 * {@code error.response?.data?.message} 的读取方式一致。
 * <p>若不处理，Spring Boot 3/4 对 {@link ResponseStatusException} 常序列化为 Problem Details（如 {@code detail}），
 * 前端拿不到 {@code message}，用户侧表现为「错误文案不显示」。</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 将业务/框架抛出的 HTTP 状态异常转为简单 JSON，保留原 HTTP 状态码。
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String message = ex.getReason() != null && !ex.getReason().isBlank()
                ? ex.getReason()
                : "请求失败";
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    /**
     * {@code @Valid} 校验失败时，取第一条字段错误文案作为 {@code message}，HTTP 400。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数错误");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
