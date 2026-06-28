package com.somehow.work.prompthub.util;

import lombok.Data;

/**
 * 统一响应体
 * 所有 API 返回格式：{ code, message, data }
 */
@Data
public class Result<T> {

    /** 状态码：200-成功，其他-失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 返回数据 */
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ──────────── 成功 ────────────

    public static <T> Result<T> ok() {
        return new Result<>(200, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ──────────── 失败 ────────────

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }
}
