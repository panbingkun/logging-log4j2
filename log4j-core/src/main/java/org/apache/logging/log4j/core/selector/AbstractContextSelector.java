/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.selector;

import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.internal.ContextEnvironmentPropertySource;
import org.apache.logging.log4j.kit.env.internal.ContextPropertiesPropertySource;
import org.apache.logging.log4j.kit.env.support.PropertySourcePropertyEnvironment;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.status.StatusLogger;

public abstract class AbstractContextSelector implements ContextSelector {

    private static final int PROPERTY_SOURCE_PRIORITY_OFFSET = -10;

    protected final ConfigurableInstanceFactory instanceFactory;

    public AbstractContextSelector(final ConfigurableInstanceFactory instanceFactory) {
        this.instanceFactory = instanceFactory;
    }

    private PropertyEnvironment createChildPropertyEnvironment(final String contextName, final ClassLoader loader) {
        final PropertyEnvironment parentEnvironment = instanceFactory.getInstance(PropertyEnvironment.class);
        final Logger statusLogger = instanceFactory.getInstance(Constants.DEFAULT_STATUS_LOGGER_KEY);
        return new PropertySourcePropertyEnvironment(
                parentEnvironment,
                List.of(
                        new ContextPropertiesPropertySource(contextName, PROPERTY_SOURCE_PRIORITY_OFFSET),
                        new ContextEnvironmentPropertySource(contextName, PROPERTY_SOURCE_PRIORITY_OFFSET)),
                loader,
                statusLogger != null ? statusLogger : StatusLogger.getLogger());
    }

    private ConfigurableInstanceFactory createChildInstanceFactory(final String contextName, final ClassLoader loader) {
        return instanceFactory.newChildInstanceFactory(
                () -> createChildPropertyEnvironment(contextName, loader), () -> loader);
    }

    protected final LoggerContext createContext(
            final String contextName, final URI configLocation, final ClassLoader loader) {
        final ConfigurableInstanceFactory childFactory = createChildInstanceFactory(contextName, loader);
        return createContext(contextName, configLocation, childFactory);
    }

    protected abstract LoggerContext createContext(
            final String contextName, final URI configLocation, final ConfigurableInstanceFactory instanceFactory);
}
