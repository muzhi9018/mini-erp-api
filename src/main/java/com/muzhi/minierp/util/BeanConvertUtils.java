package com.muzhi.minierp.util;

import io.github.linpeilie.Converter;

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

    private final static Converter converter;

    static {
        converter = new Converter();
    }

    public static <S, D> D convert(S source, Class<D> destinationClass) {
        if (source == null) {
            return null;
        }
        return converter.convert(source, destinationClass);
    }

    public static <S, D> List<D> mapList(final List<S> source, final Class<D> destType) {
        final List<D> dest = new ArrayList<>(16);
        if (source != null && !source.isEmpty()) {
            for (S element : source) {
                dest.add(converter.convert(element, destType));
            }
        }
        return dest;
    }

    public static <S, D> Collection<D> mapCollection(final Collection<S> source, final Class<D> destType) {
        final Collection<D> dest = new ArrayList<>(16);
        if (source != null && !source.isEmpty()) {
            for (S element : source) {
                dest.add(converter.convert(element, destType));
            }
        }
        return dest;
    }

}

