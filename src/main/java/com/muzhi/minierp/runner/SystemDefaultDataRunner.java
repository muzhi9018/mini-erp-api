package com.muzhi.minierp.runner;

import com.muzhi.minierp.service.system.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * <p>
 * {@code SystemDefaultDataRunner}: 系统默认数据初始化 Runner
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/29 21:32
 */
@Component
@RequiredArgsConstructor
public class SystemDefaultDataRunner implements ApplicationRunner {

    private final ISysRoleService sysRoleService;

    @Override
    public void run(ApplicationArguments args) {
        sysRoleService.initializeDefaultRoles();
    }
}
