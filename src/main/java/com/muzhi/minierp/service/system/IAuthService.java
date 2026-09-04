package com.muzhi.minierp.service.system;

import com.muzhi.minierp.model.LoginQuery;
import com.muzhi.minierp.model.LoginUser;

/**
 * <p>
 * {@code IAuthService}: IAuthService
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/1/20 10:25
 */
public interface IAuthService {

    /**
     * 登录
     * @author Mr.Muzhi
     * @since 2026/1/20 10:29
     * @param query 参数
     * @return 返回结果
     */
    LoginUser login(LoginQuery query);

    /**
     * 获取当前登陆用户
     * @author Mr.Muzhi
     * @since 2026/1/20 10:29
     * @return 返回结果
     */
    LoginUser getCurrentUser();

}
