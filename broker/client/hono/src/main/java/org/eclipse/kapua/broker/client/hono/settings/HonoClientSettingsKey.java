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
package org.eclipse.kapua.broker.client.hono.settings;

import org.eclipse.kapua.commons.setting.SettingKey;

/**
 * HonoClientSettingsKey keys.
 * 
 * @since 1.0
 */
public enum HonoClientSettingsKey implements SettingKey {

    /**
     * Broker name (or ip)
     */
    BROKER_HOST("hono.client.broker.host"),
    /**
     * Broker url
     */
    BROKER_PORT("hono.client.broker.port"),
    /**
     * Username
     */
    BROKER_USERNAME("hono.client.broker.username"),
    /**
     * Broker password
     */
    BROKER_PASSWORD("hono.client.broker.password"),
    /**
     * Proton client maximum reconnect attempts
     */
    PROTON_MAX_RECONNECT_ATTEMPTS("hono.client.proton.max_reconnect_attempts"),
    /**
     * Proton client wait between reconnection attempt
     */
    PROTON_WAIT_BETWEEN_RECONNECT("hono.client.proton.wait_between_reconnect"),
    /**
     * Proton client connect timeout
     */
    PROTON_CONNECT_TIMEOUT("hono.client.proton.connect_timeout"),
    /**
     * Proton client idle timeout
     */
    PROTON_IDLE_TIMEOUT("hono.client.proton.idle_timeout"),
    /**
     * Tenant id
     */
    TENANT_ID("hono.client.tenant_id"),
    /**
     * Client trust store
     */
    TRUSTSTORE_FILE("hono.client.proton.truststore_file"),
    /**
     * Maximum reconnection attempt (without any success between them) before exiting JVM (negative numbers means no exit)
     */
    MAX_RECONNECTION_ATTEMPTS("hono.client.maximum_reconnection_attempt"),
    /**
     * Exiting code when maximum reconnection attempt is reached
     */
    EXIT_CODE("hono.client.exit_code");

    private String key;

    private HonoClientSettingsKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
