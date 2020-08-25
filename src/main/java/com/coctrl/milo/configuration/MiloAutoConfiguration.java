package com.coctrl.milo.configuration;

import com.coctrl.milo.service.MiloService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Configuration
@EnableConfigurationProperties(MiloProperties.class)
@ConditionalOnClass(MiloService.class)
@ConditionalOnProperty(prefix = "coctrl.milo", value = "enabled", matchIfMissing = true)
public class MiloAutoConfiguration {
    private final MiloProperties properties;

    public MiloAutoConfiguration(MiloProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(MiloService.class)
    public MiloService miloService() {
        return new MiloService(properties);
    }
}
