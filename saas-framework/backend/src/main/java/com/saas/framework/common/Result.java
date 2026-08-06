package com.saas.framework.common;

import lombok.Data;

/**
 * 统一响应类
 * 所有 API 返回都使用此类包装
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {

    /** 状态码，与 HTTP 状态码一致 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    private Result() {}

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ---------- 成功响应 ----------

    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    // ---------- 失败响应 ----------

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(400, msg, null);
    }

    // ---------- 常用快捷方法 ----------

    public static <T> Result<T> unauth() {
        return new Result<>(401, "未登录或登录已过期", null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(403, "权限不足", null);
    }

    public static <T> Result<T> notFound() {
        return new Result<>(404, "资源不存在", null);
    }

    public static <T> Result<T> serverError() {
        return new Result<>(500, "服务器内部错误", null);
    }
}
