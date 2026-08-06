package com.muzhi.minierp.model;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 * {@code LoginUserDeserializer}: 登录用户对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2021-03-04 09:37
 */
public class LoginUser extends User implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 6706569988256258964L;

    private final boolean enabled;

    private final boolean accountNonExpired;

    private final boolean credentialsNonExpired;

    private final boolean accountNonLocked;

    private final Collection<? extends GrantedAuthority> authorities;

    private  String accessToken;

    private String tokenType;


    public LoginUser(String username, String password, boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        this(username, password, enabled, enabled, enabled, enabled, authorities);
    }

    public LoginUser(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super.setUsername(username);
        super.setPassword(password);
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities == null ? new HashSet<>(16) : new HashSet<>(authorities);
    }


    @Override
    public void eraseCredentials() {
        super.setPassword(null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setUsername(String username) {
        // 重写父类方法，不允许修改用户名
    }

    @Override
    public void setPassword(String password) {
        // 重写父类方法，不允许修改密码
    }

    private static void getAVoid() {
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
