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
package org.eclipse.kapua.qa.steps;

import javax.inject.Inject;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class ExternalConsumers {

    private DBHelper database;

    @Inject
    public ExternalConsumers(DBHelper database) {
        this.database = database;
    }

    @Before(value = "@StartExternalConsumers")
    public void start() throws Exception {
        database.setup();
        org.eclipse.kapua.consumer.activemq.datastore.Consumer.main(null);
        org.eclipse.kapua.consumer.activemq.lifecycle.Consumer.main(null);
        org.eclipse.kapua.consumer.activemq.error.Consumer.main(null);
    }

    @After(value = "@StopExternalConsumers")
    public void stop() {
    }

}
