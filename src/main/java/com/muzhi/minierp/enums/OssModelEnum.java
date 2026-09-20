package com.muzhi.minierp.enums;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;

/**
 * <p>
 * {@code OssModelEnum}: Oss 模塊枚舉
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/18 16:36
 */
@Getter
public enum OssModelEnum {


    /**
     * 官网商品图片
     */
    WEBSITE_PRODUCT("官网商品", "WEBSITE_PRODUCT", "website/product"),

    /**
     * 官网商品分类图片
     */
    WEBSITE_PRODUCT_CATEGORY("官网商品分类", "WEBSITE_PRODUCT_CATEGORY", "website/product-category"),

    ;

    /**
     * 名称
     */
    private final String name;

    /**
     * 模块
     */
    private final String model;

    /**
     * 前綴
     */
    private final String prefix;

    OssModelEnum(String name, String model, String prefix) {
        this.name = name;
        this.model = model;
        this.prefix = prefix;
    }


    public static OssModelEnum ofModel(String model) {
        for (OssModelEnum ossModelEnum : OssModelEnum.values()) {
            if (ossModelEnum.getModel().equals(model)) {
                return ossModelEnum;
            }
        }
        return null;
    }

    /**
     * 生成对象存储路径，使用唯一文件名并保留原文件扩展名。
     *
     * @param id 业务记录 ID
     * @param filename 原始文件名
     * @return 对象存储路径
     */
    public String builderOssKey(long id, String filename) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String date = format.format(System.currentTimeMillis());
        String extension = StringUtils.getFilenameExtension(filename);
        String objectFilename = String.valueOf(id);
        if (StringUtils.hasLength(extension)) {
            objectFilename += "." + extension;
        }
        return this.prefix + "/" + date + "/" + objectFilename;
    }
}
