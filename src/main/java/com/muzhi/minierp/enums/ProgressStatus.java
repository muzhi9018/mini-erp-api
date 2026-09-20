package com.muzhi.minierp.enums;

import lombok.Getter;

/**
 * <p>
 * {@code ProgressStatus}: 进度状态枚举
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2024/2/22 17:20
 */
@Getter
public enum ProgressStatus {

    /**
     * 初始化 成功
     */
    INITIATED("Initiated", "初始化"),

    /**
     * 活跃的
     */
    ACTIVE("Active", "活跃的"),

    /**
     * 已完成
     */
    COMPLETE("Complete", "完成"),

    /**
     * 失败
     */
    FAILED("Failed", "失败"),
    ;

    /**
     * 状态
     */
    private final String status;

    /**
     * 名称
     */
    private final String name;


    ProgressStatus(String status, String name) {
        this.status = status;
        this.name = name;
    }
}


