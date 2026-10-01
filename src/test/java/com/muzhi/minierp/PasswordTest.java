package com.muzhi.minierp;

import com.muzhi.minierp.security.SecurityUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <p>
 * {@code PasswordTest}: TODO
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/10/1 16:13
 */
public class PasswordTest {

    public static void main(String[] args) {
        String password = SecurityUtils.passwordSaltAddition("muzhi", "muzhi@123");
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        System.out.println(passwordEncoder.encode(password));

    }
}
