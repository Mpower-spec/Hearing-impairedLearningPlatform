package com.hearing.platform.common;

import jakarta.validation.ConstraintViolationException; // 参数校验约束异常
import org.slf4j.Logger; // 日志接口
import org.slf4j.LoggerFactory; // 日志工厂
import org.springframework.http.HttpStatus; // HTTP 状态码
import org.springframework.http.ResponseEntity; // 带状态码的响应实体
import org.springframework.web.bind.MethodArgumentNotValidException; // @Valid 请求体校验失败异常
import org.springframework.web.bind.MissingServletRequestParameterException; // 缺少请求参数异常
import org.springframework.web.bind.annotation.ExceptionHandler; // 声明异常处理方法
import org.springframework.web.bind.annotation.RestControllerAdvice; // 全局异常拦截器
import org.springframework.web.multipart.MaxUploadSizeExceededException; // 上传文件超限异常

/**
 * 全局异常处理器。
 * 拦截各控制器抛出的异常，统一转成 ApiResponse 返回。
 * 保证前端拿到的永远是 {code, message, data} 结构。
 */
@RestControllerAdvice // 同时具备 @ControllerAdvice 与 @ResponseBody
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class); // 本类日志对象

    /**
     * 处理业务异常。
     * 按异常自带的状态码与消息原样返回。
     * @param ex 捕获到的业务异常
     * @return 对应 HTTP 状态码 + 错误响应体
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getStatus().value(), ex.getMessage()));
    }

    /**
     * 处理参数类异常（校验失败、缺参、非法入参）。
     * 统一归为 400 请求错误。
     * @param ex 捕获到的请求异常
     * @return 400 + 具体字段错误提示
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        // 若为请求体校验失败，取第一个字段错误拼成 "字段: 提示"；否则直接用异常消息
        String message = ex instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream()
                    .findFirst().map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("请求参数不正确")
                : ex.getMessage();
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }

    /**
     * 处理上传文件超限异常。
     * @param ex 超限异常
     * @return 400 + 固定提示
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "上传文件超过大小限制"));
    }

    /**
     * 兜底处理其余未预期的异常。
     * 记录错误日志并返回 500。
     * @param ex 未知异常
     * @return 500 + 服务器内部错误
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected server error", ex); // 打印堆栈便于排查
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误：" + ex.getMessage()));
    }
}
