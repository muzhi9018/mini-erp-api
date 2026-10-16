package com.muzhi.minierp.vo.vault;

import lombok.Data;

/**
 * <p>
 * {@code VaultErrorVO}：过滤器与接口共用的公开错误信息
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
@Data
public class VaultErrorVO {

    /**
     * 机器可读错误码，参见 {@link com.muzhi.minierp.enums.VaultEnum.Error}
     */
    private String errorCode;

    /**
     * 本次请求追踪标识
     */
    private String requestId;
}
