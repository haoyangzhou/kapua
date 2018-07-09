/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.connector.activemq;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.proton.message.Message;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.broker.client.amqp.AmqpConsumer;
import org.eclipse.kapua.broker.client.amqp.ClientOptions;
import org.eclipse.kapua.commons.setting.system.SystemSetting;
import org.eclipse.kapua.connector.AmqpAbstractConnector;
import org.eclipse.kapua.connector.MessageContext;
import org.eclipse.kapua.converter.Converter;
import org.eclipse.kapua.converter.KapuaConverterException;
import org.eclipse.kapua.message.transport.TransportMessage;
import org.eclipse.kapua.message.transport.TransportMessageType;
import org.eclipse.kapua.message.transport.TransportQos;
import org.eclipse.kapua.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * AMQP ActiveMQ connector implementation
 *
 */
public class AmqpActiveMQConnector extends AmqpAbstractConnector<TransportMessage> {

    protected final static Logger logger = LoggerFactory.getLogger(AmqpActiveMQConnector.class);

    private final static String ACTIVEMQ_QOS = "ActiveMQ.MQTT.QoS";
    private final static String CLASSIFIER = SystemSetting.getInstance().getMessageClassifier();

    private AmqpConsumer consumer;

    public AmqpActiveMQConnector(Vertx vertx, ClientOptions clientOptions, Converter<byte[], TransportMessage> converter, Processor<TransportMessage> processor, @SuppressWarnings("rawtypes") Processor errorProcessor) {
        super(vertx, converter, processor, errorProcessor);
        context = vertx.getOrCreateContext();
        consumer = new AmqpConsumer(vertx, clientOptions, (delivery, message) -> {
                try {
                    super.handleMessage(new MessageContext<Message>(message));
                } catch (KapuaException e) {
                    logger.error("Exception while processing message: {}", e.getMessage(), e);
                    //TODO check how to have the message not acknowledged
                }
            });
    }

    @Override
    public void startInternal(Future<Void> startFuture) {
        connect(startFuture);
    }

    @Override
    public void stopInternal(Future<Void> stopFuture) {
        disconnect(stopFuture);
    }

    @Override
    protected void connect(Future<Void> connectFuture) {
        logger.info("Opening broker connection...");
        consumer.connect(connectFuture);
    }

    @Override
    protected void disconnect(Future<Void> disconnectFuture) {
        logger.info("Closing broker connection...");
        consumer.disconnect(disconnectFuture);
    }

    @Override
    protected Map<String, Object> getMessageParameters(Message message) throws KapuaConverterException {
        Map<String, Object> parameters = new HashMap<>();
        // extract original MQTT topic
        //TODO restore it once the ActiveMQ issue will be fixed
        //String mqttTopic = message.getProperties().getTo(); // topic://VirtualTopic.kapua-sys.02:42:AC:11:00:02.heater.data
        String mqttTopic = (String)message.getApplicationProperties().getValue().get("originalTopic");
        mqttTopic = mqttTopic.replace(".", "/");
        // process prefix and extract message type
        // FIXME: pluggable message types and dialects
        if (CLASSIFIER.equals(mqttTopic)) {
            parameters.put(Converter.MESSAGE_TYPE, TransportMessageType.CONTROL);
            mqttTopic = mqttTopic.substring(CLASSIFIER.length());
        } else {
            parameters.put(Converter.MESSAGE_TYPE, TransportMessageType.TELEMETRY);
        }
        parameters.put(Converter.MESSAGE_DESTINATION, mqttTopic);

        //extract connection id
        parameters.put(Converter.CONNECTION_ID, 
                Base64.getDecoder().decode((String)message.getApplicationProperties().getValue().get(Converter.HEADER_KAPUA_CONNECTION_ID)));

        // extract the original QoS
        Object activeMqQos = message.getApplicationProperties().getValue().get(ACTIVEMQ_QOS);
        if (activeMqQos != null && activeMqQos instanceof Integer) {
            int activeMqQosInt = (int) activeMqQos;
            switch (activeMqQosInt) {
            case 0:
                parameters.put(Converter.MESSAGE_QOS, TransportQos.AT_MOST_ONCE);
                break;
            case 1:
                parameters.put(Converter.MESSAGE_QOS, TransportQos.AT_LEAST_ONCE);
                break;
            case 2:
                parameters.put(Converter.MESSAGE_QOS, TransportQos.EXACTLY_ONCE);
                break;
            }
        }
        return parameters;
    }

}
