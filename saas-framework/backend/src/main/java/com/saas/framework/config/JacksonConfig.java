package com.saas.framework.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter[] DESERIALIZATION_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private static final DateTimeFormatter SERIALIZATION_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // 配置 JavaTimeModule
            JavaTimeModule javaTimeModule = new JavaTimeModule();

            // 自定义 LocalDateTime 反序列化器，支持多种格式
            javaTimeModule.addDeserializer(LocalDateTime.class, new MultiFormatLocalDateTimeDeserializer());

            // 自定义 LocalDateTime 序列化器
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(SERIALIZATION_FORMATTER));

            builder.modules(javaTimeModule);

            // 禁用将日期写为时间戳
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }

    /**
     * 支持多种格式的 LocalDateTime 反序列化器
     */
    private static class MultiFormatLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }

            String trimmed = text.trim();

            // 尝试所有支持的格式
            for (DateTimeFormatter formatter : DESERIALIZATION_FORMATTERS) {
                try {
                    return LocalDateTime.parse(trimmed, formatter);
                } catch (Exception ignored) {
                    // 继续尝试下一个格式
                }
            }

            // 如果所有格式都失败，尝试 ISO 标准解析
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception e) {
                throw new IOException("无法解析日期时间: '" + text + "'，支持格式: yyyy-MM-dd HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss");
            }
        }
    }
}