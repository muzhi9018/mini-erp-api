package com.muzhi.minierp.config;

import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>
 * {@code FastJsonWebMvcConfiguration}: 配置 SpringBoot 使用 fastJson 进行数据的请求接受和响应 (默认使用jackson)
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/8/14 11:11
 */
@Configuration
public class FastJsonWebMvcConfiguration implements WebMvcConfigurer {


    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setCharset(StandardCharsets.UTF_8);
        converter.setFastJsonConfig(config);
        List<MediaType> mediaTypes = List.of(MediaType.APPLICATION_JSON, MediaType.parseMediaType("application/*+json"));
        converter.setSupportedMediaTypes(mediaTypes);
        // 使用 withJsonConverter 方法替换默认的 JSON 转换器
        builder.withJsonConverter(converter);
    }
}
