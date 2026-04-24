package com.muzhi.minierp.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * {@code LoginQuery}: 登录参数对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/1/16 15:31
 */
@Data
public class LoginQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 2872539028161200814L;

    private String username;

    private String password;
}
