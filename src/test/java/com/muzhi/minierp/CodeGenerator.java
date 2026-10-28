package com.muzhi.minierp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import javax.lang.model.SourceVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * <p>
 * {@code CodeGenerator}: 代码生成器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/4/23 16:27
 */
public class CodeGenerator {

    /**
     * 根据控制台输入的表名和业务模块包名生成代码。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入表名，多个表名用英文逗号分隔（例如：website_product,website_product_i18n）：");
        String tableInput = scanner.nextLine();
        System.out.println("请输入业务模块包名（例如：system、website，支持 website.catalog 这样的多级包名）：");
        String moduleInput = scanner.nextLine();

        FastAutoGenerator generator = CodeGenerator.createGenerator(tableInput, moduleInput);
        generator.execute();
    }

    /**
     * 校验生成范围，并按项目的分层目录结构配置生成器。
     *
     * @param tableInput 以英文逗号分隔的表名
     * @param moduleInput 业务模块包名，相对于各分层包
     * @return 仅生成指定表、默认不覆盖已有文件的生成器
     */
    static FastAutoGenerator createGenerator(String tableInput, String moduleInput) {
        if (tableInput == null || tableInput.isBlank()) {
            throw new IllegalArgumentException("表名不能为空，必须明确指定需要生成的表");
        }
        List<String> tableNames = Arrays.stream(tableInput.split(",", -1))
                .map(String::trim)
                .distinct()
                .toList();
        Pattern tableNamePattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
        for (String tableName : tableNames) {
            boolean validTableName = tableNamePattern.matcher(tableName).matches();
            if (!validTableName) {
                throw new IllegalArgumentException("表名只能包含字母、数字和下划线，不能以数字开头或包含空项：" + tableName);
            }
        }

        if (moduleInput == null || moduleInput.isBlank()) {
            throw new IllegalArgumentException("业务模块包名不能为空，例如：system、website");
        }
        String moduleName = moduleInput.trim();
        if (!SourceVersion.isName(moduleName)) {
            throw new IllegalArgumentException("业务模块包名不合法：" + moduleName);
        }

        String workingDirectory = System.getProperty("user.dir");
        Path projectDirectory = Paths.get(workingDirectory);
        String javaOutputDirectory = projectDirectory.resolve("src/main/java").toString();
        String modulePath = moduleName.replace('.', '/');
        Path mapperDirectory = projectDirectory.resolve("src/main/resources/mapper");
        String xmlOutputDirectory = mapperDirectory.resolve(modulePath).toString();
        Map<OutputFile, String> outputPaths = Map.of(OutputFile.xml, xmlOutputDirectory);

        return FastAutoGenerator.create("jdbc:postgresql://192.168.1.199:5432/mini_erp", "root", "postgres@root@602")
                .globalConfig(builder -> {
                    builder.author("Mr.Muzhi");
                    builder.outputDir(javaOutputDirectory);
                    builder.commentDate("yyyy-MM-dd");
                    builder.disableOpenDir();
                })
                .packageConfig(builder -> {
                    builder.parent("com.muzhi.minierp");
                    builder.entity("entity." + moduleName);
                    builder.mapper("mapper." + moduleName);
                    builder.service("service." + moduleName);
                    builder.serviceImpl("service." + moduleName + ".impl");
                    builder.controller("controller." + moduleName);
                    builder.xml("mapper." + moduleName);
                    builder.pathInfo(outputPaths);
                })
                .strategyConfig(builder -> {
                    // 必须设置包含表，避免空范围退回全库生成。
                    builder.addInclude(tableNames);
                    builder.entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .logicDeleteColumnName("is_deleted")
                            .logicDeletePropertyName("deleted")
                            .addTableFills(
                                    new Column("gmt_create", FieldFill.INSERT),
                                    new Column("gmt_modified", FieldFill.INSERT_UPDATE),
                                    new Column("create_user", FieldFill.INSERT),
                                    new Column("update_user", FieldFill.INSERT_UPDATE)
                    );
                    builder.controllerBuilder().enableRestStyle();
                })
                .templateEngine(new VelocityTemplateEngine());
    }
}
