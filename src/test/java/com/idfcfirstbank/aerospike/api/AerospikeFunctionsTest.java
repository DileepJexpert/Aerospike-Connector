package com.idfcfirstbank.aerospike.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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
}
