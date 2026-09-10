package com.muzhi.minierp.config;

import com.muzhi.minierp.enums.SysLocale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        // 当前版本支持 简体中文，繁体中文，英文
        List<Locale> locales = SysLocale.locales();
        localeResolver.setSupportedLocales(locales);
        localeResolver.setDefaultLocale(SysLocale.defaultLocale());
        return localeResolver;
    }
}
