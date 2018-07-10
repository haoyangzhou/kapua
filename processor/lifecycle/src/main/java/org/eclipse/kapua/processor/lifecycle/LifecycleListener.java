/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

import org.eclipse.kapua.KapuaErrorCodes;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.metric.MetricServiceFactory;
import org.eclipse.kapua.commons.metric.MetricsService;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.connector.MessageContext;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.message.device.lifecycle.KapuaAppsChannel;
import org.eclipse.kapua.message.device.lifecycle.KapuaAppsMessage;
import org.eclipse.kapua.message.device.lifecycle.KapuaAppsPayload;
import org.eclipse.kapua.message.device.lifecycle.KapuaBirthMessage;
import org.eclipse.kapua.message.device.lifecycle.KapuaDisconnectMessage;
import org.eclipse.kapua.message.device.lifecycle.KapuaMissingMessage;
import org.eclipse.kapua.message.device.lifecycle.KapuaNotifyMessage;
import org.eclipse.kapua.message.internal.device.lifecycle.KapuaAppsChannelImpl;
import org.eclipse.kapua.message.internal.device.lifecycle.KapuaAppsMessageImpl;
import org.eclipse.kapua.message.transport.TransportMessage;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.processor.KapuaProcessorException;
import org.eclipse.kapua.service.account.Account;
import org.eclipse.kapua.service.account.AccountService;
import org.eclipse.kapua.service.device.registry.Device;
import org.eclipse.kapua.service.device.registry.DeviceRegistryService;
import org.eclipse.kapua.service.device.registry.lifecycle.DeviceLifeCycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device messages listener (device life cycle).<br>
 * Manage:<br>
 * - BIRTH/DC/LWT/APPS/NOTIFY device messages<br>
 *
 * @since 1.0
 */
public class LifecycleListener {

    //TODO it's the same field in org.eclipse.kapua.broker.core.message.MessageConstants. move them to a shared place
    public static final String HEADER_KAPUA_CONNECTION_ID = "KAPUA_CONNECTION_ID";
    private static final Logger logger = LoggerFactory.getLogger(LifecycleListener.class);

    private static DeviceLifeCycleService deviceLifeCycleService = KapuaLocator.getInstance().getService(DeviceLifeCycleService.class);
    private static DeviceRegistryService deviceRegistryService = KapuaLocator.getInstance().getService(DeviceRegistryService.class);
    private static AccountService accountService = KapuaLocator.getInstance().getService(AccountService.class);

    // metrics
    private String metricComponentName = "listener";
    private String name = "deviceLifeCycle";
    private static final MetricsService METRICS_SERVICE = MetricServiceFactory.getInstance();

    private Counter metricDeviceBirthMessage;
    private Counter metricDeviceDisconnectMessage;
    private Counter metricDeviceMissingMessage;
    private Counter metricDeviceAppsMessage;
    private Counter metricDeviceNotifyMessage;
    private Counter metricDeviceUnmatchedMessage;
    private Counter metricDeviceErrorMessage;

    protected LifecycleListener() {
        metricDeviceBirthMessage = registerCounter("messages", "birth", "count");
        metricDeviceDisconnectMessage = registerCounter("messages", "dc", "count");
        metricDeviceMissingMessage = registerCounter("messages", "missing", "count");
        metricDeviceAppsMessage = registerCounter("messages", "apps", "count");
        metricDeviceNotifyMessage = registerCounter("messages", "notify", "count");
        metricDeviceUnmatchedMessage = registerCounter("messages", "unmatched", "count");
        metricDeviceErrorMessage = registerCounter("messages", "error", "count");
    }

    protected Counter registerCounter(String... names) {
        return METRICS_SERVICE.getCounter(metricComponentName, name, names);
    }

    protected Timer registerTimer(String... names) {
        return METRICS_SERVICE.getTimer(metricComponentName, name, names);
    }

    /**
     * Process a birth message.
     *
     * @param birthMessage
     */
    public void processBirthMessage(MessageContext<TransportMessage> message) {
        try {
            deviceLifeCycleService.birth(getConnectionId(message), convertToBirth(message));
            metricDeviceBirthMessage.inc();
        } catch (KapuaException e) {
            metricDeviceErrorMessage.inc();
            logger.error("Error while processing device birth life-cycle event", e);
        }
    }

    /**
     * Process a disconnect message.
     *
     * @param disconnectMessage
     */
    public void processDisconnectMessage(MessageContext<TransportMessage> message) {
        try {
            deviceLifeCycleService.death(getConnectionId(message), convertToDc(message));
            metricDeviceDisconnectMessage.inc();
        } catch (KapuaException e) {
            metricDeviceErrorMessage.inc();
            logger.error("Error while processing device disconnect life-cycle event", e);
        }
    }

    /**
     * Process an application message.
     *
     * @param appsMessage
     */
    public void processAppsMessage(MessageContext<TransportMessage> message) {
        try {
            deviceLifeCycleService.applications(getConnectionId(message), convertToApps(message));
            metricDeviceAppsMessage.inc();
        } catch (KapuaException e) {
            metricDeviceErrorMessage.inc();
            logger.error("Error while processing device apps life-cycle event", e);
        }
    }

    /**
     * Process a missing message.
     *
     * @param missingMessage
     */
    public void processMissingMessage(MessageContext<TransportMessage> message) {
        try {
            deviceLifeCycleService.missing(getConnectionId(message), convertToMissing(message));
            metricDeviceMissingMessage.inc();
        } catch (KapuaException e) {
            metricDeviceErrorMessage.inc();
            logger.error("Error while processing device missing life-cycle event", e);
        }
    }

    /**
     * Process a notify message.
     *
     * @param notifyMessage
     */
    public void processNotifyMessage(MessageContext<TransportMessage> message) {
        logger.info("Received notify message from device channel: {}", message.getMessage().getChannel());
        metricDeviceNotifyMessage.inc();
    }

    /**
     * Process a unmatched message.
     *
     * @param unmatchedMessage
     */
    public void processUnmatchedMessage(MessageContext<TransportMessage> message) {
        logger.info("Received unmatched message from device channel: {}", message.getMessage().getChannel());
        metricDeviceUnmatchedMessage.inc();
    }

    private KapuaAppsMessage convertToApps(MessageContext<TransportMessage> message) throws KapuaException {
        try {
            TransportMessage tm = message.getMessage();
            KapuaAppsMessage appsMessage = new KapuaAppsMessageImpl();
            //channel
            KapuaAppsChannel kapuaChannel = new KapuaAppsChannelImpl();
            kapuaChannel.setSemanticParts(tm.getChannel().getSemanticParts());
            appsMessage.setChannel(kapuaChannel);

            //payload
            //TODO implement me!
            KapuaAppsPayload kapuaPayload = null;//new KapuaAppsPayloadImpl();
            kapuaPayload.setMetrics(tm.getPayload().getMetrics());
            kapuaPayload.setBody(tm.getPayload().getBody());
            appsMessage.setPayload(kapuaPayload);
            appsMessage.setClientId(tm.getClientId());
            appsMessage.setPosition(tm.getPosition());
            appsMessage.setReceivedOn(tm.getReceivedOn());
            appsMessage.setSentOn(tm.getSentOn());
            try {
                KapuaSecurityUtils.doPrivileged(() -> {
                    Account account = accountService.findByName(tm.getScopeName());
                    if (account==null) {
                        throw new KapuaProcessorException(KapuaErrorCodes.ILLEGAL_ARGUMENT, "message.scopeName", tm.getScopeName());
                    }
                    appsMessage.setScopeId(account.getId());
                    Device device = deviceRegistryService.findByClientId(account.getId(), tm.getClientId());
                    if (device==null) {
                        throw new KapuaProcessorException(KapuaErrorCodes.ILLEGAL_ARGUMENT, "device.clientId", tm.getClientId());
                    }
                    appsMessage.setDeviceId(device.getId());
                    logger.debug("Lifecycle birth... converting message... DONE");
                });
            } catch (KapuaException e) {
                logger.info("Lifecycle birth... Error processing message {}", e.getMessage());
                throw new KapuaProcessorException(KapuaErrorCodes.INTERNAL_ERROR, e);
            }
            deviceLifeCycleService.applications(getConnectionId(message), appsMessage);
            metricDeviceAppsMessage.inc();
            return appsMessage;
        } catch (KapuaException e) {
            metricDeviceErrorMessage.inc();
            logger.error("Error while processing device apps life-cycle event", e);
            throw e;
        }
    }

    private KapuaDisconnectMessage convertToDc(MessageContext<TransportMessage> message) {
        //TODO implement me!
        return null;
    }

    private KapuaMissingMessage convertToMissing(MessageContext<TransportMessage> message) {
        //TODO implement me!
        return null;
    }

    private KapuaBirthMessage convertToBirth(MessageContext<TransportMessage> message) {
        //TODO implement me!
        return null;
    }

    private KapuaNotifyMessage convertToNotify(MessageContext<TransportMessage> message) {
        //TODO implement me!
        return null;
    }

    private KapuaId getConnectionId(MessageContext<TransportMessage> message) {
        return (KapuaId)message.getProperties().get(HEADER_KAPUA_CONNECTION_ID);
    }
}
