package com.muzhi.minierp;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

/**
 * <p>
 * {@code CodeGenerator}: 代码生成器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/4/23 16:27
 */
@Slf4j
public class CodeGenerator {

    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:postgresql://192.168.1.199:5432/mini_erp", "root", "postgres@root@602")
                .globalConfig(builder -> builder
                        .author("Mr.Muzhi")
                        .outputDir(Paths.get(System.getProperty("user.dir")) + "/src/main/java")
                        .commentDate("yyyy-MM-dd")
                )
                .packageConfig(builder -> builder
                        .parent("com.muzhi.minierp")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper.xml")
                )
                .strategyConfig(builder -> builder
                        .entityBuilder()
                        .enableLombok()
                )
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
