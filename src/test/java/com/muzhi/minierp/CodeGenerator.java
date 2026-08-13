package com.muzhi.minierp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;

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
                        .enableTableFieldAnnotation()
                        .logicDeleteColumnName("is_deleted")
                        .logicDeletePropertyName("deleted")
                        .addTableFills(
                                new Column("gmt_create", FieldFill.INSERT),
                                new Column("gmt_modified", FieldFill.INSERT_UPDATE),
                                new Column("create_user", FieldFill.INSERT),
                                new Column("update_user", FieldFill.INSERT_UPDATE)
                        )
                )
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
