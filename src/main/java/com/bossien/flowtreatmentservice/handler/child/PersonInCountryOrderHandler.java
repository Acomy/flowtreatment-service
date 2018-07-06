package com.bossien.flowtreatmentservice.handler.child;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.flowtreatmentservice.handler.BasicHandler;
import com.bossien.flowtreatmentservice.handler.MessageHandler;
import org.springframework.data.redis.core.RedisTemplate;
import sun.util.resources.ga.LocaleNames_ga;

/**
 * 个人在全国的排名
 *
 * @author gb
 */
public class PersonInCountryOrderHandler extends BasicHandler implements MessageHandler<ComputingResourceModel> {
    @Override
    public void process(ComputingResourceModel computingResourceModel) {
        RedisTemplate redisTemplate = getRedisTemplate();
        redisTemplate.opsForZSet().add("country", computingResourceModel.getUserId(), computingResourceModel.getScore());

    }
}
