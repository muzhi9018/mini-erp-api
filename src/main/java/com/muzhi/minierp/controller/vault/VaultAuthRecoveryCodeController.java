package com.muzhi.minierp.controller.vault;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

/**
 * <p>
 * 仅用于重新绑定 Passkey 的一次性 MFA 恢复码摘要；与客户端解密恢复密钥 RK 完全分离 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Controller
@RequestMapping("/vaultAuthRecoveryCode")
public class VaultAuthRecoveryCodeController {

}
