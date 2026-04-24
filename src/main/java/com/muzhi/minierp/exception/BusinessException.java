package com.muzhi.minierp.exception;

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

    public BusinessException(String message){
        super(message);
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(cause);
        this.message = message;
    }

    public BusinessException(Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
    }

    @Override
    public String getMessage(){
        return message;
    }

}