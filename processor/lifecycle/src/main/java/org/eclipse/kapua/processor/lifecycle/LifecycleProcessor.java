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
package org.eclipse.kapua.processor.lifecycle;

import java.util.List;

import org.eclipse.kapua.connector.MessageContext;
import org.eclipse.kapua.message.transport.TransportMessage;
import org.eclipse.kapua.processor.KapuaProcessorException;
import org.eclipse.kapua.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;

public class LifecycleProcessor implements Processor<TransportMessage> {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleProcessor.class);

    //TODO keep messages types as enum or switch to String?
    enum LifecycleTypes {
        APPS,
        BIRTH,
        DC,
        MISSING,
        NOTIFY
    }

    @Override
    public void start(Future<Void> startFuture) {
        // nothing to do
        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        // nothing to do
        stopFuture.complete();
    }

    @Override
    public void process(MessageContext<TransportMessage> message) throws KapuaProcessorException {
        List<String> destination = message.getMessage().getChannel().getSemanticParts();
        if (destination!=null && destination.size()>1) {
            LifecycleTypes token = LifecycleTypes.valueOf(destination.get(1));
            switch (token) {
            case APPS:
                LifecycleListener.getInstance().processAppsMessage(message);
                break;
            case BIRTH:
                LifecycleListener.getInstance().processBirthMessage(message);
                break;
            case DC:
                LifecycleListener.getInstance().processDisconnectMessage(message);
                break;
            case MISSING:
                LifecycleListener.getInstance().processMissingMessage(message);
                break;
            case NOTIFY:
                LifecycleListener.getInstance().processNotifyMessage(message);
                break;
            default:
                //throw exception??
                break;
            }
        }
        else {
            //throw exception??
        }
    }

}
