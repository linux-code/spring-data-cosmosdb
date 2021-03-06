/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb;

import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBAccessException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.COSMOSDB_FAKE_CONNECTION_STRING;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.COSMOSDB_FAKE_HOST;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.COSMOSDB_FAKE_KEY;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.COSMOSDB_INVALID_FAKE_CONNECTION_STRING;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.DB_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@PropertySource(value = {"classpath:application.properties"})
@RunWith(SpringJUnit4ClassRunner.class)
public class CosmosDbFactoryTestIT {

    @Value("${cosmosdb.uri:}")
    private String cosmosDbUri;

    @Value("${cosmosdb.key:}")
    private String cosmosDbKey;

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyKey() {
        final CosmosDBConfig dbConfig = CosmosDBConfig.builder(COSMOSDB_FAKE_HOST, "", DB_NAME).build();
        new CosmosDbFactory(dbConfig);
    }

    @Test
    public void testInvalidEndpoint() {
        final CosmosDBConfig dbConfig =
                CosmosDBConfig.builder(COSMOSDB_FAKE_HOST, COSMOSDB_FAKE_KEY, DB_NAME).build();
        final CosmosDbFactory factory = new CosmosDbFactory(dbConfig);

        assertThat(factory).isNotNull();
    }

    @Test
    public void testConnectWithConnectionString() {
        final CosmosDBConfig dbConfig =
                CosmosDBConfig.builder(COSMOSDB_FAKE_CONNECTION_STRING, DB_NAME).build();
        final CosmosDbFactory factory = new CosmosDbFactory(dbConfig);

        assertThat(factory).isNotNull();
    }

    @Test(expected = CosmosDBAccessException.class)
    public void testInvalidConnectionString() {
        CosmosDBConfig.builder(COSMOSDB_INVALID_FAKE_CONNECTION_STRING, DB_NAME).build();
    }

    @Test
    public void testConnectionPolicyUserAgentKept() {
        final CosmosDBConfig dbConfig =
                CosmosDBConfig.builder(cosmosDbUri, cosmosDbKey, DB_NAME).build();
        final CosmosDbFactory factory = new CosmosDbFactory(dbConfig);
        factory.getCosmosClient();

        final String uaSuffix = factory.getConfig().getConnectionPolicy().userAgentSuffix();
        assertThat(uaSuffix).contains("spring-data");
    }
}
