package com.muzhi.minierp.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 附件信息表，用于记录上传到对象存储中的文件信息
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-16
 */
@Getter
@Setter
@ToString
@TableName("attachment")
public class Attachment implements Serializable {

    @Serial
    private static final long serialVersionUID = -6074463593593747808L;

    /**
     * 附件id，业务主键
     */
    @TableId("id")
    private Long id;

    /**
     * 存储类型，参见 {@link com.muzhi.minierp.enums.AttachmentEnum.StorageType}
     */
    @TableField("storage_type")
    private String storageType;

    /**
     * 对象存储桶名称，例如：mini-erp
     */
    @TableField("bucket_name")
    private String bucketName;

    /**
     * 对象在存储桶中的唯一key，例如：product/2026/09/abc.png
     */
    @TableField("object_key")
    private String objectKey;

    /**
     * 用户上传时的原始文件名称，例如：img_6232.png
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 文件mime类型，例如：image/png、image/jpeg、application/pdf
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 文件大小，单位：字节（byte）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 对象存储返回的etag，用于标识对象版本或内容特征，不建议作为严格文件哈希使用
     */
    @TableField("etag")
    private String etag;

    /**
     * 访问类型，参见 {@link com.muzhi.minierp.enums.AttachmentEnum.AccessType}；私有附件通过预签名 URL 访问
     */
    @TableField("access_type")
    private String accessType;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 最后修改时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 最后修改人id
     */
    @TableField(value = "update_user", fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /**
     * 创建人id
     */
    @TableField(value = "create_user", fill = FieldFill.INSERT)
    private Long createUser;

    /**
     * 逻辑删除标记：false-未删除，true-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean isDeleted;
}
