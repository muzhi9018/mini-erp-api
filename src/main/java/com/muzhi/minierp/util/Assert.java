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
 * <p>条件命中时抛出业务异常；支持传入国际化编码和默认提示，由全局异常处理器按请求语言翻译。</p>
 *
 * @author Mr.Muzhi
 * @since 2023/10/26 14:11
 */
public final class Assert {

    private Assert() { }

    public static void isTrue(boolean expression, String message) {
        isTrue(expression, null, message);
    }

    public static void isTrue(boolean expression, String i18nCode, String message, Object... i18nArgs) {
        if (expression) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static void isFalse(boolean expression, String message) {
        isFalse(expression, null, message);
    }

    public static void isFalse(boolean expression, String i18nCode, String message, Object... i18nArgs) {
        isTrue(!expression, i18nCode, message, i18nArgs);
    }

    public static void isNull(Object object, String message) {
        isNull(object, (String) null, message);
    }

    public static void isNull(Object object, String i18nCode, String message, Object... i18nArgs) {
        if (object == null) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static <T, R> void isNull(T object, Function<T, R> function, String message) {
        isNull(object, function, null, message);
    }

    public static <T, R> void isNull(T object, Function<T, R> function, String i18nCode, String message, Object... i18nArgs) {
        isNull(function, "assert.function-required", "断言函数不能为 null");
        isNull(object, i18nCode, message, i18nArgs);
        R result = function.apply(object);
        isNull(result, i18nCode, message, i18nArgs);
    }

    public static void isNotNull(Object object, String message) {
        isNotNull(object, null, message);
    }

    public static void isNotNull(Object object, String i18nCode, String message, Object... i18nArgs) {
        if (object != null) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static void isEmpty(CharSequence content, String message) {
        isEmpty(content, (String) null, message);
    }

    public static void isEmpty(CharSequence content, String i18nCode, String message, Object... i18nArgs) {
        if (StringUtils.isEmpty(content)) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static void isEmpty(Collection<?> coll, String message) {
        isEmpty(coll, (String) null, message);
    }

    public static void isEmpty(Collection<?> coll, String i18nCode, String message, Object... i18nArgs) {
        if (CollectionUtils.isEmpty(coll)) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static void isEmpty(Map<?, ?> map, String message) {
        isEmpty(map, (String) null, message);
    }

    public static void isEmpty(Map<?, ?> map, String i18nCode, String message, Object... i18nArgs) {
        if (MapUtils.isEmpty(map)) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static <T, R> void isEmpty(T object, Function<T, R> function, String message) {
        isEmpty(object, function, null, message);
    }

    public static <T, R> void isEmpty(T object, Function<T, R> function, String i18nCode, String message, Object... i18nArgs) {
        isNull(function, "assert.function-required", "断言函数不能为 null");
        isNull(object, i18nCode, message, i18nArgs);
        R result = function.apply(object);
        isNull(result, i18nCode, message, i18nArgs);
        if (result instanceof CharSequence) {
            isEmpty((CharSequence) result, i18nCode, message, i18nArgs);
        }
        if (result instanceof Collection<?>) {
            isEmpty((Collection<?>) result, i18nCode, message, i18nArgs);
        }
    }

    public static void isNotEmpty(CharSequence content, String message) {
        isNotEmpty(content, null, message);
    }

    public static void isNotEmpty(CharSequence content, String i18nCode, String message, Object... i18nArgs) {
        if (StringUtils.isNotEmpty(content)) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static void isNotEmpty(Collection<?> coll, String message) {
        isNotEmpty(coll, null, message);
    }

    public static void isNotEmpty(Collection<?> coll, String i18nCode, String message, Object... i18nArgs) {
        if (CollectionUtils.isNotEmpty(coll)) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

    public static void isNotEmpty(Map<?, ?> map, String message) {
        isNotEmpty(map, null, message);
    }

    public static void isNotEmpty(Map<?, ?> map, String i18nCode, String message, Object... i18nArgs) {
        if (MapUtils.isNotEmpty(map)) {
            throw new BusinessException(i18nCode, message, null, i18nArgs);
        }
    }

}
