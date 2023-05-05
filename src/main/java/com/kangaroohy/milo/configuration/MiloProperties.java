package com.kangaroohy.milo.configuration;

import lombok.Data;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Data
@Component
@ConfigurationProperties(prefix = MiloProperties.PREFIX)
@Primary
public class MiloProperties {
    public static final String PREFIX = "kangaroohy.milo";

    /**
     * OPC UA地址
     */
    private String endpoint;

    /**
     * 安全策略
     */
    private SecurityPolicy securityPolicy = SecurityPolicy.None;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 连接池配置
     */
    private Pool pool = new Pool();

    @Data
    public static class Pool {
        /**
         * 最大空闲
         */
        private int maxIdle = 5;
        /**
         * 最大总数
         */
        private int maxTotal = 20;
        /**
         * 最小空闲
         */
        private int minIdle = 2;

        /**
         * 初始化连接数
         */
        private int initialSize = 3;
    }
}
