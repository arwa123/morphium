package de.caluga.morphium.messaging.jms;

import de.caluga.morphium.messaging.Messaging;

import javax.jms.JMSConsumer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;

public class Consumer implements JMSConsumer, de.caluga.morphium.messaging.MessageListener<JMSMessage> {
    @Override
    public String getMessageSelector() {
        return null;
    }

    @Override
    public MessageListener getMessageListener() throws JMSRuntimeException {
        return null;
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSRuntimeException {

    }

    @Override
    public Message receive() {
        return null;
    }

    @Override
    public Message receive(long timeout) {
        return null;
    }

    @Override
    public Message receiveNoWait() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public <T> T receiveBody(Class<T> c) {
        return null;
    }

    @Override
    public <T> T receiveBody(Class<T> c, long timeout) {
        return null;
    }

    @Override
    public <T> T receiveBodyNoWait(Class<T> c) {
        return null;
    }

    @Override
    public JMSMessage onMessage(Messaging msg, JMSMessage m) throws InterruptedException {
        return null;
    }
}
