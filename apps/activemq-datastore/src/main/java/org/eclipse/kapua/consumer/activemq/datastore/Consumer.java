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
package org.eclipse.kapua.consumer.activemq.datastore;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.eclipse.kapua.apps.api.AbstractApplication;
import org.eclipse.kapua.apps.api.ApplicationContext;
import org.eclipse.kapua.commons.jpa.JdbcConnectionUrlResolvers;
import org.eclipse.kapua.commons.setting.system.SystemSetting;
import org.eclipse.kapua.commons.setting.system.SystemSettingKey;
import org.eclipse.kapua.commons.util.xml.XmlUtil;
import org.eclipse.kapua.connector.activemq.AmqpActiveMQConnector;
import org.eclipse.kapua.converter.kura.KuraPayloadProtoConverter;
import org.eclipse.kapua.processor.datastore.DatastoreProcessor;
import org.eclipse.kapua.service.liquibase.KapuaLiquibaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import io.vertx.ext.healthchecks.Status;

/**
 * ActiveMQ AMQP consumer with Kura payload converter and Kapua data store ingestion
 *
 */
public class Consumer extends AbstractApplication {

    protected final static Logger logger = LoggerFactory.getLogger(Consumer.class);
    private final static String HEALTH_NAME = "Consumer-ActiveMQ";
    private final static String HEALTH_PATH = "/health/consumer/activemq";

    public static void main(String args[]) throws Exception {
        Consumer consumer = new Consumer();
        consumer.start(args);
    }

    private AmqpActiveMQConnector connector;
    private KuraPayloadProtoConverter converter;
    private DatastoreProcessor processor;

    protected Consumer() {
        SystemSetting configSys = SystemSetting.getInstance();
        logger.info("Checking database... '{}'", configSys.getBoolean(SystemSettingKey.DB_SCHEMA_UPDATE));
        if(configSys.getBoolean(SystemSettingKey.DB_SCHEMA_UPDATE, false)) {
            logger.debug("Starting Liquibase embedded client.");
            String dbUsername = configSys.getString(SystemSettingKey.DB_USERNAME);
            String dbPassword = configSys.getString(SystemSettingKey.DB_PASSWORD);
            String schema = MoreObjects.firstNonNull(configSys.getString(SystemSettingKey.DB_SCHEMA_ENV), configSys.getString(SystemSettingKey.DB_SCHEMA));
            new KapuaLiquibaseClient(JdbcConnectionUrlResolvers.resolveJdbcUrl(), dbUsername, dbPassword, Optional.of(schema)).update();
        }
    }

    @Override
    protected CompletableFuture<String> internalStart(ApplicationContext applicationContext) throws Exception {
        CompletableFuture<String> startFuture = new CompletableFuture<>();
        //disable Vertx BlockedThreadChecker log
        java.util.logging.Logger.getLogger("io.vertx.core.impl.BlockedThreadChecker").setLevel(Level.OFF);
        XmlUtil.setContextProvider(new JAXBContextProvider());
        logger.info("Instantiating HonoConsumer...");
        logger.info("Instantiating HonoConsumer... initializing KuraPayloadProtoConverter");
        converter = new KuraPayloadProtoConverter();
        logger.info("Instantiating HonoConsumer... initializing DataStoreProcessor");
        processor = new DatastoreProcessor();
        logger.info("Instantiating HonoConsumer... instantiating AmqpHonoConnector");
        connector = new AmqpActiveMQConnector(applicationContext.getVertx(), converter, processor);
        logger.info("Instantiating HonoConsumer... DONE");
        applicationContext.getVertx().deployVerticle(connector, ar -> {
            if (ar.succeeded()) {
                startFuture.complete(ar.result());
            }
            else {
                startFuture.completeExceptionally(ar.cause());
            }
        });
        applicationContext.registerHealthCheck(HEALTH_PATH, HEALTH_NAME, hcm -> {
            if (connector.isConnected()) {
                hcm.complete(Status.OK());
            }
            else {
                hcm.complete(Status.KO());
            }
        });
        return startFuture;
    }

    @Override
    protected CompletableFuture<String> internalStop(ApplicationContext applicationContext) throws Exception {
        CompletableFuture<String> stopFuture = new CompletableFuture<>();
        stopFuture.complete("Application stopped!");
        return stopFuture;
    }

}
