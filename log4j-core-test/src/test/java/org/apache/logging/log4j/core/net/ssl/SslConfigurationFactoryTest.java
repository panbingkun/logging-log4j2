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
package org.apache.logging.log4j.core.net.ssl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.core.impl.PropertyKeys;
import org.apache.logging.log4j.core.impl.PropertyKeys.TransportSecurity;
import org.apache.logging.log4j.core.test.net.ssl.TestConstants;
import org.junit.jupiter.api.Test;

public class SslConfigurationFactoryTest {

    private static PropertyKeys.KeyStore createKeyStoreProps() {
        return new PropertyKeys.KeyStore(
                TestConstants.KEYSTORE_FILE_RESOURCE, null, null, null, TestConstants.KEYSTORE_TYPE, null);
    }

    private static PropertyKeys.KeyStore createTrustStoreProps() {
        return new PropertyKeys.KeyStore(
                TestConstants.TRUSTSTORE_FILE_RESOURCE, null, null, null, TestConstants.KEYSTORE_TYPE, null);
    }

    @Test
    public void testStaticConfiguration() {
        final PropertyKeys.KeyStore keyStore = createKeyStoreProps();
        final PropertyKeys.KeyStore trustStore = createTrustStoreProps();
        TransportSecurity transportSecurity = TransportSecurity.defaultValue();
        // No keystore and truststore -> no SslConfiguration
        SslConfiguration sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity);
        assertNull(sslConfiguration);
        // Only keystore
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity.withKeyStore(keyStore));
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNull(sslConfiguration.getTrustStoreConfig());
        // Only truststore
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(transportSecurity.withTrustStore(trustStore));
        assertNotNull(sslConfiguration);
        assertNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());
        // Both
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(
                transportSecurity.withKeyStore(keyStore).withTrustStore(trustStore));
        assertNotNull(sslConfiguration);
        assertNotNull(sslConfiguration.getKeyStoreConfig());
        assertNotNull(sslConfiguration.getTrustStoreConfig());
    }
}
