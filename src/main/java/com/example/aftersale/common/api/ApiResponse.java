package com.example.aftersale.common.api;

public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "ok", data);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
