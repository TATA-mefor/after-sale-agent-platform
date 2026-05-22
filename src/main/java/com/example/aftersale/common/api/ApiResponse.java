package com.example.aftersale.common.api;
/*
 * 统一 API 响应格式
 * 所有 Controller 都返回 ApiResponse<T>，其中 T 是具体的数据类型，比如 TicketResponse。
 * ApiResponse 包含三个字段：code（状态码）、message（提示信息）和 data（具体数据）。
 * 这样做的好处是：前端可以统一处理响应，无论成功还是失败，都能从 code 和 message 获取状态信息，从 data 获取具体数据。
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "ok", data);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
