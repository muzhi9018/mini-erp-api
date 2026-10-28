package com.muzhi.minierp.config;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.security.vault.*;
import com.muzhi.minierp.service.vault.IVaultIdentityService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.*;

/**
 * <p>
 * {@code VaultSecurityConfig}：密码库专用安全链，不接受 ERP JWT
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Configuration
@EnableConfigurationProperties(VaultProperties.class)
public class VaultSecurityConfig {

    @Bean
    @Order(1)
    @ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain vaultDisabledChain(HttpSecurity http, VaultErrorWriter errors) throws Exception {
        http.securityMatcher(VaultPaths::matches)
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, failure) -> errors.write(request, response, VaultEnum.Error.UNAVAILABLE))
                        .accessDeniedHandler((request, response, failure) -> errors.write(request, response, VaultEnum.Error.UNAVAILABLE)));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
    public HttpSessionSecurityContextRepository vaultSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
    public HttpSessionCsrfTokenRepository vaultCsrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
    public SecurityFilterChain vaultChain(HttpSecurity http, VaultProperties properties, VaultErrorWriter errors,
            IVaultIdentityService identities, HttpSessionSecurityContextRepository contextRepository,
            HttpSessionCsrfTokenRepository csrfRepository) throws Exception {
        properties.validate();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        http.securityMatcher(VaultPaths::matches)
                .cors(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(contextRepository).requireExplicitSave(true))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/vault/auth/options", "/vault/auth/login").permitAll()
                        .requestMatchers("/vault/auth/**").authenticated()
                        .anyRequest().hasAuthority(VaultContexts.MFA_AUTHORITY))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, failure) -> errors.write(request, response, VaultEnum.Error.SESSION_EXPIRED))
                        .accessDeniedHandler((request, response, failure) -> errors.write(request, response,
                                failure instanceof CsrfException ? VaultEnum.Error.CSRF_REJECTED : VaultEnum.Error.MFA_REQUIRED)))
                .addFilterBefore(new VaultHttpFilter(properties, errors, identities), CsrfFilter.class);
        return http.build();
    }
}
