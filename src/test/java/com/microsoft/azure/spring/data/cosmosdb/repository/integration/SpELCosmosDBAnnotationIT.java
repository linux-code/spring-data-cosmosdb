/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.spring.data.cosmosdb.CosmosDbFactory;
import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.config.CosmosDBConfig;
import com.microsoft.azure.spring.data.cosmosdb.core.CosmosTemplate;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingCosmosConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.convert.ObjectMapperFactory;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.CosmosMappingContext;
import com.microsoft.azure.spring.data.cosmosdb.domain.SpELBeanStudent;
import com.microsoft.azure.spring.data.cosmosdb.domain.SpELPropertyStudent;
import com.microsoft.azure.spring.data.cosmosdb.repository.TestRepositoryConfig;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.CosmosEntityInformation;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Persistent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 
 * @author Domenico Sibilio
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class SpELCosmosDBAnnotationIT {
    private static final SpELPropertyStudent TEST_PROPERTY_STUDENT = 
            new SpELPropertyStudent(TestConstants.ID_1, TestConstants.FIRST_NAME,
            TestConstants.LAST_NAME);

    @Value("${cosmosdb.uri}")
    private String dbUri;

    @Value("${cosmosdb.key}")
    private String dbKey;

    @Autowired
    private ApplicationContext applicationContext;

    private CosmosTemplate cosmosTemplate;
    private CosmosEntityInformation<SpELPropertyStudent, String> cosmosEntityInformation;

    @After
    public void cleanUp() {
        if (cosmosTemplate != null && cosmosEntityInformation != null) {
            cosmosTemplate.deleteCollection(cosmosEntityInformation.getCollectionName());
        }
    }
    
    @Test
    public void testDynamicCollectionNameWithPropertySourceExpression() {
        final CosmosEntityInformation<SpELPropertyStudent, Object> propertyStudentInfo =
                new CosmosEntityInformation<>(SpELPropertyStudent.class);
        
        assertEquals(TestConstants.DYNAMIC_PROPERTY_COLLECTION_NAME, propertyStudentInfo.getCollectionName());
    }
    
    @Test
    public void testDynamicCollectionNameWithBeanExpression() {
        final CosmosEntityInformation<SpELBeanStudent, Object> beanStudentInfo =
                new CosmosEntityInformation<>(SpELBeanStudent.class);
        
        assertEquals(TestConstants.DYNAMIC_BEAN_COLLECTION_NAME, beanStudentInfo.getCollectionName());
    }
    
    @Test
    public void testDatabaseOperationsOnDynamicallyNamedCollection() throws ClassNotFoundException {
      final CosmosDBConfig dbConfig = CosmosDBConfig.builder(dbUri, dbKey, TestConstants.DB_NAME).build();
      final CosmosDbFactory dbFactory = new CosmosDbFactory(dbConfig);

      cosmosEntityInformation = new CosmosEntityInformation<>(SpELPropertyStudent.class);
      final CosmosMappingContext dbContext = new CosmosMappingContext();
      dbContext.setInitialEntitySet(new EntityScanner(this.applicationContext).scan(Persistent.class));

      final ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
      final MappingCosmosConverter mappingConverter = new MappingCosmosConverter(dbContext, objectMapper);
      cosmosTemplate = new CosmosTemplate(dbFactory, mappingConverter, TestConstants.DB_NAME);
      
      cosmosTemplate.createCollectionIfNotExists(cosmosEntityInformation);

      final SpELPropertyStudent insertedRecord = 
              cosmosTemplate.insert(cosmosEntityInformation.getCollectionName(), TEST_PROPERTY_STUDENT, null);
      assertNotNull(insertedRecord);
      
      final SpELPropertyStudent readRecord = 
              cosmosTemplate.findById(TestConstants.DYNAMIC_PROPERTY_COLLECTION_NAME,
                      insertedRecord.getId(), SpELPropertyStudent.class);
      assertNotNull(readRecord);
    }

}

