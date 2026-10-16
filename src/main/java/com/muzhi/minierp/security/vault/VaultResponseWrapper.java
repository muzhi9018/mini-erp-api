package com.muzhi.minierp.security.vault;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * <p>
 * {@code VaultResponseWrapper}：让消息转换器设置的正文长度经过 Spring Session 的提交回调
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
public final class VaultResponseWrapper extends HttpServletResponseWrapper {

    public VaultResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setHeader(String name, String value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            long length = value == null ? -1 : Long.parseLong(value);
            super.setContentLengthLong(length);
        } else {
            super.setHeader(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            this.setHeader(name, value);
        } else {
            super.addHeader(name, value);
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            super.setContentLengthLong(value);
        } else {
            super.setIntHeader(name, value);
        }
    }

    @Override
    public void addIntHeader(String name, int value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            super.setContentLengthLong(value);
        } else {
            super.addIntHeader(name, value);
        }
    }
}
