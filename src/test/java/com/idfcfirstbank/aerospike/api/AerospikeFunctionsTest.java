package com.idfcfirstbank.aerospike.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AerospikeFunctionsTest {

    @Test
    void exposesMuleFriendlyPutRecordWithConfigOverloadWithoutTtl() throws Exception {
        Method method = AerospikeFunctions.class.getMethod(
                "putRecordWithConfig",
                Map.class,
                String.class,
                String.class,
                Map.class);

        assertNotNull(method);
    }

    @Test
    void exposesMuleFriendlyPutRecordWithConfigOverloadWithNumberTtl() throws Exception {
        Method method = AerospikeFunctions.class.getMethod(
                "putRecordWithConfig",
                Map.class,
                String.class,
                String.class,
                Map.class,
                Number.class);

        assertNotNull(method);
    }

    @Test
    void exposesProjectedReadWithConfigMethod() throws Exception {
        Method method = AerospikeFunctions.class.getMethod(
                "getRecordFieldsWithConfig",
                Map.class,
                String.class,
                String.class,
                List.class);

        assertNotNull(method);
    }

    @Test
    void exposesProjectedBatchReadWithConfigMethod() throws Exception {
        Method method = AerospikeFunctions.class.getMethod(
                "batchGetFieldsWithConfig",
                Map.class,
                String.class,
                List.class,
                List.class);

        assertNotNull(method);
    }

    @Test
    void exposesQueryByFieldEqualsWithConfigMethod() throws Exception {
        Method method = AerospikeFunctions.class.getMethod(
                "queryRecordsByFieldEqualsWithConfig",
                Map.class,
                String.class,
                String.class,
                Object.class,
                List.class);

        assertNotNull(method);
    }

    @Test
    void exposesQueryByFieldRangeWithConfigMethod() throws Exception {
        Method method = AerospikeFunctions.class.getMethod(
                "queryRecordsByFieldRangeWithConfig",
                Map.class,
                String.class,
                String.class,
                Number.class,
                Number.class,
                List.class);

        assertNotNull(method);
    }
}
