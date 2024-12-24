package com.kangaroohy.milo.service;

import com.kangaroohy.milo.configuration.MiloProperties;

import java.util.Map;

/**
 * 类 MiloConfigProvider 功能描述：<br/>
 *
 * @author hy
 * @version 0.0.1
 * @date 2024/12/24 10:39
 */
public interface MiloConfigProvider {

    Map<String, MiloProperties.Config> config();
}
