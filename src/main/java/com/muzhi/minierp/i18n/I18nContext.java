package com.muzhi.minierp.i18n;

import com.muzhi.minierp.enums.SysLocale;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * <p>
 * {@code I18nContext}: I18n 上下文
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/13 17:13
 */
public final class I18nContext {


   private I18nContext() { }


    public static Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }

    public static SysLocale getCurrentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return SysLocale.ofLocaleOrDefault(locale);
    }
}
