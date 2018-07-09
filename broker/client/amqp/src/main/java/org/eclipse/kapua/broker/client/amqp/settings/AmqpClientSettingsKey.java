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
package org.eclipse.kapua.broker.client.amqp.settings;

import org.eclipse.kapua.commons.setting.SettingKey;

/**
 * AmqpClientSettingsKey keys.
 * 
 * @since 1.0
 */
public enum AmqpClientSettingsKey implements SettingKey {

    /**
     * Broker name (or ip)
     */
    BROKER_HOST("amqp.client.broker.host"),
    /**
     * Broker url
     */
    BROKER_PORT("amqp.client.broker.port"),
    /**
     * Username
     */
    BROKER_USERNAME("amqp.client.broker.username"),
    /**
     * Broker password
     */
    BROKER_PASSWORD("amqp.client.broker.password"),
    /**
     * Broker client id
     */
    BROKER_CLIENT_ID("amqp.client.broker.client_id"),
    /**
     * Destination to subscribe/produce
     */
    DESTINATION("amqp.client.broker.destination"),
    /**
     * Maximum reconnection attempt (without any success between them) before exiting JVM (negative numbers means no exit)
     */
    MAX_RECONNECTION_ATTEMPTS("amqp.client.maximum_reconnection_attempt"),
    /**
     * Exiting code when maximum reconnection attempt is reached
     */
    EXIT_CODE("amqp.client.exit_code");

    private String key;

    private AmqpClientSettingsKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
