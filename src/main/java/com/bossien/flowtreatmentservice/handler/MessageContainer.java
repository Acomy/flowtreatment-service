package com.bossien.flowtreatmentservice.handler;


import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息容器
 *
 * @author gb
 */
public class MessageContainer<K extends MessageHandler<V>,V> {

    private CopyOnWriteArrayList<K> messageHandlers = new CopyOnWriteArrayList<>();

    private AtomicInteger point = new AtomicInteger(0);

    private MessageContainer() {
    }

    public static MessageContainer newMessageContainer() {
        return new MessageContainer();
    }

    public void onNext(V v) {
        try {
            messageHandlers.get(point.get()).process(v);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
           // resetPoint();
        }
    }

    private void resetPoint() {
        point.getAndIncrement();
        if (messageHandlers.size() == point.get()) {
            point.set(0);
        }

    }

    public MessageContainer addMessageHandler(K k) {
        messageHandlers.add(k);
        return this;
    }

}
