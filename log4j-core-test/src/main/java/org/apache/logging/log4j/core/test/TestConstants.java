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
package org.apache.logging.log4j.core.test;

import org.apache.logging.log4j.core.impl.PropertyKeys;
import org.jspecify.annotations.Nullable;

public final class TestConstants {

    private TestConstants() {}

    private static final String ASYNC = "Async.";

    public static final String ASYNC_FORMAT_MESSAGES_IN_BACKGROUND = ASYNC + "formatMessagesInBackground";

    private static final String ASYNC_LOGGER = "AsyncLogger.";

    public static final String ASYNC_LOGGER_EXCEPTION_HANDLER = ASYNC_LOGGER + "exceptionHandler";

    public static final String ASYNC_LOGGER_RING_BUFFER_SIZE = ASYNC_LOGGER + "ringBufferSize";

    private static final String ASYNC_LOGGER_CONFIG = "AsyncLoggerConfig.";

    public static final String ASYNC_LOGGER_CONFIG_RING_BUFFER_SIZE = ASYNC_LOGGER_CONFIG + "ringBufferSize";

    private static final String CONFIGURATION = "Configuration.";
    /**
     * @see PropertyKeys.Configuration
     */
    public static final String CONFIGURATION_CONFIGURATION_FACTORY = CONFIGURATION + "configurationFactory";

    public static final String CONFIGURATION_FILE = CONFIGURATION + "file";

    public static final String CONFIGURATION_RELIABILITY_STRATEGY = CONFIGURATION + "reliabilityStrategy";

    public static final String CONFIGURATION_USE_PRECISE_CLOCK = CONFIGURATION + "usePreciseClock";

    public static final String CONSOLE_JANSI_ENABLED = "Console.jansiEnabled";

    private static final String GC = "GC.";

    public static final String GC_ENABLE_DIRECT_ENCODERS = GC + "enableDirectEncoders";

    private static final String LOGGER_CONTEXT = "LoggerContext.";

    public static final String LOGGER_CONTEXT_LOG_EVENT_FACTORY = LOGGER_CONTEXT + "logEventFactory";

    public static final String LOGGER_CONTEXT_SELECTOR = LOGGER_CONTEXT + "selector";

    private static final String MESSAGE = "Message.";

    public static final String MESSAGE_FACTORY = MESSAGE + "factory";

    public static final String VERSION1_CONFIGURATION = "Version1.configuration";

    public static final String VERSION1_COMPATIBILITY = "Version1.compatibility";

    private static final String THREAD_CONTEXT = "ThreadContext.";

    public static final String THREAD_CONTEXT_CONTEXT_DATA = THREAD_CONTEXT + "contextData";

    public static final String THREAD_CONTEXT_GARBAGE_FREE = THREAD_CONTEXT + "garbageFree";

    public static final String THREAD_CONTEXT_MAP_CLASS = THREAD_CONTEXT + "mapClass";

    private static final String WEB = "WEB.";

    public static final String WEB_IS_WEB_APP = WEB + "isWebApp";

    /**
     * Transforms a Log4j property key to a form suitable for Java system properties
     *
     * @param key A Log4j property key.
     * @return A Java system property key.
     */
    public static String toSystemProperty(final String key) {
        return "log4j2." + key;
    }

    public static @Nullable String setSystemProperty(final String key, final @Nullable String value) {
        final String systemKey = toSystemProperty(key);
        final String oldValue = System.getProperty(systemKey);
        if (value != null) {
            System.setProperty(systemKey, value);
        } else {
            System.clearProperty(systemKey);
        }
        return oldValue;
    }
}
