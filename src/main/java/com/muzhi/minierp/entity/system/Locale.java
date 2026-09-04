package com.muzhi.minierp.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 国际化语言
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
@Getter
@Setter
@ToString
@TableName("locale")
public class Locale implements Serializable {

    @Serial
    private static final long serialVersionUID = -2491063257685021986L;

    /**
     * id
     */
    @TableId("id")
    private Long id;

    /**
     * 国际化编码
     */
    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    /**
     * 本地名称
     */
    @TableField("native_name")
    private String nativeName;

    @TableField("enabled")
    private Boolean enabled;

    /**
     * 是否默认语言
     */
    @TableField("is_default")
    private Boolean isDefault;

    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
