package com.bossien.flowtreatmentservice.listener;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.flowtreatmentservice.handler.MessageContainerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author gb
 */
public class FlowTreatmentConsumerListener {

    MessageContainerFactory messageContainer ;

    public  FlowTreatmentConsumerListener(){
         messageContainer
                = new MessageContainerFactory()
                .loadHandlerRule();
    }
    ObjectMapper objectMapper = new ObjectMapper();

    public void onMessage(Message message, Channel channel) {
        byte[] body = message.getBody();
        try {
            String content = new String(body, "utf-8");
            ComputingResourceModel computingResourceModel =
                    objectMapper.readValue(content, ComputingResourceModel.class);
           messageContainer.onNext(computingResourceModel);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
