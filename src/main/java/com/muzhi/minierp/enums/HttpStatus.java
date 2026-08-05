package com.muzhi.minierp.enums;

public enum HttpStatus {

    OK(200, "http.status.ok", "success"),
    BAD_REQUEST_PARAM_TYPE_ERROR(400, "http.status.bad-request.param-type-error", "参数类型错误"),
    BAD_REQUEST_MISSING_PARAM(400, "http.status.bad-request.missing-param", "缺少必要参数"),
    UNAUTHORIZED(401, "http.status.unauthorized", "授权已过期，请重新登录"),
    FORBIDDEN(403, "http.status.forbidden", "没有访问权限"),
    NOT_FOUND(404, "http.status.not-found", "资源不存在"),
    METHOD_NOT_ALLOWED(405, "http.status.method-not-allowed", "方法不允许"),
    TOO_MANY_REQUESTS(429, "http.status.too-many-requests", "服务器繁忙"),
    INTERNAL_SERVER_ERROR(500, "http.status.internal-server-error", "服务器异常"),
    SERVICE_UNAVAILABLE(503, "http.status.service-unavailable", "当前服务不可用");

    HttpStatus(int value, String i18nCode, String reasonPhrase) {
        this.value = value;
        this.i18nCode = i18nCode;
        this.reasonPhrase = reasonPhrase;
    }


    private final int value;
    private final String i18nCode;
    private final String reasonPhrase;

    public int value() {
        return this.value;
    }

    public String getReasonPhrase() {
        return this.reasonPhrase;
    }

    public String getI18nCode() {
        return this.i18nCode;
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
