package com.kangaroohy.milo.configuration;

import com.kangaroohy.milo.service.MiloClientManager;
import com.kangaroohy.milo.service.MiloConfigProvider;
import com.kangaroohy.milo.service.MiloService;
import com.kangaroohy.milo.service.MiloSubscriptionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * OPC UA client lifecycle configuration.
 *
 * <p>A pool is a poor fit for OPC UA because a client owns a Session and
 * subscriptions can live for the entire application lifetime. We keep one
 * connected client per configured endpoint instead.</p>
 *
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Configuration
@EnableConfigurationProperties(MiloProperties.class)
@ConditionalOnClass(MiloService.class)
@ConditionalOnProperty(prefix = MiloProperties.PREFIX, value = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MiloAutoConfiguration {

    private final MiloProperties properties;

    public MiloAutoConfiguration(MiloProperties properties) {
        this.properties = properties;
    }

    /** 创建并管理每个 endpoint 对应的长期 OPC UA 客户端。 */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public MiloClientManager miloClientManager(Optional<MiloConfigProvider> configProvider) {
        initConfig(configProvider);
        return new MiloClientManager(properties);
    }

    /** 创建统一的订阅复用和回调管理器。 */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public MiloSubscriptionManager miloSubscriptionManager(MiloClientManager clientManager) {
        return new MiloSubscriptionManager(clientManager, properties);
    }

    /** 创建对外提供读写、浏览和订阅 API 的服务。 */
    @Bean
    @ConditionalOnMissingBean(MiloService.class)
    public MiloService miloService(MiloClientManager clientManager,
                                   MiloSubscriptionManager subscriptionManager) {
        return new MiloService(clientManager, subscriptionManager, properties);
    }

    /**
     * 合并外部配置并校验默认 endpoint 和各项批量参数。
     *
     * @param configProvider 可选的外部配置提供器
     */
    private void initConfig(Optional<MiloConfigProvider> configProvider) {
        if (configProvider.isPresent()) {
            MiloConfigProvider provider = configProvider.get();
            if (provider.config() != null) {
                properties.getConfig().putAll(provider.config());
            }
            if (provider.primary() != null && properties.getConfig().containsKey(provider.primary())) {
                properties.setPrimary(provider.primary());
            }
        }
        if (properties.getConfig().isEmpty()) {
            throw new IllegalStateException("请配置 OPC UA 地址信息");
        }
        if (properties.getPrimary() == null || !properties.getConfig().containsKey(properties.getPrimary())) {
            properties.setPrimary(properties.getConfig().keySet().iterator().next());
            log.warn("The primary property is '{}'.", properties.getPrimary());
        }
        validatePositive(properties.getReadBatchSize(), "read-batch-size");
        validatePositive(properties.getWriteBatchSize(), "write-batch-size");
        validatePositive(properties.getSubscriptionBatchSize(), "subscription-batch-size");
        validatePositive(properties.getRequestTimeout(), "request-timeout");
        validatePositive(properties.getSubscriptionQueueSize(), "subscription-queue-size");
        validatePositive(properties.getCallbackThreads(), "callback-threads");
        validatePositive(properties.getCallbackQueueCapacity(), "callback-queue-capacity");
    }

    private void validatePositive(long value, String property) {
        if (value <= 0) {
            throw new IllegalArgumentException("kangaroohy.milo." + property + " 必须大于 0");
        }
    }
}
