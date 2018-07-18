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

import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.proton.message.Message;
import org.eclipse.kapua.apps.api.HealthCheckable;
import org.eclipse.kapua.broker.client.amqp.AmqpConsumer;
import org.eclipse.kapua.broker.client.amqp.ClientOptions;
import org.eclipse.kapua.commons.setting.system.SystemSetting;
import org.eclipse.kapua.connector.AmqpAbstractConnector;
import org.eclipse.kapua.connector.MessageContext;
import org.eclipse.kapua.converter.Converter;
import org.eclipse.kapua.converter.KapuaConverterException;
import org.eclipse.kapua.message.transport.TransportMessageType;
import org.eclipse.kapua.message.transport.TransportQos;
import org.eclipse.kapua.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.Status;
import io.vertx.proton.ProtonHelper;

/**
 * AMQP ActiveMQ connector implementation.<br>
 * This connector doesn't do any message processing in the incoming message so it's useful for handling the process of messages in error.
 */
public abstract class AmqpActiveMQConnector extends AmqpAbstractConnector<Message, Message> implements HealthCheckable {

    protected final static Logger logger = LoggerFactory.getLogger(AmqpActiveMQConnector.class);

    private final static String ACTIVEMQ_QOS = "ActiveMQ.MQTT.QoS";
    private final static String TOPIC_SEPARATOR = "/";
    private final static String CLASSIFIER_TOPIC_PREFIX = SystemSetting.getInstance().getMessageClassifier() + TOPIC_SEPARATOR;

    private AmqpConsumer consumer;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AmqpActiveMQConnector(Vertx vertx, ClientOptions clientOptions, Processor<Message> processor) {
        super(vertx, processor);
        consumer = new AmqpConsumer(vertx, clientOptions, (delivery, message) -> {
                try {
                    super.handleMessage(new MessageContext<Message>(message), result -> {
                        if (result.succeeded()) {
                            ProtonHelper.accepted(delivery, true);
                        }
                        else {
                            try {
                                errorProcessor.process(new MessageContext(message), new Handler<AsyncResult<Void>>() {
                                    @Override
                                    public void handle(AsyncResult<Void> event) {
                                        if (event.succeeded()) {
                                            ProtonHelper.accepted(delivery, true);
                                        }
                                        else {
                                            ProtonHelper.released(delivery, true);
                                        }
                                    }
                                });
                            } catch (Exception e1) {
                                ProtonHelper.released(delivery, true);
                            }
                        }
                    });
                } catch (Exception e) {
                    logger.error("Exception while processing message: {}", e.getMessage(), e);
                    ProtonHelper.released(delivery, true);
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
    protected MessageContext<Message> convert(MessageContext<?> message) throws KapuaConverterException {
        //this cast is safe since this implementation is using the AMQP connector
        Message msg = (Message)message.getMessage();
        return new MessageContext<Message>(
                msg,
                getMessageParameters(msg));

        // By default, the receiver automatically accepts (and settles) the delivery
        // when the handler returns, if no other disposition has been applied.
        // To change this and always manage dispositions yourself, use the
        // setAutoAccept method on the receiver.
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
        if (mqttTopic!=null && mqttTopic.startsWith(CLASSIFIER_TOPIC_PREFIX)) {
            parameters.put(Converter.MESSAGE_TYPE, TransportMessageType.CONTROL);
            mqttTopic = mqttTopic.substring(CLASSIFIER_TOPIC_PREFIX.length());
        } else {
            parameters.put(Converter.MESSAGE_TYPE, TransportMessageType.TELEMETRY);
        }
        parameters.put(Converter.MESSAGE_DESTINATION, mqttTopic);

        //extract connection id
        try {
          //Base 64 encoded String (by the broker plugin)
            String connectionId = (String)message.getApplicationProperties().getValue().get(Converter.HEADER_KAPUA_CONNECTION_ID);
            if (connectionId!=null) {
                parameters.put(Converter.CONNECTION_ID, connectionId);
            }
        }
        //TODO remove ClassCast from the catched exceptions?
        catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            //cannot get connection id so it may be not available at publish time
            logger.debug("Cannot get Kapua connection Id: {}", e.getMessage(), e);
        }

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

    @Override
    public Status getStatus() {
        if (consumer.isConnected()) {
            return Status.OK();
        }
        else {
            return Status.KO();
        }
    }

    @Override
    public boolean isHealty() {
        return consumer.isConnected();
    }
}
