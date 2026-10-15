package com.muzhi.minierp.controller.vault;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

/**
 * <p>
 * 数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Controller
@RequestMapping("/vaultSessionRevocationOutbox")
public class VaultSessionRevocationOutboxController {

}
