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
package org.apache.logging.log4j.plugins.validation.validators;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.plugins.validation.constraints.RequiredProperty;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Validator that checks that a property exists and has the correct value if a value is required.
 *
 * @since 3.0.0
 */
public class RequiredPropertyValidator implements ConstraintValidator<RequiredProperty> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private RequiredProperty annotation;

    @Override
    public void initialize(final RequiredProperty anAnnotation) {
        this.annotation = anAnnotation;
    }

    @Override
    public boolean isValid(final String name, final Object value) {
        final String property = PropertyEnvironment.getGlobal().getStringProperty(annotation.name());
        if (property == null) {
            LOGGER.error("{} cannot be used. Required property {} is not defined", name, annotation.name());
            return false;
        }
        if (annotation.value().length() > 0 && !annotation.value().equalsIgnoreCase(property)) {
            LOGGER.error(
                    "{} cannot be used. Required property {} is not set to {}",
                    name,
                    annotation.name(),
                    annotation.value());
            return false;
        }
        return true;
    }
}
