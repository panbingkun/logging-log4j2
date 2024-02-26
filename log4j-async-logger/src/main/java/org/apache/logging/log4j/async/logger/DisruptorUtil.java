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
package org.apache.logging.log4j.async.logger;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.WaitStrategy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.async.logger.AsyncLoggerConfigDisruptor.Log4jEventWrapper;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Utility methods for getting Disruptor related configuration.
 */
final class DisruptorUtil {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int RINGBUFFER_MIN_SIZE = 128;

    /**
     * LOG4J2-2606: Users encountered excessive CPU utilization with Disruptor v3.4.2 when the application
     * was logging more than the underlying appender could keep up with and the ringbuffer became full,
     * especially when the number of application threads vastly outnumbered the number of cores.
     * CPU utilization is significantly reduced by restricting access to the enqueue operation.
     */
    private DisruptorUtil() {}

    static WaitStrategy createWaitStrategy(
            final PropertyKeys.DisruptorProperties disruptorProperties,
            final AsyncWaitStrategyFactory asyncWaitStrategyFactory) {
        if (asyncWaitStrategyFactory == null) {
            LOGGER.debug("No AsyncWaitStrategyFactory was configured in the configuration, using default factory...");
            return new DefaultAsyncWaitStrategyFactory(disruptorProperties).createWaitStrategy();
        }
        LOGGER.debug(
                "Using configured AsyncWaitStrategyFactory {}",
                asyncWaitStrategyFactory.getClass().getName());
        return asyncWaitStrategyFactory.createWaitStrategy();
    }

    static int calculateRingBufferSize(final PropertyKeys.DisruptorProperties disruptorProperties) {
        final int ringBufferSize = disruptorProperties.ringBufferSize();
        if (ringBufferSize < RINGBUFFER_MIN_SIZE) {
            LOGGER.warn("Invalid RingBufferSize {}, using minimum size {}.", ringBufferSize, RINGBUFFER_MIN_SIZE);
            return RINGBUFFER_MIN_SIZE;
        }
        return Integers.ceilingNextPowerOfTwo(ringBufferSize);
    }

    static ExceptionHandler<RingBufferLogEvent> getAsyncLoggerExceptionHandler(
            final PropertyKeys.AsyncLogger propsConfig) {
        try {
            final Class<? extends ExceptionHandler<RingBufferLogEvent>> handlerClass = propsConfig.exceptionHandler();
            if (handlerClass != null) {
                return LoaderUtil.newInstanceOf(handlerClass);
            }
        } catch (final ReflectiveOperationException e) {
            LOGGER.debug("Invalid AsyncLogger.ExceptionHandler value: {}", e.getMessage(), e);
        }
        return new AsyncLoggerDefaultExceptionHandler();
    }

    static ExceptionHandler<Log4jEventWrapper> getAsyncLoggerConfigExceptionHandler(
            final PropertyKeys.AsyncLoggerConfig propsConfig) {
        try {
            final Class<? extends ExceptionHandler<Log4jEventWrapper>> handlerClass = propsConfig.exceptionHandler();
            if (handlerClass != null) {
                return LoaderUtil.newInstanceOf(handlerClass);
            }
        } catch (final ReflectiveOperationException e) {
            LOGGER.debug("Invalid AsyncLogger.ExceptionHandler value: {}", e.getMessage(), e);
        }
        return new AsyncLoggerConfigDefaultExceptionHandler();
    }

    /**
     * Returns the thread ID of the background appender thread. This allows us to detect Logger.log() calls initiated
     * from the appender thread, which may cause deadlock when the RingBuffer is full. (LOG4J2-471)
     *
     * @param executor runs the appender thread
     * @return the thread ID of the background appender thread
     */
    public static long getExecutorThreadId(final ExecutorService executor) {
        final Future<Long> result = executor.submit(() -> Thread.currentThread().getId());
        try {
            return result.get();
        } catch (final Exception ex) {
            final String msg =
                    "Could not obtain executor thread Id. " + "Giving up to avoid the risk of application deadlock.";
            throw new IllegalStateException(msg, ex);
        }
    }
}
