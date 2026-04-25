package com.example.mall.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Jackson 全局配置
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // 日期格式
            builder.dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            // 时区
            builder.timeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            // 关闭时间戳输出（避免输出默认时间戳格式）
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 自定义序列化：Long → String（防止前端 JS 精度丢失）
            builder.modules(new SimpleModule() {{
                addSerializer(Long.class, ToStringSerializer.instance);
                addSerializer(Long.TYPE, ToStringSerializer.instance);
            }});
        };
    }
}
