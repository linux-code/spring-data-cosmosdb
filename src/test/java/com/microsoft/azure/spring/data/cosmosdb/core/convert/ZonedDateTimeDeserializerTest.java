/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.convert;

import org.junit.Test;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class ZonedDateTimeDeserializerTest {
    private static final ZonedDateTime ZONED_DATE_TIME
            = ZonedDateTime.of(2018, 10, 8, 15, 6, 7, 992000000, ZoneId.of("UTC"));
    private static final String OFFSET_DATE_TIME_WRAPPER_JSON = "{ \"zonedDateTime\": \""
            + ZONED_DATE_TIME.format(ISO_OFFSET_DATE_TIME) + "\" }";
    private static final String ZONED_DATE_TIME_WRAPPER_JSON = "{ \"zonedDateTime\": \""
            + ZONED_DATE_TIME.format(ISO_OFFSET_DATE_TIME) + "\" }";

    @Test
    public void deserializeZonedDateTime() throws IOException {
        final ZonedDateTimeWrapper wrapper = ObjectMapperFactory.getObjectMapper()
                .readValue(ZONED_DATE_TIME_WRAPPER_JSON, ZonedDateTimeWrapper.class);
        assertThat(wrapper.getZonedDateTime()).isEqualTo(ZONED_DATE_TIME);
    }

    @Test
    public void deserializeOffsetDateTime() throws IOException {
        final ZonedDateTimeWrapper wrapper = ObjectMapperFactory.getObjectMapper()
                .readValue(OFFSET_DATE_TIME_WRAPPER_JSON, ZonedDateTimeWrapper.class);
        assertThat(wrapper.getZonedDateTime()).isEqualTo(ZONED_DATE_TIME);
    }

    static final class ZonedDateTimeWrapper {
        ZonedDateTime zonedDateTime;

        public ZonedDateTime getZonedDateTime() {
            return zonedDateTime;
        }

        public void setZonedDateTime(ZonedDateTime zonedDateTime) {
            this.zonedDateTime = zonedDateTime;
        }
    }
}
