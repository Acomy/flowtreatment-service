//package com.bossien.flowtreatmentservice.cache;
//
//import com.bossien.common.util.QueueUtils;
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.DirectExchange;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.boot.SpringBootConfiguration;
//import org.springframework.context.annotation.Bean;
//
///**
// * rabbitmq 只用于发送消息的队列配置
// *
// * @author gb
// */
//@SpringBootConfiguration
//public class RabbitmqProducerConfig {
//
//    private String DEFAULTEXANGE = "CALCULATION-EXCHANGE";
//
//    @Bean
//    public DirectExchange defaultExchange() {
//        return new DirectExchange(DEFAULTEXANGE);
//    }
//
//    @Bean
//    public Queue queue() {
//        return new Queue(QueueUtils.CALCULATION_QUEUE);
//    }
//
//    @Bean
//    public Binding binding() {
//        return BindingBuilder.bind(queue()).to(defaultExchange()).with(QueueUtils.CALCULATION_QUEUE.toUpperCase());
//    }
//
//    @Bean
//    public MessageConverter jsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//
//
//    @Bean
//    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
//        RabbitTemplate template = new RabbitTemplate(connectionFactory);
//        template.setMessageConverter(jsonMessageConverter());
//        return template;
//    }
//}
