package com.muzhi.minierp.exception;

import com.muzhi.minierp.enums.HttpStatus;
import com.muzhi.minierp.model.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;

/**
 * <p>
 * {@code GlobalExceptionHandler}: 全局异常处理器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2021-08-03 12:36
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 500 未知异常
     */
    @ExceptionHandler(value = Exception.class)
    public JsonResult<?> exceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        this.logError(request, e);
        JsonResult<?> error = JsonResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        this.feignCallHandler(request, response, error);
        return error;
    }


    /**
     * 500 自定义业务异常
     */
    @ExceptionHandler(value = BusinessException.class)
    public JsonResult<?> businessHandler(HttpServletRequest request, HttpServletResponse response, BusinessException e) {
        this.logDebug(request, e);
        JsonResult<?> error = JsonResult.error(e.getMessage());
        this.feignCallHandler(request, response, error);
        return error;
    }


    /**
     * 400 参数类型不匹配
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public JsonResult<?> methodArgumentTypeMismatchHandler(HttpServletRequest request, HttpServletResponse response, MethodArgumentTypeMismatchException e) {
        this.logDebug(request, e);
        JsonResult<?> error = JsonResult.error(HttpStatus.BAD_REQUEST_PARAM_TYPE_ERROR.value(), HttpStatus.BAD_REQUEST_PARAM_TYPE_ERROR.getReasonPhrase() + "[" + e.getName() + "]");
        this.feignCallHandler(request, response, error);
        return error;
    }

    /**
     * 400 缺少参数
     */
    @ExceptionHandler({MissingServletRequestParameterException.class})
    public JsonResult<?> missingServletRequestParameterHandler(HttpServletRequest request, HttpServletResponse response, MissingServletRequestParameterException e) {
        this.logDebug(request, e);
        JsonResult<?> error = JsonResult.error(HttpStatus.BAD_REQUEST_MISSING_PARAM.value(), HttpStatus.BAD_REQUEST_MISSING_PARAM.getReasonPhrase() + "[" + e.getParameterName() + "]");
        this.feignCallHandler(request, response, error);
        return error;
    }

    /**
     * 403 没有访问权限
     */
    @ExceptionHandler(AccessDeniedException.class)
    public JsonResult<?> handleAccessDeniedException(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) {
        this.logDebug(request, e);
        JsonResult<Object> error = JsonResult.error(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
        this.feignCallHandler(request, response, error);
        return error;
    }

    /**
     * 404 请求资源不存在
     */
    @ExceptionHandler({NoHandlerFoundException.class})
    public JsonResult<?> noHandlerFoundHandler(HttpServletRequest request, HttpServletResponse response, NoHandlerFoundException e) {
        this.logDebug(request, e);
        JsonResult<Object> error = JsonResult.error(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
        this.feignCallHandler(request, response, error);
        return error;
    }

    /**
     * 405 请求 method 不匹配
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public JsonResult<?> httpRequestMethodNotSupportedHandler(HttpServletRequest request, HttpServletResponse response, HttpRequestMethodNotSupportedException e) {
        this.logDebug(request, e);
        JsonResult<Object> error = JsonResult.error(HttpStatus.METHOD_NOT_ALLOWED.value(), HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
        this.feignCallHandler(request, response, error);
        return error;
    }


    /**
     * Feign 调用处理器
     * @param request request
     * @param error   异常
     * @author Mr.Muzhi
     * @since 2023/1/18 15:33
     */
    private void feignCallHandler(HttpServletRequest request, HttpServletResponse response, JsonResult<?> error) {
        if (request.getServletPath().startsWith("/feign")) {
            response.setStatus(error.getCode());
        }
    }

    private void logError(HttpServletRequest request, Throwable throwable) {
        log.error("服务请求时发生异常；服务接口: {}", request.getRequestURI(), throwable);
    }

    private void logDebug(HttpServletRequest request, Throwable throwable) {
        log.debug("服务请求时发生异常；服务接口: {}", request.getRequestURI(), throwable);
    }


}
