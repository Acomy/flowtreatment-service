package com.bossien.flowtreatmentservice.handler.child;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.flowtreatmentservice.cache.CacheManager;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.handler.BasicHandler;
import com.bossien.flowtreatmentservice.handler.MessageHandler;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 个人在全省排名
 * @author gb
 */
public class PersonInProvinceOrderHandler extends BasicHandler implements MessageHandler<ComputingResourceModel>{
    @Override
    public void process(ComputingResourceModel computingResourceModel) {
        RedisTemplate redisTemplate = getRedisTemplate();
        Long companyId = computingResourceModel.getCompanyId();
        //redisTemplate.opsForZSet().add(, computingResourceModel.getUserId(), computingResourceModel.getScore());
    }
}
