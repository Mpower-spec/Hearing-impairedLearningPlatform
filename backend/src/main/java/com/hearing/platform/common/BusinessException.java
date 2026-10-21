package com.hearing.platform.common;

import org.springframework.http.HttpStatus; // 复用 Spring 的 HTTP 状态码枚举

/**
 * 业务异常。
 * 运行时抛出，携带对应的 HTTP 状态码与提示信息。
 * 由 GlobalExceptionHandler 统一捕获并转成 ApiResponse。
 */
public class BusinessException extends RuntimeException { // 继承运行时异常，无需强制 try

    private final HttpStatus status; // 该异常对应的 HTTP 状态码

    /**
     * 指定状态码与消息的构造器。
     * @param status HTTP 状态码
     * @param message 错误提示
     */
    public BusinessException(HttpStatus status, String message) {
        super(message); // 消息交给父类 RuntimeException
        this.status = status; // 保存状态码
    }

    /**
     * 仅给消息的构造器，默认状态码为 400。
     * @param message 错误提示
     */
    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message); // 委托上面的构造器
    }

    /**
     * @return 该异常携带的 HTTP 状态码
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 快捷工厂：404 资源不存在。
     * @param message 错误提示
     * @return 状态码为 NOT_FOUND 的业务异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    /**
     * 快捷工厂：403 无权访问。
     * @param message 错误提示
     * @return 状态码为 FORBIDDEN 的业务异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }
}
