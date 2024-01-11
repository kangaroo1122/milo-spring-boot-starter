package com.kangaroohy.milo.configuration;

import lombok.Data;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = MiloProperties.PREFIX)
public class MiloProperties {
    public static final String PREFIX = "kangaroohy.milo";

    /**
     * 是否启用组件
     */
    private Boolean enabled = true;

    /**
     * server 默认请求配置，不指定，则默认取 config中第一个
     */
    private String primary;

    /**
     * server 列表
     */
    private Map<String, Config> config = new LinkedHashMap<>();

    /**
     * 连接池配置
     */
    private Pool pool = new Pool();

    @Data
    public static class Config {

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
    }

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
