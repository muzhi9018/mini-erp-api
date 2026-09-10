package com.muzhi.minierp.entity.website;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.muzhi.minierp.vo.website.WebsiteProductI18nVO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 官网商品国际化
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
@Getter
@Setter
@ToString
@TableName("website_product_i18n")
public class WebsiteProductI18n implements Serializable {

    @Serial
    private static final long serialVersionUID = 402545445331950545L;

    /**
     * 主键
     */
    @TableId("id")
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 创建用户 id
     */
    @TableField(value = "create_user", fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 更新用户 id
     */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 是否已逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 商品 id（逻辑关联）
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 语言标识，例如 zh-CN、en-US
     */
    @TableField("locale")
    private String locale;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 商品卡片的次级名称，例如英文名称
     */
    @TableField("subtitle")
    private String subtitle;

    /**
     * 商品卡片摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 商品详情页 Hero 文案
     */
    @TableField("tagline")
    private String tagline;

    /**
     * 商品特点区域介绍文案
     */
    @TableField("feature_introduction")
    private String featureIntroduction;

    /**
     * 技术参数区域介绍文案
     */
    @TableField("specification_introduction")
    private String specificationIntroduction;

    /**
     * 应用场景区域介绍文案
     */
    @TableField("application_introduction")
    private String applicationIntroduction;

    /**
     * 案例展示区域介绍文案
     */
    @TableField("case_introduction")
    private String caseIntroduction;

    /**
     * 商品卡片封面图片地址
     */
    @TableField("cover_image_url")
    private String coverImageUrl;

    /**
     * 商品特点区域图片地址
     */
    @TableField("feature_image_url")
    private String featureImageUrl;
}
