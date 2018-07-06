package com.bossien.flowtreatmentservice.handler;

/**
 * 流处理消息类
 * @author gb
 */
public interface MessageHandler<T> {

    void process(T t);
}
