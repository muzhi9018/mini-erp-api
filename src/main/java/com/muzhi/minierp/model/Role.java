package com.muzhi.minierp.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * {@code Role}: 角色对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/10/26 14:59
 */
@Setter
@Getter
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 2781580856719071780L;

    private Long id;

    private String roleName;

    private String roleCode;
}
