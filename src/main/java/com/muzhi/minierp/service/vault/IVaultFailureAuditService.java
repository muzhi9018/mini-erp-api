package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.enums.VaultEnum;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>
 * {@code IVaultFailureAuditService}：密码库失败请求审计
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-20
 */
public interface IVaultFailureAuditService {

    /**
     * 在独立事务中记录失败审计，同一请求仅记录一次。
     * 审计数据库故障不影响原错误响应。
     *
     * @param request 当前请求
     * @param error 密码库错误类型
     */
    void record(HttpServletRequest request, VaultEnum.Error error);
}
