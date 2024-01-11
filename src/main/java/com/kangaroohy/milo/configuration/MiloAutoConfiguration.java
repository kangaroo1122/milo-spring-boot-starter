package com.kangaroohy.milo.configuration;

import com.kangaroohy.milo.pool.MiloConnectFactory;
import com.kangaroohy.milo.pool.MiloConnectPool;
import com.kangaroohy.milo.service.MiloService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.PreDestroy;
import java.time.Duration;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Configuration
@EnableConfigurationProperties(MiloProperties.class)
@ConditionalOnClass({MiloService.class, MiloConnectPool.class})
@ConditionalOnProperty(prefix = MiloProperties.PREFIX, value = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MiloAutoConfiguration {
    private final MiloProperties properties;

    private MiloConnectPool connectPool;

    public MiloAutoConfiguration(MiloProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "miloConnectPool")
    @ConditionalOnMissingBean({MiloConnectPool.class})
    protected MiloConnectPool miloConnectPool() {
        MiloConnectFactory objectFactory = new MiloConnectFactory(this.properties);
        //设置对象池的相关参数
        GenericKeyedObjectPoolConfig<OpcUaClient> poolConfig = new GenericKeyedObjectPoolConfig<>();

        MiloProperties.Pool pool = properties.getPool();
        // 最大空闲数
        poolConfig.setMaxIdlePerKey(pool.getMaxIdle());
        //最小空闲,设置为2表示池内至少存放2个空闲对象(当池内有2个空闲对象时调用borrowObject去对象时会立即调用创建对象的方法保证池内有2个空闲对象)
        poolConfig.setMinIdlePerKey(pool.getMinIdle());
        //最大总数 10
        poolConfig.setMaxTotal(pool.getMaxTotal());
        // 多久执行一次对象扫描，将无用的对象销毁，默认-1不扫描
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMinutes(1));
        // 在获取对象的时候检查有效性, 默认false
        poolConfig.setTestOnBorrow(true);
        // 在归还对象的时候检查有效性, 默认false
        poolConfig.setTestOnReturn(false);
        // 在空闲时检查有效性, 默认false
        poolConfig.setTestWhileIdle(false);
        // 最大等待时间， 默认的值为-1，表示无限等待。
        poolConfig.setMaxWait(Duration.ofSeconds(1));
        // 是否启用后进先出, 默认true
        poolConfig.setLifo(true);
        // 连接耗尽时是否阻塞, false立即抛异常,true阻塞直到超时, 默认true
        poolConfig.setBlockWhenExhausted(true);
        // 每次逐出检查时 逐出的最大数目 默认3
        poolConfig.setNumTestsPerEvictionRun(3);

        //一定要关闭jmx，不然springboot启动会报已经注册了某个jmx的错误
        poolConfig.setJmxEnabled(false);

        //新建一个对象池,传入对象工厂和配置
        connectPool = new MiloConnectPool(objectFactory, poolConfig);

        initPool(pool.getInitialSize(), pool.getMaxIdle());
        return connectPool;
    }

    @Bean
    @ConditionalOnMissingBean(MiloService.class)
    @DependsOn("miloConnectPool")
    public MiloService miloService(MiloConnectPool miloConnectPool) {
        return new MiloService(miloConnectPool, properties);
    }

    /**
     * 预先加载testObject对象到对象池中
     *
     * @param initialSize 初始化连接数
     * @param maxIdle     最大空闲连接数
     */
    private void initPool(int initialSize, int maxIdle) {
        if (initialSize <= 0) {
            return;
        }

        properties.getConfig().forEach((key, config) -> {
            for (int i = 0; i < Math.min(initialSize, maxIdle); i++) {
                try {
                    connectPool.addObject(config);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    @PreDestroy
    public void destroy() {
        if (connectPool != null) {
            connectPool.close();
            log.info("all opcUaClients are closed");
        }
    }
}
