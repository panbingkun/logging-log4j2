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
package org.apache.logging.log4j.core.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLConnection;
import java.util.Base64;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Provides the Basic Authorization header to a request.
 */
public class BasicAuthorizationProvider implements AuthorizationProvider {
    private static final String[] PREFIXES = {"log4j2.config.", "log4j2.Configuration.", "logging.auth."};
    private static final String AUTH_USER_NAME = "username";
    private static final String AUTH_PASSWORD = "password";
    private static final String AUTH_PASSWORD_DECRYPTOR = "passwordDecryptor";
    public static final String CONFIG_USER_NAME = "log4j2.configurationUserName";
    public static final String CONFIG_PASSWORD = "log4j2.configurationPassword";
    public static final String PASSWORD_DECRYPTOR = "log4j2.passwordDecryptor";

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Base64.Encoder encoder = Base64.getEncoder();

    private String authString = null;

    public BasicAuthorizationProvider(final PropertyEnvironment env) {
        final Properties props = env.getProperty(Properties.class);
        String password = props.password();
        if (props.passwordDecryptor() != null) {
            try {
                final PasswordDecryptor decryptor = LoaderUtil.newInstanceOf(props.passwordDecryptor);
                password = decryptor.decryptPassword(password);
            } catch (final Exception ex) {
                LOGGER.warn("Unable to decrypt password.", ex);
            }
        }
        if (props.username() != null && password != null) {
            authString = "Basic " + encoder.encodeToString((props.username() + ":" + password).getBytes(UTF_8));
        }
    }

    @Override
    public void addAuthorization(final URLConnection urlConnection) {
        if (authString != null) {
            urlConnection.setRequestProperty("Authorization", authString);
        }
    }

    /**
     * Configuration properties for HTTP Basic authentication.
     *
     * @param username The username.
     * @param password A possibly encrypted password.
     * @param passwordDecryptor
     */
    @Log4jProperty(name = "BasicAuthorizationProvider")
    public record Properties(String username, String password, Class<? extends PasswordDecryptor> passwordDecryptor) {}
}
