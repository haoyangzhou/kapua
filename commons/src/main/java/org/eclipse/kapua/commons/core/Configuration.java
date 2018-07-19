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
package org.eclipse.kapua.commons.core;

import java.util.List;

/**
 * Configuration holds a set of key-value pairs collected from configuration 
 * sources {@link ConfigurationSource} (e.g. configuration files) provided 
 * by the application.
 * <p>
 * Keys use a dotted notation (e.g. "comp.feature.property").
 * Configuration is by default a managed bean.
 */
public interface Configuration {

    public List<String> getKeys();

    public String getProperty(String key);
}
