package com.bossien.flowtreatmentservice.handler.child;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.flowtreatmentservice.handler.BasicHandler;
import com.bossien.flowtreatmentservice.handler.MessageHandler;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 个人在本公司的排名
 * @author gb
 */
public class PersonInCompanyOrderHandler extends BasicHandler implements MessageHandler<ComputingResourceModel>{
    @Override
    public void process(ComputingResourceModel computingResourceModel) {
        RedisTemplate redisTemplate = getRedisTemplate();
        Long companyId = computingResourceModel.getCompanyId();
        redisTemplate.opsForZSet().add(companyId, computingResourceModel.getUserId(), computingResourceModel.getScore());
        System.out.println("ScoreOrderHandler"+computingResourceModel.getScore());
    }
}
