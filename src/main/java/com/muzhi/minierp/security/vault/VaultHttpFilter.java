package com.muzhi.minierp.security.vault;

import com.muzhi.minierp.config.VaultProperties;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultSession;
import com.muzhi.minierp.model.vault.VaultProtocol;
import com.muzhi.minierp.service.vault.IVaultIdentityService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;

/**
 * <p>
 * {@code VaultHttpFilter}：精确 Origin、请求体上限和服务端身份代次防护
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@RequiredArgsConstructor
public class VaultHttpFilter extends OncePerRequestFilter {

    private final VaultProperties properties;

    private final VaultErrorWriter errors;

    private final IVaultIdentityService identities;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // Fastjson 使用 setHeader 设置长度，需要经过 Session 的 setContentLengthLong 提交钩子。
        response = new VaultResponseWrapper(response);
        byte[] body = null;
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Request-ID", errors.requestId(request));
        try {
            boolean unsafe = !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
            String origin = request.getHeader("Origin");
            VaultEnum.Error.ORIGIN_REJECTED.rejectIf((unsafe || origin != null) && !properties.getOrigin().equals(origin));
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof VaultSession session) {
                Instant now = Instant.now();
                boolean expired = session.loginAt().isBefore(now.minusSeconds(28800))
                        || (session.stage() == VaultEnum.Stage.PASSWORD && session.passwordAt().isBefore(now.minusSeconds(300)))
                        || (session.recovering() && session.recoveryUntil() != null && session.recoveryUntil().isBefore(now));
                if (expired) {
                    request.getSession().invalidate();
                    throw VaultEnum.Error.SESSION_EXPIRED.exception();
                }
                try {
                    identities.checkSession(session);
                } catch (com.muzhi.minierp.exception.BusinessException failure) {
                    request.getSession().invalidate();
                    throw failure;
                }
            }
            HttpServletRequest actual = request;
            if (unsafe) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (path.startsWith("/vault/api/")) {
                    // 客户端必须兼容既有账号内容与账号组，才能写入或提交轮换。
                    Enumeration<String> versions = request.getHeaders(VaultProtocol.CLIENT_SCHEMA_HEADER);
                    String expected = Integer.toString(VaultProtocol.ITEM_SCHEMA_VERSION);
                    VaultEnum.Error.CLIENT_UPGRADE_REQUIRED.rejectIf(!versions.hasMoreElements()
                            || !expected.equals(versions.nextElement()) || versions.hasMoreElements());
                    Enumeration<String> lifecycleVersions = request.getHeaders(VaultProtocol.CLIENT_LIFECYCLE_HEADER);
                    String lifecycleExpected = Integer.toString(VaultProtocol.LIFECYCLE_PROTOCOL_VERSION);
                    VaultEnum.Error.CLIENT_UPGRADE_REQUIRED.rejectIf(!lifecycleVersions.hasMoreElements()
                            || !lifecycleExpected.equals(lifecycleVersions.nextElement()) || lifecycleVersions.hasMoreElements());
                }
                String contentType = request.getContentType();
                VaultEnum.Error.UNSUPPORTED_MEDIA_TYPE.rejectIf(contentType == null
                        || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json"));
                int maximum = (request.getRequestURI().endsWith("/rotations/batch")
                        || request.getRequestURI().endsWith("/restores/start")
                        || request.getRequestURI().endsWith("/items/batch")) ? 1048576 : 65536;
                VaultEnum.Error.PAYLOAD_TOO_LARGE.rejectIf(request.getContentLengthLong() > maximum);
                body = request.getInputStream().readNBytes(maximum + 1);
                VaultEnum.Error.PAYLOAD_TOO_LARGE.rejectIf(body.length > maximum);
                actual = new BodyRequest(request, body);
            }
            chain.doFilter(actual, response);
        } catch (Exception failure) {
            if (response.isCommitted()) {
                throw new ServletException("Vault response interrupted");
            }
            errors.writeException(request, response, failure);
        } finally {
            if (body != null) {
                Arrays.fill(body, (byte) 0);
            }
        }
    }

    private static final class BodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private BodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(this.body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(this.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
