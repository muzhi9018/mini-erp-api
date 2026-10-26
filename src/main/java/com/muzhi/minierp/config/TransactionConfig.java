package com.muzhi.minierp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>
 * 编程式事务模板配置，统一管理默认事务与密码库失败审计的独立事务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-26
 */
@Configuration(proxyBeanMethods = false)
public class TransactionConfig {

    /**
     * 默认事务模板，按 REQUIRED 传播规则开启或参与事务。
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /**
     * 失败审计专用事务模板，保证审计记录独立于业务事务提交。
     */
    @Bean
    @ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
    public TransactionTemplate vaultFailureAuditTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }
}
