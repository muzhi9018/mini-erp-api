package com.muzhi.minierp.security.vault;

import com.alibaba.fastjson2.JSON;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.vault.IVaultFailureAuditService;
import com.muzhi.minierp.vo.vault.VaultErrorVO;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.NestedExceptionUtils;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <p>
 * {@code VaultErrorWriter}：过滤器和 MVC 共用的安全错误响应，不回显输入和异常正文
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VaultErrorWriter {
    private final I18nHelper i18nHelper;

    private final ObjectProvider<IVaultFailureAuditService> audit;

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
        return this.result(request, response, error, failure);
    }

    public JsonResult<?> result(HttpServletRequest request, HttpServletResponse response, VaultEnum.Error error) {
        return this.result(request, response, error, null);
    }

    private JsonResult<?> result(HttpServletRequest request, HttpServletResponse response, VaultEnum.Error error,
            Exception failure) {
        if (response.isCommitted()) {
            throw new IllegalStateException("Vault response already committed");
        }
        response.resetBuffer();
        response.setContentLengthLong(-1);
        String requestId = this.requestId(request);
        this.logFailure(request, error, requestId, failure);
        audit.ifAvailable(recorder -> recorder.record(request, error));
        String message = i18nHelper.getMessage(error.getI18nCode(), error.getMessage(), request);
        VaultErrorVO detail = new VaultErrorVO();
        detail.setErrorCode(error.getMachineCode());
        detail.setRequestId(requestId);
        JsonResult<VaultErrorVO> result = JsonResult.error(error.getStatus(), message);
        result.setData(detail);
        response.setStatus(error.getStatus());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Request-ID", requestId);
        if (error == VaultEnum.Error.RATE_LIMITED) {
            response.setHeader("Retry-After", "60");
        }
        return result;
    }

    /**
     * 仅保留根异常类型和代码位置，避免异常正文夹带凭据、密文或数据库参数。
     */
    private void logFailure(HttpServletRequest request, VaultEnum.Error error, String requestId, Exception failure) {
        Throwable diagnostic = null;
        if (failure != null) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(failure);
            diagnostic = new Throwable(cause.getClass().getName());
            diagnostic.setStackTrace(cause.getStackTrace());
        }
        if (error.getStatus() >= 500) {
            log.error("密码库请求失败；method={}, path={}, status={}, errorCode={}, requestId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    error.getStatus(),
                    error.getMachineCode(),
                    requestId,
                    diagnostic
            );
        } else {
            log.warn("密码库请求被拒绝；method={}, path={}, status={}, errorCode={}, requestId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    error.getStatus(),
                    error.getMachineCode(),
                    requestId,
                    diagnostic
            );
        }
    }

    public void write(HttpServletRequest request, HttpServletResponse response, VaultEnum.Error error) throws IOException {
        JsonResult<?> result = this.result(request, response, error);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = JSON.toJSONString(result);
        response.getWriter().write(json);
    }

    public void writeException(HttpServletRequest request, HttpServletResponse response, Exception failure) throws IOException {
        JsonResult<?> result = this.handle(request, response, failure);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = JSON.toJSONString(result);
        response.getWriter().write(json);
    }

    public String requestId(HttpServletRequest request) {
        return VaultContexts.requestId(request);
    }
}
