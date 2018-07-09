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
package org.eclipse.kapua.broker.client.amqp;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kapua.broker.client.amqp.settings.AmqpClientSettings;
import org.eclipse.kapua.broker.client.amqp.settings.AmqpClientSettingsKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;

public abstract class AbstractAmqpClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAmqpClient.class);

    protected String destination = AmqpClientSettings.getInstance().getString(AmqpClientSettingsKey.DESTINATION);
    protected String brokerHost = AmqpClientSettings.getInstance().getString(AmqpClientSettingsKey.BROKER_HOST);
    protected int brokerPort = AmqpClientSettings.getInstance().getInt(AmqpClientSettingsKey.BROKER_PORT);
    protected String brokerUsername = AmqpClientSettings.getInstance().getString(AmqpClientSettingsKey.BROKER_USERNAME);
    protected String brokerPassword = AmqpClientSettings.getInstance().getString(AmqpClientSettingsKey.BROKER_PASSWORD);

    protected boolean connected;
    protected AtomicInteger reconnectionFaultCount = new AtomicInteger();
    protected Long reconnectTaskId;
    protected int maxReconnectionAttempts;
    protected int exitCode = -1;

    protected Vertx vertx;
    protected Context context;
    protected ProtonClient client;
    protected ProtonConnection connection;

    protected static final String KEY_MAX_RECONNECTION_ATTEMPTS = "maxReconnectionAttempts";
    protected static final String KEY_EXIT_CODE = "exitCode";

    //TODO move this configuration parameters to global section
    protected void setConfiguration(Map<String, Object> configuration) {
        logger.info("Configuration");
        Integer tmp = (Integer)configuration.get(KEY_MAX_RECONNECTION_ATTEMPTS);
        maxReconnectionAttempts = tmp != null ? (int) tmp : -1;
        logger.info("Maximum reconnection attemps {}", maxReconnectionAttempts);
        tmp = (Integer)configuration.get(KEY_EXIT_CODE);
        exitCode = tmp != null ? (int) tmp : -1;
        logger.info("Exit code {}", exitCode);
    }

    protected AbstractAmqpClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public boolean isConnected() {
        return connected;
    }

    protected void setConnected(boolean connected) {
        this.connected = connected;
    }

    protected abstract void registerAction(ProtonConnection connection, Future<Object> future);

    public void disconnect(Future<Void> stopFuture) {
        //TODO disable reconnection in the meanwhile
        if (connection != null) {
            connection.close();
            connection = null;
            stopFuture.complete();
        }
    }

    public void connect(Future<Void> startFuture) {
        logger.info("Connecting to broker {}:{}...", brokerHost, brokerPort);
        // make sure connection is already closed
        if (connection != null && !connection.isDisconnected()) {
            if (!startFuture.isComplete()) {
                startFuture.fail("Unable to connect: still connected");
            }
            return;
        }

        client = ProtonClient.create(vertx);
        ProtonClientOptions options = new ProtonClientOptions();
        //TODO get parameters from configuration
        options.setConnectTimeout(60);
        options.setIdleTimeout(60);
        options.setHeartbeat(15000);
        client.connect(
                options,
                brokerHost,
                brokerPort,
                brokerUsername,
                brokerPassword,
                asynchResult ->{
                    if (asynchResult.succeeded()) {
                        logger.info("Connecting to broker {}:{}... Creating receiver... DONE", brokerHost, brokerPort);
                        connection = asynchResult.result();
                        connection.openHandler((event) -> {
                            if (event.succeeded()) {
                                connection = event.result();
                                context.executeBlocking(future -> registerAction(asynchResult.result(), future), result -> {
                                    if (result.succeeded()) {
                                        logger.debug("Starting connector...DONE");
                                        setConnected(true);
                                        startFuture.complete();
                                    } else {
                                        logger.warn("Starting connector...FAIL [message:{}]", result.cause().getMessage());
                                        if (!startFuture.isComplete()) {
                                            startFuture.fail(asynchResult.cause());
                                        }
                                        setConnected(false);
                                        notifyConnectionLost();
                                    }
                                });
                            }
                            else {
                                startFuture.fail("Cannot establish connection!");
                                setConnected(false);
                                notifyConnectionLost();
                            }
                        });
                        connection.open();
                    } else {
                        logger.error("Cannot register ActiveMQ connection! ", asynchResult.cause().getCause());
                        if (!startFuture.isComplete()) {
                            startFuture.fail(asynchResult.cause());
                        }
                        setConnected(false);
                        notifyConnectionLost();
                    }
                });
    }

    protected void notifyConnectionLost() {
        logger.info("Notify disconnection...");
        if (reconnectTaskId == null) {
            if (reconnectTaskId == null) {
                long backOff = evaluateBackOff();
                logger.info("Notify disconnection... Start new task {}", backOff);
                reconnectTaskId = vertx.setTimer(backOff, new Handler<Long>() {

                    @Override
                    public void handle(Long obj) {
                        Future<Void> future = Future.future();
                        future.setHandler(result -> {
                            reconnectTaskId = null;
                            if (result.succeeded()) {
                                logger.info("Establish connection retry {}... SUCCESS", reconnectionFaultCount.get());
                                reconnectionFaultCount.set(0);
                            } else {
                                logger.info("Establish connection retry {}... FAILURE", reconnectionFaultCount.get(), result.cause());
                                if (reconnectionFaultCount.incrementAndGet() > maxReconnectionAttempts && maxReconnectionAttempts>-1) {
                                    logger.error("Maximum reconnection attempts reached. Exiting...");
                                    System.exit(exitCode);
                                };
                                //schedule a new task
                                notifyConnectionLost();
                            }
                        });
                        connect(future);
                        logger.info("Started new connection donw");
                    }
                });
            }
            else {
                logger.info("Another reconnect operation is enqueed. No action will be taken!");
            }
        }
        else {
            logger.info("Another reconnect operation is enqueed. No action will be taken!");
        }
        logger.info("Notify disconnection... DONE");
    }

    private long evaluateBackOff() {
        //TODO change algorithm to something exponential
        return (1 + reconnectionFaultCount.get()) * 3000;
    }

}
