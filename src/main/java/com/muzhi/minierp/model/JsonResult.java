package com.muzhi.minierp.model;

import com.muzhi.minierp.enums.HttpStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * {@code JsonResult}: 统一返回JSON对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2021-02-24 11:48
 */
@Getter
@Setter
public final class JsonResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 134865203495105918L;

    private T data;

    private int code;

    private String message;

    private boolean success;

    private JsonResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private JsonResult() {
        this.code = HttpStatus.OK.value();
        this.message = HttpStatus.OK.getReasonPhrase();
        this.success = true;
    }

    private JsonResult(T data) {
        this.data = data;
        this.code = HttpStatus.OK.value();
        this.message = HttpStatus.OK.getReasonPhrase();
        this.success = true;
    }

    private JsonResult(int code, boolean success) {
        this.code = code;
        this.success = success;
    }

    public JsonResult(T data, int code, boolean success) {
        this.data = data;
        this.code = code;
        this.success = success;
    }



    public static <T> JsonResult<T> success(T data) {
        return new JsonResult<>(data);
    }

    public static <T> JsonResult<T> success() {
        return new JsonResult<>();
    }

    public static <T> JsonResult<T> error(String message) {
        return new JsonResult<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public static <T> JsonResult<T> error(int code, String message) {
        return new JsonResult<>(code, message);
    }

    public static <T> JsonResult<T> error() {
        return new JsonResult<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }
}
