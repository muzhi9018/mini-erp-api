package com.muzhi.minierp.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * {@code Progress}: 进度条实体
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 16:37
 */
@Data
public class Progress implements Serializable {

    @Serial
    private static final long serialVersionUID = 5189600925229054767L;

    /**
     * 进度 id
     */
    private Long progressId;

    /**
     * 进度 title
     */
    private String title;

    /**
     * 内容长度
     */
    private Long contentLength;

    /**
     * 百分比
     */
    private Double percentage;

    /**
     * 状态
     */
    private String status;

}
