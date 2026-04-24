package com.muzhi.minierp.enums;

public enum HttpStatus {

    OK(200, "success"),
    BAD_REQUEST_PARAM_TYPE_ERROR(400, "参数类型错误"),
    BAD_REQUEST_MISSING_PARAM(400, "缺少必要参数"),
    UNAUTHORIZED(401, "授权已过期，请重新登录"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    TOO_MANY_REQUESTS(429, "服务器繁忙"),
    INTERNAL_SERVER_ERROR(500, "服务器异常"),
    SERVICE_UNAVAILABLE(503, "当前服务不可用");

    HttpStatus(int value, String reasonPhrase) {
        this.value = value;
        this.reasonPhrase = reasonPhrase;
    }


    private final int value;
    private final String reasonPhrase;

    public int value() {
        return this.value;
    }

    public String getReasonPhrase() {
        return this.reasonPhrase;
    }

    public static HttpStatus valueOf(int value) {
        HttpStatus[] values = HttpStatus.values();
        for (HttpStatus httpStatus : values) {
            if (httpStatus.value == value) {
                return httpStatus;
            }
        }
        return null;
    }


}
