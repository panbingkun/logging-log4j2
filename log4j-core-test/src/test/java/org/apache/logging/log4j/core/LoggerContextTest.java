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
package org.apache.logging.log4j.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.jupiter.api.Test;

/**
 * Validate Logging after Shutdown.
 */
public class LoggerContextTest {

    @Test
    public void shutdownTest() {
        final LoggerContextFactory loggerContextFactory = LogManager.getFactory();
        assertThat(loggerContextFactory).isInstanceOf(Log4jContextFactory.class);
        final Log4jContextFactory factory = (Log4jContextFactory) loggerContextFactory;

        final ShutdownCallbackRegistry shutdownCallbackRegistry = factory.getShutdownCallbackRegistry();
        assertThat(shutdownCallbackRegistry).isInstanceOf(DefaultShutdownCallbackRegistry.class);
        final LifeCycle registry = (LifeCycle) shutdownCallbackRegistry;

        registry.start();
        registry.stop();

        final org.apache.logging.log4j.spi.LoggerContext loggerContext = LogManager.getContext(false);
        assertThat(loggerContext).isInstanceOf(SimpleLoggerContext.class);
    }
}
