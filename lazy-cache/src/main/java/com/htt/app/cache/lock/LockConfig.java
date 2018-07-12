package com.htt.app.cache.lock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  分布式锁 配置类
 *  Created by sunnyLu on 2018/6/1.
 */
@Configuration
public class LockConfig {

    @Bean
    public LockSupport lockSupport(){
        return new LockSupport();
    }
}
