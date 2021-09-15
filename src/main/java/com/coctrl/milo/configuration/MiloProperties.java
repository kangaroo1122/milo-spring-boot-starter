package com.coctrl.milo.configuration;

import lombok.Data;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "coctrl.milo")
public class MiloProperties {
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
