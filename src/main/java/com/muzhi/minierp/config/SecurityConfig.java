package com.muzhi.minierp.config;

import com.muzhi.minierp.annotation.OpenApi;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.security.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   WebApplicationContext webApplicationContext,
                                                   JwtAuthenticationEntryPoint authenticationEntryPoint,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   UserAuthenticationProvider userAuthenticationProvider) throws Exception {
        Set<String> openApiPatterns = this.loadOpenApiPatterns(webApplicationContext);
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(openApiPatterns.toArray(new String[0])).permitAll();
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(userAuthenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                                            ObjectMapper objectMapper,
                                                            I18nHelper i18nHelper) {
        return new JwtAuthenticationFilter(jwtTokenProvider, objectMapper, i18nHelper);
    }

    @Bean
    public JwtAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper, I18nHelper i18nHelper) {
        return new JwtAuthenticationEntryPoint(objectMapper, i18nHelper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 获取开放 API Patterns
     * @author Mr.Muzhi
     * @since 2022/12/8 16:27
     * @return openApiPatterns
     */
    private Set<String> loadOpenApiPatterns(WebApplicationContext webApplicationContext) {
        Set<String> openApiPatterns = new HashSet<>(16);
        // 获取到所有 RequestMappingHandlerMapping
        ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider = webApplicationContext.getBeanProvider(RequestMappingHandlerMapping.class);
        // 遍历 handlerMappingProvider
        handlerMappingProvider.forEach(handlerMapping -> {
            // 获取到所有的请求方法
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
            // key => Mapping 信息 如：请求方式、请求路径; val => handlerMethod 信息就是对应的请求方法 也就是我们自己实现的 Controller 方法
            handlerMethods.forEach((mappingInfo, handlerMethod) -> {
                // 判断是否标记 OpenApi 注解
                Annotation openApi = handlerMethod.getMethodAnnotation(OpenApi.class);
                if (openApi != null) {
                    openApiPatterns.addAll(mappingInfo.getPatternValues());
                }
            });
        });
        return openApiPatterns;
    }
}
