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
package org.eclipse.kapua.processor.error.amqp.activemq;

//import org.apache.qpid.proton.amqp.Binary;
//import org.apache.qpid.proton.amqp.messaging.AmqpValue;
//import org.apache.qpid.proton.amqp.messaging.Data;
//import org.apache.qpid.proton.message.Message;
//import org.apache.qpid.proton.message.impl.MessageImpl;
//import org.eclipse.kapua.KapuaErrorCodes;
import org.eclipse.kapua.broker.client.amqp.AmqpSender;
import org.eclipse.kapua.connector.MessageContext;
import org.eclipse.kapua.message.transport.TransportMessage;
import org.eclipse.kapua.processor.KapuaProcessorException;
import org.eclipse.kapua.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ErrorProcessor implements Processor<TransportMessage> {

    private static final Logger logger = LoggerFactory.getLogger(ErrorProcessor.class);

    private AmqpSender sender;

    public ErrorProcessor(Vertx vertx) {
        sender = new AmqpSender(vertx);
    }

    @Override
    public void start(Future<Void> startFuture) {
        sender.connect(startFuture);
    }

    @Override
    public void process(MessageContext<TransportMessage> message) throws KapuaProcessorException {
        sender.send(translate(message));
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        // nothing to do
        stopFuture.complete();
    }

    private byte[] translate(MessageContext<TransportMessage> transportMessage) {
        //TODO missing message creation
        byte[] data = null;
        return data;
    }

}
