package com.campusrelay.worker;

import com.campusrelay.shared.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerBeans {

    /**
     * worker 用独立的 workerId（默认 2），避免与在线服务（默认 1）生成重复 ID。
     * 分片准备版本 分库分表时改为启动时从 Redis 分配，彻底消除人工配错的可能。
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(@Value("${relay.worker-id:2}") long workerId) {
        return new SnowflakeIdGenerator(workerId);
    }
}
