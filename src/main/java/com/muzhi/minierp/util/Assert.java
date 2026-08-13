package com.muzhi.minierp.util;

import com.muzhi.minierp.exception.BusinessException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;


import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>
 * {@code Assert}: 断言
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2023/10/26 14:11
 */
public final class Assert {

    private Assert() { }

    public static void isTrue(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(message);
        }
    }
    public static void isFalse(boolean expression, String message) {
        isTrue(!expression, message);
    }

    public static void isNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }

    public static <T, R> void isNull(T object, Function<T, R> function, String message) {
        isNull(function, "断言函数不能为 null");
        isNull(object, message);
        R result = function.apply(object);
        isNull(result, message);
    }

    public static void isNotNull(Object object, String message) {
        if (object != null) {
            throw new BusinessException(message);
        }
    }

    public static void isEmpty(CharSequence content, String message) {
        if (StringUtils.isEmpty(content)) {
            throw new BusinessException(message);
        }
    }

    public static void isEmpty(Collection<?> coll, String message) {
        if (CollectionUtils.isEmpty(coll)) {
            throw new BusinessException(message);
        }
    }

    public static void isEmpty(Map<?, ?> map, String message) {
        if (MapUtils.isEmpty(map)) {
            throw new BusinessException(message);
        }
    }

    public static <T, R> void isEmpty(T object, Function<T, R> function, String message) {
        isNull(function, "断言函数不能 null");
        isNull(object, message);
        R result = function.apply(object);
        isNull(result, message);
        if (result instanceof CharSequence) {
            isEmpty((CharSequence) result, message);
        }
        if (result instanceof Collection<?>) {
            isEmpty((Collection<?>) result, message);
        }
    }

    public static void isNotEmpty(CharSequence content, String message) {
        if (StringUtils.isNotEmpty(content)) {
            throw new BusinessException(message);
        }
    }

    public static void isNotEmpty(Collection<?> coll, String message) {
        if (CollectionUtils.isNotEmpty(coll)) {
            throw new BusinessException(message);
        }
    }

    public static void isNotEmpty(Map<?, ?> map, String message) {
        if (MapUtils.isNotEmpty(map)) {
            throw new BusinessException(message);
        }
    }

}
