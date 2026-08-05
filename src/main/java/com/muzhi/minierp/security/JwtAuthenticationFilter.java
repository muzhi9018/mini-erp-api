package com.muzhi.minierp.security;

import com.muzhi.minierp.enums.HttpStatus;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final I18nHelper i18nHelper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper, I18nHelper i18nHelper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
        this.i18nHelper = i18nHelper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = this.resolveToken(request);
        if (StringUtils.hasText(token)) {
            if (!jwtTokenProvider.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                String message = this.i18nHelper.getMessage(HttpStatus.UNAUTHORIZED, request);
                JsonResult<?> result = JsonResult.error(HttpStatus.UNAUTHORIZED.value(), message);
                response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
                response.getWriter().write(objectMapper.writeValueAsString(result));
                return;
            }
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
