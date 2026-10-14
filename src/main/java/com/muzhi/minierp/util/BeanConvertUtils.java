package com.muzhi.minierp.util;

import io.github.linpeilie.Converter;
import io.github.linpeilie.mapstruct.SpringContextUtils4Msp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * <p>
 * {@code BeanConvertUtils}: Bean 转化工具
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2021-02-24 11:50
 */
public final class BeanConvertUtils {

    public static <S, D> D convert(S source, Class<D> destinationClass) {
        if (source == null) {
            return null;
        }

        Converter converter = BeanConvertUtils.getConverter();
        return converter.convert(source, destinationClass);
    }

    public static <S, D> List<D> mapList(final List<S> source, final Class<D> destType) {
        final List<D> dest = new ArrayList<>(16);
        if (source != null && !source.isEmpty()) {
            Converter converter = BeanConvertUtils.getConverter();
            for (S element : source) {
                D convertedElement = converter.convert(element, destType);
                dest.add(convertedElement);
            }
        }
        return dest;
    }

    public static <S, D> Collection<D> mapCollection(final Collection<S> source, final Class<D> destType) {
        final Collection<D> dest = new ArrayList<>(16);
        if (source != null && !source.isEmpty()) {
            Converter converter = BeanConvertUtils.getConverter();
            for (S element : source) {
                D convertedElement = converter.convert(element, destType);
                dest.add(convertedElement);
            }
        }
        return dest;
    }

    private static Converter getConverter() {
        // 可执行 JAR 中的映射交由 Spring 查找，避免默认转换器只扫描文件目录。
        return SpringContextUtils4Msp.getBean(Converter.class);
    }

}

