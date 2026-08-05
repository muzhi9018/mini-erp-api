package com.muzhi.minierp.security;

import com.muzhi.minierp.enums.HttpStatus;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private final I18nHelper i18nHelper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper, I18nHelper i18nHelper) {
        this.objectMapper = objectMapper;
        this.i18nHelper = i18nHelper;
    }
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        String message = this.i18nHelper.getMessage(HttpStatus.UNAUTHORIZED, request);
        JsonResult<?> result = JsonResult.error(HttpStatus.UNAUTHORIZED.value(),message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
