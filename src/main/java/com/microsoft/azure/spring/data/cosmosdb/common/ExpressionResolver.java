/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.common;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;

/**
 * 
 * @author Domenico Sibilio
 *
 */
public class ExpressionResolver {

    private static EmbeddedValueResolver embeddedValueResolver;
    
    public ExpressionResolver(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableBeanFactory) {
            setEmbeddedValueResolver(new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory));
        }
    }
    
    /**
     * Resolve the given string value via an {@link EmbeddedValueResolver}
     * @param expression the expression to be resolved
     * @return the resolved expression, may be {@literal null}
     */
    public static String resolveExpression(String expression) {
        return embeddedValueResolver != null
                ? embeddedValueResolver.resolveStringValue(expression)
                : expression;
    }

    private static void setEmbeddedValueResolver(EmbeddedValueResolver embeddedValueResolver) {
        ExpressionResolver.embeddedValueResolver = embeddedValueResolver;
    }

}
