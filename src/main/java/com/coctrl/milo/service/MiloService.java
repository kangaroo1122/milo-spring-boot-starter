package com.coctrl.milo.service;

import com.coctrl.milo.configuration.MiloProperties;
import com.coctrl.milo.model.ReadOrWrite;
import com.coctrl.milo.runner.MiloClient;
import com.coctrl.milo.runner.MiloReadRunner;
import com.coctrl.milo.runner.MiloSubscriptionRunner;
import com.coctrl.milo.runner.MiloWriteRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author kangaroo hy
 * @date 2020/4/25
 * @desc milo-spring-boot-starter
 * @since 0.0.1
 */
@Service
public class MiloService {
    private MiloProperties properties;

    public MiloService() {
    }

    public MiloService(MiloProperties properties) {
        this.properties = properties;
    }

    public void writeToOpcUa(ReadOrWrite entity) {
        new MiloClient(new MiloWriteRunner(entity, properties)).run();
    }

    @SuppressWarnings("unchecked")
    public List<ReadOrWrite> readFromOpcUa(List<String> ids) {
        return (List<ReadOrWrite>) new MiloClient(new MiloReadRunner(ids, properties)).run();
    }

    public Object subscriptionFromOpcUa(List<String> ids) {
        return new MiloClient(new MiloSubscriptionRunner(ids, properties)).run();
    }
}
