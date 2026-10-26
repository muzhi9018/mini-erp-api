package com.muzhi.minierp.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * {@code User}: 用户对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2022/12/6 17:18
 */
@Setter
@Getter
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 4753806461567249178L;

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
      */
    private String realName;

    /**
     * 用户编号
     */
    private String userNo;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色列表
     */
    private List<Role> roles;

    /**
     * 权限码
     */
    private List<String> permissionCodes;


}
