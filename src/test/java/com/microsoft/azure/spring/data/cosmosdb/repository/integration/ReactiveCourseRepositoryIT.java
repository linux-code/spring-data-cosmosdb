/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.azure.data.cosmos.PartitionKey;
import com.microsoft.azure.spring.data.cosmosdb.core.ReactiveCosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.domain.Course;
import com.microsoft.azure.spring.data.cosmosdb.exception.CosmosDBAccessException;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.repository.ReactiveCourseRepository;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collections;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class ReactiveCourseRepositoryIT {

    private static final String COURSE_ID_1 = "1";
    private static final String COURSE_ID_2 = "2";
    private static final String COURSE_ID_3 = "3";
    private static final String COURSE_ID_4 = "4";
    private static final String COURSE_ID_5 = "5";

    private static final String COURSE_NAME_1 = "Course1";
    private static final String COURSE_NAME_2 = "Course2";
    private static final String COURSE_NAME_3 = "Course3";
    private static final String COURSE_NAME_4 = "Course4";
    private static final String COURSE_NAME_5 = "Course5";

    private static final String DEPARTMENT_NAME_1 = "Department1";
    private static final String DEPARTMENT_NAME_2 = "Department2";
    private static final String DEPARTMENT_NAME_3 = "Department3";

    private static final Course COURSE_1 = new Course(COURSE_ID_1, COURSE_NAME_1, DEPARTMENT_NAME_3);
    private static final Course COURSE_2 = new Course(COURSE_ID_2, COURSE_NAME_2, DEPARTMENT_NAME_2);
    private static final Course COURSE_3 = new Course(COURSE_ID_3, COURSE_NAME_3, DEPARTMENT_NAME_2);
    private static final Course COURSE_4 = new Course(COURSE_ID_4, COURSE_NAME_4, DEPARTMENT_NAME_1);
    private static final Course COURSE_5 = new Course(COURSE_ID_5, COURSE_NAME_5, DEPARTMENT_NAME_1);

    private final CosmosEntityInformation<Course, String> entityInformation =
        new CosmosEntityInformation<>(Course.class);

    @Autowired
    private ReactiveCosmosTemplate template;

    @Autowired
    private ReactiveCourseRepository repository;

    @PreDestroy
    public void cleanUpCollection() {
        template.deleteContainer(entityInformation.getCollectionName());
    }

    @Before
    public void setup() {
        final Flux<Course> savedFlux = repository.saveAll(Arrays.asList(COURSE_1, COURSE_2,
            COURSE_3, COURSE_4));
        StepVerifier.create(savedFlux).thenConsumeWhile(course -> true).expectComplete().verify();
    }

    @After
    public void cleanup() {
        final Mono<Void> deletedMono = repository.deleteAll();
        StepVerifier.create(deletedMono).thenAwait().verifyComplete();
    }

    @Test
    public void testFindById() {
        final Mono<Course> idMono = repository.findById(COURSE_ID_4);
        StepVerifier.create(idMono).expectNext(COURSE_4).expectComplete().verify();
    }

    @Test
    public void testFindByIdAndPartitionKey() {
        final Mono<Course> idMono = repository.findById(COURSE_ID_4,
            new PartitionKey(entityInformation.getPartitionKeyFieldValue(COURSE_4)));
        StepVerifier.create(idMono).expectNext(COURSE_4).expectComplete().verify();
    }

    @Test
    public void testFindByIdAsPublisher() {
        final Mono<Course> byId = repository.findById(Mono.just(COURSE_ID_1));
        StepVerifier.create(byId).expectNext(COURSE_1).verifyComplete();
    }

    @Test
    public void testFindAllWithSort() {
        final Flux<Course> sortAll = repository.findAll(Sort.by(Sort.Order.desc("name")));
        StepVerifier.create(sortAll).expectNext(COURSE_4, COURSE_3, COURSE_2, COURSE_1).verifyComplete();
    }

    @Test
    public void testFindByIdNotFound() {
        final Mono<Course> idMono = repository.findById("10");
        //  Expect an empty mono as return value
        StepVerifier.create(idMono).expectComplete().verify();
    }

    @Test
    public void testFindByIdAndPartitionKeyNotFound() {
        final Mono<Course> idMono = repository.findById("10",
            new PartitionKey(entityInformation.getPartitionKeyFieldValue(COURSE_1)));
        //  Expect an empty mono as return value
        StepVerifier.create(idMono).expectComplete().verify();
    }

    @Test
    public void testFindAll() {
        final Flux<Course> allFlux = repository.findAll();
        StepVerifier.create(allFlux).expectNextCount(4).verifyComplete();
    }

    @Test
    public void testInsert() {
        final Mono<Course> save = repository.save(COURSE_5);
        StepVerifier.create(save).expectNext(COURSE_5).verifyComplete();
    }

    @Test
    public void testUpsert() {
        Mono<Course> save = repository.save(COURSE_1);
        StepVerifier.create(save).expectNext(COURSE_1).expectComplete().verify();

        save = repository.save(COURSE_1);
        StepVerifier.create(save).expectNext(COURSE_1).expectComplete().verify();
    }

    @Test
    public void testDeleteByIdWithoutPartitionKey() {
        final Mono<Void> deleteMono = repository.deleteById(COURSE_1.getCourseId());
        StepVerifier.create(deleteMono).expectError(CosmosDBAccessException.class).verify();
    }

    @Test
    public void testDeleteByIdAndPartitionKey() {
        final Mono<Void> deleteMono = repository.deleteById(COURSE_1.getCourseId(),
            new PartitionKey(entityInformation.getPartitionKeyFieldValue(COURSE_1)));
        StepVerifier.create(deleteMono).verifyComplete();

        final Mono<Course> byId = repository.findById(COURSE_ID_1,
            new PartitionKey(entityInformation.getPartitionKeyFieldValue(COURSE_1)));
        //  Expect an empty mono as return value
        StepVerifier.create(byId).verifyComplete();
    }

    @Test
    public void testDeleteByEntity() {
        final Mono<Void> deleteMono = repository.delete(COURSE_4);
        StepVerifier.create(deleteMono).verifyComplete();

        final Mono<Course> byId = repository.findById(COURSE_ID_4);
        //  Expect an empty mono as return value
        StepVerifier.create(byId).expectComplete().verify();
    }

    @Test
    public void testDeleteByIdNotFound() {
        final Mono<Void> deleteMono = repository.deleteById(COURSE_ID_5);
        StepVerifier.create(deleteMono).expectError(CosmosDBAccessException.class).verify();
    }

    @Test
    public void testDeleteByEntityNotFound() {
        final Mono<Void> deleteMono = repository.delete(COURSE_5);
        StepVerifier.create(deleteMono).expectError(CosmosDBAccessException.class).verify();
    }

    @Test
    public void testCountAll() {
        final Mono<Long> countMono = repository.count();
        StepVerifier.create(countMono).expectNext(4L).verifyComplete();
    }

    @Test
    public void testFindByDepartmentIn() {
        final Flux<Course> byDepartmentIn =
            repository.findByDepartmentIn(Collections.singletonList(DEPARTMENT_NAME_2));
        StepVerifier.create(byDepartmentIn).expectNextCount(2).verifyComplete();
    }
}
