package com.muzhi.minierp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * 客户版报价单的开单方展示配置。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
@ConfigurationProperties(prefix = "quotation.issuer")
@Data
public class QuotationIssuerProperties {

    /**
     * 新建报价保存的开单方名称。
     */
    private String name = "建绿新材";

    /**
     * 新建报价保存的开单方联系方式。
     */
    private String contact;

    /**
     * 客户版实时展示的 Logo 地址。
     */
    private String logoUrl;
}
