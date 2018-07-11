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
package org.eclipse.kapua.processor.logger;

import java.io.StringWriter;

import org.eclipse.kapua.apps.api.HealthCheckable;
import org.eclipse.kapua.commons.util.xml.XmlUtil;
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

public class LoggerProcessor implements Processor<TransportMessage>, HealthCheckable {

    private static final Logger logger = LoggerFactory.getLogger(LoggerProcessor.class);

    @Override
    public void start(Future<Void> startFuture) {
        XmlUtil.setContextProvider(new LoggerProcessorJAXBContextProvider());
        logger.info("Instantiate Jaxb Context... Done.");
        startFuture.complete();
    }

    @Override
    public void process(MessageContext<TransportMessage> message, Handler<AsyncResult<Void>> result) throws KapuaProcessorException {

        StringWriter sw = new StringWriter();
        try {
            XmlUtil.marshalJson(message.getMessage(), sw);
        } catch (Exception e) {
            result.handle(Future.failedFuture(e));
            logger.error("Exception while marshalling message: {}", e.getMessage(), e);
        }

        logger.info(sw.toString());
        result.handle(Future.succeededFuture());
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        // nothing to do
        stopFuture.complete();
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
