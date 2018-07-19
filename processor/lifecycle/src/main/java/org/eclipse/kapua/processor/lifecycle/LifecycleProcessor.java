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

import org.eclipse.kapua.apps.api.HealthCheckable;
import org.eclipse.kapua.connector.MessageContext;
import org.eclipse.kapua.message.transport.TransportMessage;
import org.eclipse.kapua.processor.KapuaProcessorException;
import org.eclipse.kapua.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.healthchecks.Status;

public class LifecycleProcessor implements Processor<TransportMessage>, HealthCheckable {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleProcessor.class);

    private static final String INVALID_TOPIC = "Cannot detect destination!";

    private LifecycleListener lifecycleListener;

    //TODO keep messages types as enum or switch to String?
    enum LifecycleTypes {
        APPS,
        BIRTH,
        DC,
        MISSING
    }

    public LifecycleProcessor() {
        lifecycleListener = new LifecycleListener();
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
    public void process(MessageContext<TransportMessage> message, Handler<AsyncResult<Void>> result) throws KapuaProcessorException {
        List<String> destination = message.getMessage().getChannel().getSemanticParts();
        if (destination!=null && destination.size()>1) {
            LifecycleTypes token = LifecycleTypes.valueOf(destination.get(1));
            switch (token) {
            case APPS:
                lifecycleListener.processAppsMessage(message);
                break;
            case BIRTH:
                lifecycleListener.processBirthMessage(message);
                break;
            case DC:
                lifecycleListener.processDisconnectMessage(message);
                break;
            case MISSING:
                lifecycleListener.processMissingMessage(message);
                break;
            default:
                result.handle(Future.succeededFuture());
                break;
            }
        }
        else {
            result.handle(Future.failedFuture(INVALID_TOPIC));
        }
    }

    @Override
    public Status getStatus() {
        return Status.OK();
    }

    @Override
    public boolean isHealty() {
        return true;
    }

}
