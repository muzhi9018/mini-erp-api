package com.muzhi.minierp.i18n;

import com.muzhi.minierp.enums.HttpStatus;
import com.muzhi.minierp.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class I18nHelper {

    private final MessageSource messageSource;

    private final LocaleResolver localeResolver;

    public String getMessage(HttpStatus status, HttpServletRequest request) {
        return this.getMessage(status.getI18nCode(), status.getReasonPhrase(), request);
    }

    public String getMessage(HttpStatus status, Locale locale) {
        return this.getMessage(status.getI18nCode(), status.getReasonPhrase(), locale);
    }

    public String getMessage(String code, String defaultMessage, HttpServletRequest request) {
        return this.getMessage(code, defaultMessage, this.localeResolver.resolveLocale(request));
    }

    public String getMessage(String code, String defaultMessage, Locale locale) {
        return this.messageSource.getMessage(code, null, defaultMessage, locale);
    }

    public String getMessage(BusinessException businessException, HttpServletRequest request) {
        String i18nCode = businessException.getI18nCode();
        return this.getMessage(i18nCode, businessException.getMessage(), request);
    }


}
