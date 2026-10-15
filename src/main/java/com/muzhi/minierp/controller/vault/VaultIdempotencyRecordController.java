package com.muzhi.minierp.controller.vault;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

/**
 * <p>
 * 业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Controller
@RequestMapping("/vaultIdempotencyRecord")
public class VaultIdempotencyRecordController {

}
