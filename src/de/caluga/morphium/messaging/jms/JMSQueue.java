package de.caluga.morphium.messaging.jms;

import javax.jms.JMSException;
import javax.jms.Queue;

public class JMSQueue extends JMSDestination implements Queue {

    private String queueName;

    @Override
    public String getQueueName() throws JMSException {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}
