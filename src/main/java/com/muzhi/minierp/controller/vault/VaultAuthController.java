package com.muzhi.minierp.controller.vault;

import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.*;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.security.vault.*;
import com.muzhi.minierp.service.vault.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * <p>
 * {@code VaultAuthController}：独立账号登录、Passkey MFA 和受限身份恢复入口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@RestController
@RequestMapping("/vault-auth")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultAuthController {
    private final IVaultIdentityService identities;

    private final VaultBrowserSession browser;

    private final VaultChallengeStore challenges;

    @GetMapping("/options")
    public JsonResult<Map<String, Object>> options(HttpServletRequest request, HttpServletResponse response) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        VaultSession session = principal instanceof VaultSession value ? value : null;
        Map<String, Object> result = browser.view(session, request, response);
        if (session != null) {
            VaultAccount account = identities.checkSession(session);
            result.put("credentialRevision", account.getCredentialRevision());
            result.put("recoveryRequired", account.getRecoveryRequired());
        }
        return JsonResult.success(result);
    }

    @PostMapping("/login")
    public JsonResult<Map<String, Object>> login(@RequestBody LoginQuery query, HttpServletRequest request, HttpServletResponse response) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(query == null || query.getUsername() == null || query.getUsername().isBlank()
                || query.getUsername().length() > 128 || query.getPassword() == null || query.getPassword().isEmpty()
                || query.getPassword().length() > 1024);
        challenges.rate("login-account", query.getUsername(), 10);
        challenges.rate("login-source", request.getRemoteAddr(), 60);
        VaultSession session = identities.login(query);
        Map<String, Object> result = browser.save(session, request, response);
        return JsonResult.success(result);
    }

    @PostMapping("/passkeys/register/options")
    public JsonResult<Map<String, Object>> registrationOptions(HttpServletRequest request) {
        VaultSession session = VaultContexts.current();
        this.limit(session, request);
        Map<String, Object> result = identities.options(session, VaultEnum.ChallengePurpose.REGISTER);
        return JsonResult.success(result);
    }

    @PostMapping("/passkeys/register/verify")
    public JsonResult<Boolean> register(@RequestBody VaultProtocol.Registration registration, HttpServletRequest request) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(registration == null || registration.label() == null || registration.label().isBlank() || registration.label().length() > 128);
        VaultProtocolValidator.credential(registration.credential(), true);
        VaultSession session = VaultContexts.current();
        this.limit(session, request);
        identities.register(session, registration);
        return JsonResult.success(true);
    }

    @PostMapping("/passkeys/authenticate/options")
    public JsonResult<Map<String, Object>> authenticationOptions(HttpServletRequest request) {
        VaultSession session = VaultContexts.current();
        this.limit(session, request);
        Map<String, Object> result = identities.options(session, VaultEnum.ChallengePurpose.AUTHENTICATE);
        return JsonResult.success(result);
    }

    @PostMapping("/passkeys/authenticate/verify")
    public JsonResult<Map<String, Object>> authenticate(@RequestBody VaultProtocol.Credential credential, HttpServletRequest request, HttpServletResponse response) {
        VaultProtocolValidator.credential(credential, false);
        VaultSession session = VaultContexts.current();
        this.limit(session, request);
        VaultSession verified = identities.authenticate(session, credential);
        Map<String, Object> result = browser.save(verified, request, response);
        return JsonResult.success(result);
    }

    @PostMapping("/recovery-codes/issue")
    public JsonResult<Map<String, Object>> issueRecoveryCodes(@RequestBody VaultProtocol.CredentialCommand command, HttpServletRequest request) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(command == null);
        VaultCodec.version(command.expectedCredentialRevision());
        VaultSession session = VaultContexts.current();
        this.limit(session, request);
        Map<String, Object> result = identities.issueRecoveryCodes(session, command.expectedCredentialRevision());
        return JsonResult.success(result);
    }

    @PostMapping("/recovery-codes/confirm")
    public JsonResult<Map<String, Object>> confirmRecoveryCodes(@RequestBody VaultProtocol.RecoveryConfirmation confirmation,
            HttpServletRequest request, HttpServletResponse response) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(confirmation == null);
        VaultCodec.uuid(confirmation.batchId());
        VaultCodec.version(confirmation.expectedCredentialRevision());
        VaultSession session = VaultContexts.current();
        this.limit(session, request);
        VaultSession verified = identities.confirmRecoveryCodes(session, confirmation);
        Map<String, Object> result = browser.save(verified, request, response);
        return JsonResult.success(result);
    }

    @PostMapping("/mfa-recovery/verify")
    public JsonResult<Map<String, Object>> recover(@RequestBody VaultProtocol.RecoveryInput input,
            HttpServletRequest request, HttpServletResponse response) {
        VaultEnum.Error.MFA_RECOVERY_INVALID.rejectIf(input == null || input.code() == null);
        VaultSession session = VaultContexts.current();
        this.limit(session, request);
        try {
            VaultCodec.decode(input.code(), 32, 32);
        } catch (com.muzhi.minierp.exception.BusinessException failure) {
            throw VaultEnum.Error.MFA_RECOVERY_INVALID.exception();
        }
        VaultSession restricted = identities.recover(session, input.code());
        Map<String, Object> result = browser.save(restricted, request, response);
        return JsonResult.success(result);
    }

    @GetMapping("/passkeys")
    public JsonResult<List<Map<String, Object>>> passkeys() {
        List<Map<String, Object>> keys = identities.passkeys(VaultContexts.current());
        return JsonResult.success(keys);
    }

    @PostMapping("/passkeys/revoke")
    public JsonResult<Boolean> revoke(@RequestBody VaultProtocol.CredentialCommand command,
            HttpServletRequest request, HttpServletResponse response) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(command == null);
        VaultCodec.decode(command.credentialId(), 1, 1024);
        VaultCodec.version(command.expectedCredentialRevision());
        VaultSession session = VaultContexts.current();
        identities.revokePasskey(session, command);
        browser.clear(request, response, true);
        return JsonResult.success(true);
    }

    @GetMapping("/reauth")
    public JsonResult<Map<String, Object>> reauth(HttpServletRequest request, HttpServletResponse response) {
        VaultSession session = VaultContexts.current();
        Map<String, Object> result = browser.view(session, request, response);
        result.put("optionsEndpoint", "/vault-auth/passkeys/authenticate/options");
        return JsonResult.success(result);
    }

    @PostMapping("/logout")
    public JsonResult<Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
        VaultSession session = VaultContexts.current();
        browser.clear(request, response, false);
        identities.logout(session);
        return JsonResult.success(true);
    }

    private void limit(VaultSession session, HttpServletRequest request) {
        challenges.rate("credential-account", session.ownerId().toString(), 20);
        challenges.rate("credential-source", request.getRemoteAddr(), 60);
    }
}
