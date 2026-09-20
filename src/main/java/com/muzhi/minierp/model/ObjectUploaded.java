package com.muzhi.minierp.model;

import com.muzhi.minierp.client.listener.ProgressListener;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * {@code ObjectUploaded}: 对象 Uploaded
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 17:35
 */
@Getter
@Setter
public class ObjectUploaded implements Serializable {

    @Serial
    private static final long serialVersionUID = 4351571748242774341L;

    /**
     * 进度 id
     */
    private Long progressId;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * key
     */
    private String key;

    /**
     * 对象存储返回的 ETag
     */
    private String etag;

    /**
     * 进度监听器
     */
    private ProgressListener progressListener;
}


