package com.muzhi.minierp.config;

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
        localeResolver.setSupportedLocales(List.of(
                Locale.SIMPLIFIED_CHINESE,
                Locale.TRADITIONAL_CHINESE,
                Locale.US
        ));
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }
}
