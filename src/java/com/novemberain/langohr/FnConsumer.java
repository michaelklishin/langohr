package com.novemberain.langohr;

import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;
import clojure.lang.RT;
import clojure.lang.Keyword;
import clojure.lang.IFn;

public final class FnConsumer extends DefaultConsumer {

    private final IFn handleConsumeOK, handleCancel, handleCancelOK, handleShutdownSignal, handleRecoverOK, handleDelivery;

    public static Channel asNonRecovering(Channel c) {
        if (c instanceof AutorecoveringChannel) {
            AutorecoveringChannel tmp = (AutorecoveringChannel) c;
            return tmp.getDelegate();
        } else
            return c;
    }

    public FnConsumer(Channel c, Map<Keyword, IFn> handlers) {

        super(asNonRecovering(c));

        handleConsumeOK = handlers.get(RT.keyword(null, "handle-consume-ok-fn"));
        handleCancel    = handlers.get(RT.keyword(null, "handle-cancel-fn"));
        handleCancelOK  = handlers.get(RT.keyword(null, "handle-cancel-ok-fn"));
        handleShutdownSignal = handlers.get(RT.keyword(null, "handle-shutdown-signal-fn"));
        handleRecoverOK = handlers.get(RT.keyword(null, "handle-recover-ok-fn"));
        handleDelivery  = handlers.get(RT.keyword(null, "handle-delivery-fn"));

    }

    @Override
    public void handleConsumeOk(String consumerTag) {
        if (handleConsumeOK != null)
            handleConsumeOK.invoke(consumerTag);
    }

    @Override
    public void handleCancelOk(String consumerTag) {
        if (handleCancelOK != null)
            handleCancelOK.invoke(consumerTag);
    }

    @Override
    public void handleCancel(String consumerTag) {
        if (handleCancel != null)
            handleCancel.invoke(consumerTag);
    }

    @Override
    public void handleRecoverOk(String consumerTag) {
        if (handleRecoverOK != null)
            handleRecoverOK.invoke(consumerTag);
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        if (handleShutdownSignal != null)
            handleShutdownSignal.invoke(consumerTag, sig);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {
        if (handleDelivery != null)
            handleDelivery.invoke(consumerTag, envelope, properties, body);
    }
}