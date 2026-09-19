package com.kangaroohy.milo.configuration;

import lombok.Data;
import lombok.ToString;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * milo-spring-boot-starter 配置属性。
 *
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

    /** 单次 OPC UA Read 请求的最大点位数，默认 200。 */
    private int readBatchSize = 200;

    /** 单次 OPC UA Write 请求的最大点位数，默认 200。 */
    private int writeBatchSize = 200;

    /** 单次创建 MonitoredItem 的最大点位数，默认 200。 */
    private int subscriptionBatchSize = 200;

    /** OPC UA 请求超时时间，单位毫秒。 */
    private long requestTimeout = 5000L;

    /** 每个 MonitoredItem 的默认服务端队列长度。 */
    private int subscriptionQueueSize = 10;

    /** 订阅业务回调线程数。 */
    private int callbackThreads = 2;

    /** 订阅业务回调等待队列容量；队列满时在通知线程执行以形成背压。 */
    private int callbackQueueCapacity = 10000;

    /**
     * server 列表
     */
    private Map<String, Config> config = new LinkedHashMap<>();

    @Data
    @ToString(exclude = "password")
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
         * 可选消息安全模式。为空时在相同安全策略的 endpoint 中选择第一个。
         */
        private MessageSecurityMode securityMode;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;
    }

}
