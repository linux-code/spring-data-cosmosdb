/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosItemProperties;
import com.azure.data.cosmos.FeedOptions;
import com.azure.data.cosmos.FeedResponse;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.common.TestUtils;
import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CosmosPageRequest;
import com.microsoft.azure.spring.data.cosmosdb.domain.Address;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.PageableAddressRepository;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.PageTestUtils.validateNonLastPage;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.DB_NAME;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.PAGE_SIZE_1;
import static com.microsoft.azure.spring.data.cosmosdb.common.TestConstants.PAGE_SIZE_3;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class PageableAddressRepositoryIT {
    private static final Address TEST_ADDRESS1_PARTITION1 = new Address(
            TestConstants.POSTAL_CODE, TestConstants.STREET, TestConstants.CITY);
    private static final Address TEST_ADDRESS2_PARTITION1 = new Address(
            TestConstants.POSTAL_CODE_0, TestConstants.STREET, TestConstants.CITY);
    private static final Address TEST_ADDRESS1_PARTITION2 = new Address(
            TestConstants.POSTAL_CODE_1, TestConstants.STREET_0, TestConstants.CITY_0);
    private static final Address TEST_ADDRESS4_PARTITION3 = new Address(
            TestConstants.POSTAL_CODE, TestConstants.STREET_1, TestConstants.CITY_1);

    private final CosmosEntityInformation<Address, String> entityInformation =
            new CosmosEntityInformation<>(Address.class);

    @Autowired
    private CosmosTemplate template;

    @Autowired
    private PageableAddressRepository repository;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void setup() {
        repository.save(TEST_ADDRESS1_PARTITION1);
        repository.save(TEST_ADDRESS1_PARTITION2);
        repository.save(TEST_ADDRESS2_PARTITION1);
        repository.save(TEST_ADDRESS4_PARTITION3);
    }

    @PreDestroy
    public void cleanUpCollection() {
        template.deleteCollection(entityInformation.getCollectionName());
    }

    @After
    public void cleanup() {
        repository.deleteAll();
    }

    @Test
    public void testFindAll() {
        final List<Address> result = TestUtils.toList(repository.findAll());

        assertThat(result.size()).isEqualTo(4);
    }

    @Test
    public void testFindAllByPage() {
        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, PAGE_SIZE_3, null);
        final Page<Address> page = repository.findAll(pageRequest);

        assertThat(page.getContent().size()).isEqualTo(PAGE_SIZE_3);
        validateNonLastPage(page, PAGE_SIZE_3);

        final Page<Address> nextPage = repository.findAll(page.getPageable());
        assertThat(nextPage.getContent().size()).isEqualTo(1);
        validateLastPage(nextPage, nextPage.getContent().size());
    }

    @Test
    public void testFindWithParitionKeySinglePage() {
        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, PAGE_SIZE_3, null);
        final Page<Address> page = repository.findByCity(TestConstants.CITY, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(2);
        validateResultCityMatch(page, TestConstants.CITY);
        validateLastPage(page, page.getContent().size());
    }

    @Test
    public void testFindWithParitionKeyMultiPages() {
        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, PAGE_SIZE_1, null);
        final Page<Address> page = repository.findByCity(TestConstants.CITY, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateResultCityMatch(page, TestConstants.CITY);
        validateNonLastPage(page, PAGE_SIZE_1);

        final Page<Address> nextPage = repository.findByCity(TestConstants.CITY, page.getPageable());

        assertThat(nextPage.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateResultCityMatch(page, TestConstants.CITY);
        validateLastPage(nextPage, PAGE_SIZE_1);
    }

    @Test
    public void testFindWithoutPartitionKeySinglePage() {
        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, PAGE_SIZE_3, null);
        final Page<Address> page = repository.findByStreet(TestConstants.STREET, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(2);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateLastPage(page, page.getContent().size());
    }

    @Test
    public void testFindWithoutPartitionKeyMultiPages() {
        final CosmosPageRequest pageRequest = new CosmosPageRequest(0, PAGE_SIZE_1, null);
        final Page<Address> page = repository.findByStreet(TestConstants.STREET, pageRequest);

        assertThat(page.getContent().size()).isEqualTo(1);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateNonLastPage(page, PAGE_SIZE_1);

        final Page<Address> nextPage = repository.findByStreet(TestConstants.STREET, page.getPageable());

        assertThat(nextPage.getContent().size()).isEqualTo(PAGE_SIZE_1);
        validateResultStreetMatch(page, TestConstants.STREET);
        validateLastPage(nextPage, PAGE_SIZE_1);
    }

    @Test
    public void testOffsetAndLimit() {
        final int skipCount = 2;
        final int takeCount = 2;
        final List<CosmosItemProperties> results = new ArrayList<>();
        final FeedOptions options = new FeedOptions();
        options.enableCrossPartitionQuery(true);
        options.maxDegreeOfParallelism(2);

        final String query = "SELECT * from c OFFSET " + skipCount + " LIMIT " + takeCount;

        final CosmosClient cosmosClient = applicationContext.getBean(CosmosClient.class);
        final Flux<FeedResponse<CosmosItemProperties>> feedResponseFlux =
            cosmosClient.getDatabase(DB_NAME)
                        .getContainer(entityInformation.getCollectionName())
                        .queryItems(query, options);

        StepVerifier.create(feedResponseFlux)
                    .consumeNextWith(cosmosItemPropertiesFeedResponse ->
                        results.addAll(cosmosItemPropertiesFeedResponse.results()))
                    .verifyComplete();
        assertThat(results.size()).isEqualTo(takeCount);
    }

    private void validateResultCityMatch(Page<Address> page, String city) {
        assertThat((int) page.getContent()
                            .stream()
                            .filter(address -> address.getCity().equals(city))
                            .count()).isEqualTo(page.getContent().size());
    }

    private void validateResultStreetMatch(Page<Address> page, String street) {
        assertThat((int) page.getContent()
                            .stream()
                            .filter(address -> address.getStreet().equals(street))
                            .count()).isEqualTo(page.getContent().size());
    }
}
