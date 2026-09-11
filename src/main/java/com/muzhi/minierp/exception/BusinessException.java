package com.muzhi.minierp.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * <p>
 * {@code BusinessException}: 自定义业务异常
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2021-02-24 11:50
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1333125273684654438L;

    protected final String message;

    @Getter
    private final String i18nCode;

    private final Object[] i18nArgs;

    public BusinessException(String message){
        this(null, message, null);
    }

    public BusinessException(String i18nCode, String message) {
        this(i18nCode, message, null);
    }

    public BusinessException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public BusinessException(Throwable cause) {
        this(null, cause.getMessage(), cause);
    }

    public BusinessException(String i18nCode, String message, Throwable cause, Object... i18nArgs) {
        super(message, cause);
        this.i18nCode = i18nCode;
        this.message = message;
        this.i18nArgs = i18nArgs == null ? new Object[0] : i18nArgs.clone();
    }

    public Object[] getI18nArgs() {
        return i18nArgs.clone();
    }

    @Override
    public String getMessage(){
        return message;
    }

}
