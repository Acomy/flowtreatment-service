package com.bossien.flowtreatmentservice.handler;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.flowtreatmentservice.handler.child.HBRecordHandler;
import com.bossien.flowtreatmentservice.handler.child.RecordHandler;
import com.bossien.flowtreatmentservice.service.ApplicationContextHelper;
import com.bossien.flowtreatmentservice.utils.PlatformCode;
import com.bossien.flowtreatmentservice.utils.TimeManager;
import com.ctc.wstx.exc.WstxOutputException;
import com.sun.jersey.core.impl.provider.entity.XMLRootObjectProvider;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.context.ApplicationContext;

/**
 * 消息容器工厂类用于确定传递类型
 * @author gb
 */
public class MessageContainerFactory {

    MessageContainer<MessageHandler<ComputingResourceModel>,ComputingResourceModel> messageContainer;

    public MessageContainerFactory() {
        messageContainer = MessageContainer.newMessageContainer();
    }

    public MessageContainerFactory loadHandlerRule() {
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
        TimeManager timeManager = (TimeManager) applicationContext.getBean("timeManager");
        String code = timeManager.getCode();
        if(null !=code &&code.equals(PlatformCode.HB)){
          messageContainer.addMessageHandler(new HBRecordHandler());
        } else if(null !=code &&code.equals(PlatformCode.COUNTRY) ){
            messageContainer.addMessageHandler(new RecordHandler());
        }else if(null !=code &&code.equals(PlatformCode.YNJT)){
            messageContainer.addMessageHandler(new HBRecordHandler());
        }
        return this;
    }
    public MessageContainerFactory loadHandlerRule(MessageHandler... messageHandlers) {
        for (MessageHandler container : messageHandlers) {
            messageContainer.addMessageHandler(container);
        }
        return this;
    }
    public void onNext(ComputingResourceModel computingResourceModel) {
        messageContainer.onNext(computingResourceModel);
    }

    public MessageContainer get() {
        return messageContainer;
    }
}
