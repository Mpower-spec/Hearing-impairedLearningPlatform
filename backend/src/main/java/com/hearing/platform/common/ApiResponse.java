package com.hearing.platform.common;

/**
 * 统一响应体。
 * 所有接口都返回该结构：code + message + data。
 * 用 record 实现，天然自带 getter、equals、toString。
 * @param code 业务/HTTP 状态码
 * @param message 提示信息
 * @param data 实际数据载荷
 * @param <T> 数据类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 成功响应，默认消息 "OK"。
     * @param data 业务数据
     * @return 状态码 200 的响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    /**
     * 成功响应，自定义消息。
     * @param message 成功提示
     * @param data 业务数据
     * @return 状态码 200 的响应
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 失败响应，data 为空。
     * @param code 错误码
     * @param message 错误提示
     * @return 不含数据的错误响应
     */
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
