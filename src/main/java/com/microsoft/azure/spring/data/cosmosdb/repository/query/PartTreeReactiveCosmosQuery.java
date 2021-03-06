/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import com.microsoft.azure.spring.data.cosmosdb.core.ReactiveCosmosOperations;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.CosmosPersistentProperty;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

public class PartTreeReactiveCosmosQuery extends AbstractReactiveCosmosQuery {

    private final PartTree tree;
    private final MappingContext<?, CosmosPersistentProperty> mappingContext;
    private final ResultProcessor processor;

    public PartTreeReactiveCosmosQuery(ReactiveCosmosQueryMethod method, ReactiveCosmosOperations operations) {
        super(method, operations);

        this.processor = method.getResultProcessor();
        this.tree = new PartTree(method.getName(), processor.getReturnedType().getDomainType());
        this.mappingContext = operations.getConverter().getMappingContext();
    }

    @Override
    protected DocumentQuery createQuery(ReactiveCosmosParameterAccessor accessor) {
        final ReactiveCosmosQueryCreator creator = new ReactiveCosmosQueryCreator(tree, accessor, mappingContext);

        final DocumentQuery query = creator.createQuery();

        if (tree.isLimiting()) {
            throw new NotImplementedException("Limiting is not supported.");
        }

        return query;
    }

    @Override
    protected boolean isDeleteQuery() {
        return tree.isDelete();
    }

    @Override
    protected boolean isExistsQuery() {
        return tree.isExistsProjection();
    }
}
