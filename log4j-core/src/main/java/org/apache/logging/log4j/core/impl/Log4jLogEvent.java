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
package org.apache.logging.log4j.core.impl;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.ReusableLogEvent;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of a LogEvent.
 */
public class Log4jLogEvent implements LogEvent {

    private final Marker marker;
    private final Level level;
    private final String loggerName;
    private Message message;
    private final MutableInstant instant = new MutableInstant();
    private final Throwable thrown;
    private ThrowableProxy thrownProxy;
    private final StringMap contextData;
    private final ThreadContext.ContextStack contextStack;
    private long threadId;
    private String threadName;
    private int threadPriority;
    private boolean endOfBatch = false;
    /** @since Log4J 2.4 */
    private final long nanoTime;
    // Location data
    private final String loggerFqcn;
    private @Nullable StackTraceElement source;
    private boolean includeLocation;

    /** LogEvent Builder helper class. */
    public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<LogEvent> {

        private String loggerFqcn;
        private Marker marker;
        private Level level;
        private String loggerName;
        private Message message;
        private Throwable thrown;
        private final MutableInstant instant = new MutableInstant();
        private ThrowableProxy thrownProxy;
        private StringMap contextData;
        private ThreadContext.ContextStack contextStack = ThreadContext.getImmutableStack();
        private long threadId;
        private String threadName;
        private int threadPriority;
        private StackTraceElement source;
        private boolean includeLocation;
        private boolean endOfBatch = false;
        private long nanoTime;
        private Clock clock;
        private ContextDataInjector contextDataInjector;

        public Builder() {
            initDefaultContextData();
        }

        public Builder(final LogEvent other) {
            Objects.requireNonNull(other);
            if (other instanceof ReusableLogEvent) {
                ((ReusableLogEvent) other).initializeBuilder(this);
                return;
            }
            this.loggerFqcn = other.getLoggerFqcn();
            this.marker = other.getMarker();
            this.level = other.getLevel();
            this.loggerName = other.getLoggerName();
            this.message = other.getMessage();
            this.instant.initFrom(other.getInstant());
            this.thrown = other.getThrown();
            this.contextStack = other.getContextStack();
            this.includeLocation = other.isIncludeLocation();
            this.endOfBatch = other.isEndOfBatch();
            this.nanoTime = other.getNanoTime();

            initDefaultContextData();
            // Avoid unnecessarily initializing thrownProxy, threadName and source if possible
            if (other instanceof Log4jLogEvent) {
                final Log4jLogEvent evt = (Log4jLogEvent) other;
                this.contextData = evt.contextData;
                this.thrownProxy = evt.thrownProxy;
                this.source = evt.source;
                this.threadId = evt.threadId;
                this.threadName = evt.threadName;
                this.threadPriority = evt.threadPriority;
            } else {
                if (other.getContextData() instanceof StringMap) {
                    this.contextData = (StringMap) other.getContextData();
                } else {
                    if (this.contextData.isFrozen()) {
                        this.contextData = ContextDataFactory.createContextData();
                    } else {
                        this.contextData.clear();
                    }
                    this.contextData.putAll(other.getContextData());
                }
                this.thrownProxy = other.getThrownProxy();
                this.source = other.getSource();
                this.threadId = other.getThreadId();
                this.threadName = other.getThreadName();
                this.threadPriority = other.getThreadPriority();
            }
        }

        public Builder setLevel(final Level level) {
            this.level = level;
            return this;
        }

        public Builder setLoggerFqcn(final String loggerFqcn) {
            this.loggerFqcn = loggerFqcn;
            return this;
        }

        public Builder setLoggerName(final String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder setMarker(final Marker marker) {
            this.marker = marker;
            return this;
        }

        public Builder setMessage(final Message message) {
            this.message = message;
            return this;
        }

        public Builder setThrown(final Throwable thrown) {
            this.thrown = thrown;
            return this;
        }

        public Builder setTimeMillis(final long timeMillis) {
            this.instant.initFromEpochMilli(timeMillis, 0);
            return this;
        }

        public Builder setInstant(final Instant instant) {
            this.instant.initFrom(instant);
            return this;
        }

        public Builder setThrownProxy(final ThrowableProxy thrownProxy) {
            this.thrownProxy = thrownProxy;
            return this;
        }

        public Builder setContextData(final StringMap contextData) {
            this.contextData = contextData;
            return this;
        }

        public Builder setContextStack(final ThreadContext.ContextStack contextStack) {
            this.contextStack = contextStack;
            return this;
        }

        public Builder setThreadId(final long threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder setThreadName(final String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder setThreadPriority(final int threadPriority) {
            this.threadPriority = threadPriority;
            return this;
        }

        public Builder setSource(final StackTraceElement source) {
            this.source = source;
            return this;
        }

        public Builder setIncludeLocation(final boolean includeLocation) {
            this.includeLocation = includeLocation;
            return this;
        }

        public Builder setEndOfBatch(final boolean endOfBatch) {
            this.endOfBatch = endOfBatch;
            return this;
        }

        /**
         * Sets the nano time for the event.
         * @param nanoTime The value of the running Java Virtual Machine's high-resolution time source when the event
         *          was created.
         * @return this builder
         */
        public Builder setNanoTime(final long nanoTime) {
            this.nanoTime = nanoTime;
            return this;
        }

        public Builder setClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setContextDataInjector(final ContextDataInjector contextDataInjector) {
            this.contextDataInjector = contextDataInjector;
            return this;
        }

        @Override
        public Log4jLogEvent build() {
            initTimeFields();
            final Log4jLogEvent result = new Log4jLogEvent(
                    loggerName,
                    marker,
                    loggerFqcn,
                    level,
                    message,
                    thrown,
                    thrownProxy,
                    contextData,
                    contextStack,
                    threadId,
                    threadName,
                    threadPriority,
                    source,
                    instant.getEpochMillisecond(),
                    instant.getNanoOfMillisecond(),
                    nanoTime);
            result.setIncludeLocation(includeLocation);
            result.setEndOfBatch(endOfBatch);
            return result;
        }

        private void initTimeFields() {
            if (instant.getEpochMillisecond() == 0) {
                if (message instanceof TimestampMessage) {
                    instant.initFromEpochMilli(((TimestampMessage) message).getTimestamp(), 0);
                } else {
                    instant.initFrom(clock != null ? clock : ClockFactory.getClock());
                }
            }
        }

        private void initDefaultContextData() {
            contextDataInjector = ContextDataInjectorFactory.createInjector();
            contextData = contextDataInjector.injectContextData(null, ContextDataFactory.createContextData());
        }
    }

    /**
     * Returns a new empty {@code Log4jLogEvent.Builder} with all fields empty.
     * @return a new empty builder.
     */
    public static Builder newBuilder() {
        return new Builder().setLoggerName(Strings.EMPTY);
    }

    public Log4jLogEvent() {
        this(Strings.EMPTY, null, Strings.EMPTY, null, null, null, null, null, null, 0, null, 0, null, 0, 0, 0);
    }

    /**
     * Constructor.
     * @param loggerName The name of the Logger.
     * @param marker The Marker or null.
     * @param loggerFQCN The fully qualified class name of the caller.
     * @param level The logging Level.
     * @param message The Message.
     * @param thrown A Throwable or null.
     * @param thrownProxy A ThrowableProxy or null.
     * @param contextData The key-value pairs from the context.
     * @param contextStack the nested diagnostic context.
     * @param threadId the thread ID
     * @param threadName The name of the thread.
     * @param threadPriority the thread priority
     * @param source The locations of the caller.
     * @param timestampMillis The timestamp of the event.
     * @param nanoOfMillisecond the nanoseconds within the millisecond, always positive, never exceeds {@code 999,999}
     * @param nanoTime The value of the running Java Virtual Machine's high-resolution time source when the event was
     *          created.
     */
    private Log4jLogEvent(
            final String loggerName,
            final Marker marker,
            final String loggerFQCN,
            final Level level,
            final Message message,
            final Throwable thrown,
            final ThrowableProxy thrownProxy,
            final StringMap contextData,
            final ThreadContext.ContextStack contextStack,
            final long threadId,
            final String threadName,
            final int threadPriority,
            final @Nullable StackTraceElement source,
            final long timestampMillis,
            final int nanoOfMillisecond,
            final long nanoTime) {
        this.loggerName = loggerName;
        this.marker = marker;
        this.loggerFqcn = loggerFQCN;
        this.level = level == null ? Level.OFF : level; // LOG4J2-462, LOG4J2-465
        this.message = message;
        this.thrown = thrown;
        this.thrownProxy = thrownProxy;
        this.contextData = contextData == null ? ContextDataFactory.createContextData() : contextData;
        this.contextStack = contextStack == null ? ThreadContext.EMPTY_STACK : contextStack;
        this.threadId = threadId;
        this.threadName = threadName;
        this.threadPriority = threadPriority;
        this.source = source;
        if (message instanceof LoggerNameAwareMessage) {
            ((LoggerNameAwareMessage) message).setLoggerName(loggerName);
        }
        this.nanoTime = nanoTime;
        final long millis =
                message instanceof TimestampMessage ? ((TimestampMessage) message).getTimestamp() : timestampMillis;
        instant.initFromEpochMilli(millis, nanoOfMillisecond);
    }

    /**
     * Returns a new fully initialized {@code Log4jLogEvent.Builder} containing a copy of all fields of this event.
     * @return a new fully initialized builder.
     */
    public Builder asBuilder() {
        return new Builder(this);
    }

    @Override
    public Log4jLogEvent toImmutable() {
        if (getMessage() instanceof ReusableMessage) {
            makeMessageImmutable();
        }
        return this;
    }

    /**
     * Returns the logging Level.
     * @return the Level associated with this event.
     */
    @Override
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the name of the Logger used to generate the event.
     * @return The Logger name.
     */
    @Override
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * Returns the Message associated with the event.
     * @return The Message.
     */
    @Override
    public Message getMessage() {
        return message;
    }

    public void makeMessageImmutable() {
        message = new MementoMessage(message.getFormattedMessage(), message.getFormat(), message.getParameters());
    }

    @Override
    public long getThreadId() {
        if (threadId == 0) {
            threadId = Thread.currentThread().getId();
        }
        return threadId;
    }

    /**
     * Returns the name of the Thread on which the event was generated.
     * @return The name of the Thread.
     */
    @Override
    public String getThreadName() {
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
        return threadName;
    }

    @Override
    public int getThreadPriority() {
        if (threadPriority == 0) {
            threadPriority = Thread.currentThread().getPriority();
        }
        return threadPriority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeMillis() {
        return instant.getEpochMillisecond();
    }

    /**
     * {@inheritDoc}
     * @since 2.11.0
     */
    @Override
    public Instant getInstant() {
        return instant;
    }

    /**
     * Returns the Throwable associated with the event, or null.
     * @return The Throwable associated with the event.
     */
    @Override
    public Throwable getThrown() {
        return thrown;
    }

    /**
     * Returns the ThrowableProxy associated with the event, or null.
     * @return The ThrowableProxy associated with the event.
     */
    @Override
    public ThrowableProxy getThrownProxy() {
        if (thrownProxy == null && thrown != null) {
            thrownProxy = new ThrowableProxy(thrown);
        }
        return thrownProxy;
    }

    /**
     * Returns the Marker associated with the event, or null.
     * @return the Marker associated with the event.
     */
    @Override
    public Marker getMarker() {
        return marker;
    }

    /**
     * The fully qualified class name of the class that was called by the caller.
     * @return the fully qualified class name of the class that is performing logging.
     */
    @Override
    public String getLoggerFqcn() {
        return loggerFqcn;
    }

    /**
     * Returns the {@code ReadOnlyStringMap} containing context data key-value pairs.
     * @return the {@code ReadOnlyStringMap} containing context data key-value pairs
     * @since 2.7
     */
    @Override
    public ReadOnlyStringMap getContextData() {
        return contextData;
    }

    /**
     * Returns an immutable copy of the ThreadContext stack.
     * @return The context Stack.
     */
    @Override
    public ThreadContext.ContextStack getContextStack() {
        return contextStack;
    }

    /**
     * Returns the StackTraceElement for the caller. This will be the entry that occurs right
     * before the first occurrence of FQCN as a class name.
     * @return the StackTraceElement for the caller.
     */
    @Override
    public StackTraceElement getSource() {
        if (source == null && loggerFqcn != null) {
            source = includeLocation ? StackLocatorUtil.calcLocation(loggerFqcn) : null;
        }
        return peekSource();
    }

    @Override
    public @Nullable StackTraceElement peekSource() {
        return source;
    }

    @Override
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    @Override
    public void setIncludeLocation(final boolean includeLocation) {
        this.includeLocation = includeLocation;
    }

    @Override
    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    @Override
    public void setEndOfBatch(final boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String n = loggerName.isEmpty() ? LoggerConfig.ROOT : loggerName;
        sb.append("Logger=").append(n);
        sb.append(" Level=").append(level.name());
        sb.append(" Message=").append(message == null ? null : message.getFormattedMessage());
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Log4jLogEvent that = (Log4jLogEvent) o;

        if (endOfBatch != that.endOfBatch) {
            return false;
        }
        if (includeLocation != that.includeLocation) {
            return false;
        }
        if (!instant.equals(that.instant)) {
            return false;
        }
        if (nanoTime != that.nanoTime) {
            return false;
        }
        if (loggerFqcn != null ? !loggerFqcn.equals(that.loggerFqcn) : that.loggerFqcn != null) {
            return false;
        }
        if (level != null ? !level.equals(that.level) : that.level != null) {
            return false;
        }
        if (source != null ? !source.equals(that.source) : that.source != null) {
            return false;
        }
        if (marker != null ? !marker.equals(that.marker) : that.marker != null) {
            return false;
        }
        if (contextData != null ? !contextData.equals(that.contextData) : that.contextData != null) {
            return false;
        }
        if (!message.equals(that.message)) {
            return false;
        }
        if (!loggerName.equals(that.loggerName)) {
            return false;
        }
        if (contextStack != null ? !contextStack.equals(that.contextStack) : that.contextStack != null) {
            return false;
        }
        if (threadId != that.threadId) {
            return false;
        }
        if (threadName != null ? !threadName.equals(that.threadName) : that.threadName != null) {
            return false;
        }
        if (threadPriority != that.threadPriority) {
            return false;
        }
        if (!Objects.equals(thrown, that.thrown)) {
            return false;
        }
        if (!Objects.equals(thrownProxy, that.thrownProxy)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        // Check:OFF: MagicNumber
        int result = loggerFqcn != null ? loggerFqcn.hashCode() : 0;
        result = 31 * result + (marker != null ? marker.hashCode() : 0);
        result = 31 * result + (level != null ? level.hashCode() : 0);
        result = 31 * result + loggerName.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + instant.hashCode();
        result = 31 * result + (int) (nanoTime ^ (nanoTime >>> 32));
        result = 31 * result + (thrown != null ? thrown.hashCode() : 0);
        result = 31 * result + (thrownProxy != null ? thrownProxy.hashCode() : 0);
        result = 31 * result + (contextData != null ? contextData.hashCode() : 0);
        result = 31 * result + (contextStack != null ? contextStack.hashCode() : 0);
        result = 31 * result + (int) (threadId ^ (threadId >>> 32));
        result = 31 * result + (threadName != null ? threadName.hashCode() : 0);
        result = 31 * result + threadPriority;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (includeLocation ? 1 : 0);
        result = 31 * result + (endOfBatch ? 1 : 0);
        // Check:ON: MagicNumber
        return result;
    }
}
