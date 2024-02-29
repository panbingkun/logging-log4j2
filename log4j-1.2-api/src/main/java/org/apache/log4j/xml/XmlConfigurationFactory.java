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
package org.apache.log4j.xml;

import org.apache.log4j.config.Log4j1Configuration;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.impl.PropertyKeys;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

/**
 * Constructs a Configuration usable in Log4j 2 from a Log4j 1 configuration file.
 */
@Namespace(ConfigurationFactory.NAMESPACE)
@Plugin("Log4j1XmlConfigurationFactory")
@Order(2)
public class XmlConfigurationFactory extends ConfigurationFactory {

    public static final String FILE_EXTENSION = ".xml";

    /**
     * File name prefix for test configurations.
     */
    protected static final String TEST_PREFIX = "log4j-test";

    /**
     * File name prefix for standard configurations.
     */
    protected static final String DEFAULT_PREFIX = "log4j";

    private final boolean enabled;

    public XmlConfigurationFactory() {
        this(PropertyEnvironment.getGlobal().getProperty(PropertyKeys.Version1.class));
    }

    private XmlConfigurationFactory(final PropertyKeys.Version1 config) {
        this.enabled = config.compatibility() || config.configuration() != null;
    }

    @Override
    protected String[] getSupportedTypes() {
        if (enabled) {
            return new String[] {FILE_EXTENSION};
        }
        return null;
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        final int interval =
                PropertyEnvironment.getGlobal().getIntegerProperty(Log4j1Configuration.MONITOR_INTERVAL, 0);
        return new XmlConfiguration(loggerContext, source, interval);
    }

    @Override
    protected String getTestPrefix() {
        return TEST_PREFIX;
    }

    @Override
    protected String getDefaultPrefix() {
        return DEFAULT_PREFIX;
    }

    @Override
    protected String getVersion() {
        return LOG4J1_VERSION;
    }
}
