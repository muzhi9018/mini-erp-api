package com.muzhi.minierp.security.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * {@code VaultErrorWriter}：过滤器和 MVC 共用的安全错误响应，不回显输入和异常正文
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Component
@RequiredArgsConstructor
public class VaultErrorWriter {
    private final ObjectMapper objectMapper;

    private final I18nHelper i18nHelper;

    private final org.springframework.beans.factory.ObjectProvider<com.muzhi.minierp.service.vault.VaultFailureAudit> audit;

    public JsonResult<?> handle(HttpServletRequest request, HttpServletResponse response, Exception failure) {
        VaultEnum.Error error;
        if (failure instanceof BusinessException business) {
            error = VaultEnum.Error.ofKey(business.getI18nCode());
        } else if (failure instanceof AuthenticationException) {
            error = VaultEnum.Error.INVALID_CREDENTIALS;
        } else if (failure instanceof AccessDeniedException) {
            error = VaultEnum.Error.MFA_REQUIRED;
        } else if (failure instanceof org.springframework.dao.DataIntegrityViolationException) {
            error = VaultEnum.Error.VERSION_CONFLICT;
        } else if (failure instanceof DataAccessException) {
            error = VaultEnum.Error.UNAVAILABLE;
        } else if (failure instanceof HttpRequestMethodNotSupportedException) {
            error = VaultEnum.Error.METHOD_NOT_ALLOWED;
        } else if (failure instanceof HttpMediaTypeNotSupportedException) {
            error = VaultEnum.Error.UNSUPPORTED_MEDIA_TYPE;
        } else if (failure instanceof NoHandlerFoundException || failure instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
            error = VaultEnum.Error.NOT_FOUND;
        } else if (failure instanceof HttpMessageNotReadableException
                || failure instanceof ServletRequestBindingException
                || failure instanceof MethodArgumentTypeMismatchException) {
            error = VaultEnum.Error.INVALID_REQUEST;
        } else {
            error = VaultEnum.Error.INTERNAL_ERROR;
        }
        return this.result(request, response, error);
    }

    public JsonResult<?> result(HttpServletRequest request, HttpServletResponse response, VaultEnum.Error error) {
        if (response.isCommitted()) {
            throw new IllegalStateException("Vault response already committed");
        }
        response.resetBuffer();
        response.setContentLengthLong(-1);
        String requestId = this.requestId(request);
        audit.ifAvailable(recorder -> recorder.record(request, error));
        String message = i18nHelper.getMessage(error.getI18nCode(), error.getMessage(), request);
        JsonResult<Map<String, String>> result = JsonResult.error(error.getStatus(), message);
        result.setData(Map.of("errorCode", error.getMachineCode(), "requestId", requestId));
        response.setStatus(error.getStatus());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Request-ID", requestId);
        if (error == VaultEnum.Error.RATE_LIMITED) {
            response.setHeader("Retry-After", "60");
        }
        return result;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, VaultEnum.Error error) throws IOException {
        JsonResult<?> result = this.result(request, response, error);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), result);
    }

    public void writeException(HttpServletRequest request, HttpServletResponse response, Exception failure) throws IOException {
        JsonResult<?> result = this.handle(request, response, failure);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), result);
    }

    public String requestId(HttpServletRequest request) {
        return VaultContexts.requestId(request);
    }
}
