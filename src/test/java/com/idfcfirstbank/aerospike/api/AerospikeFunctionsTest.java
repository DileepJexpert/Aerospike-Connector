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

    @Test
    void exposesFindAllWithConfigMethods() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod(
                "findAllWithConfig", Map.class, String.class));
        assertNotNull(AerospikeFunctions.class.getMethod(
                "findAllFieldsWithConfig", Map.class, String.class, List.class));
    }

    @Test
    void exposesCriteriaQueryWithConfigMethod() throws Exception {
        Method method = AerospikeFunctions.class.getMethod(
                "queryWithConfig",
                Map.class,
                String.class,
                Map.class,
                List.class);

        assertNotNull(method);
    }

    @Test
    void exposesCreateRecordWithConfigMethods() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod(
                "createRecordWithConfig", Map.class, String.class, String.class, Map.class, int.class));
        assertNotNull(AerospikeFunctions.class.getMethod(
                "createRecordWithConfig", Map.class, String.class, String.class, Map.class));
    }

    @Test
    void exposesReplaceRecordWithConfigMethods() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod(
                "replaceRecordWithConfig", Map.class, String.class, String.class, Map.class, int.class));
        assertNotNull(AerospikeFunctions.class.getMethod(
                "replaceRecordWithConfig", Map.class, String.class, String.class, Map.class));
    }

    @Test
    void exposesUpdateRecordWithConfigMethods() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod(
                "updateRecordWithConfig", Map.class, String.class, String.class, Map.class, int.class));
        assertNotNull(AerospikeFunctions.class.getMethod(
                "updateRecordWithConfig", Map.class, String.class, String.class, Map.class));
    }

    @Test
    void exposesPutRecordIfGenerationWithConfigMethod() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod(
                "putRecordIfGenerationWithConfig",
                Map.class, String.class, String.class, Map.class, int.class, int.class));
    }

    @Test
    void exposesIncrementBinsWithConfigMethods() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod(
                "incrementBinsWithConfig", Map.class, String.class, String.class, Map.class, int.class));
        assertNotNull(AerospikeFunctions.class.getMethod(
                "incrementBinsWithConfig", Map.class, String.class, String.class, Map.class));
    }

    @Test
    void exposesTouchRecordWithConfigMethod() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod(
                "touchRecordWithConfig", Map.class, String.class, String.class, int.class));
    }

    @Test
    void exposesPingMethods() throws Exception {
        assertNotNull(AerospikeFunctions.class.getMethod("ping", String.class));
        assertNotNull(AerospikeFunctions.class.getMethod("pingWithConfig", Map.class));
        assertNotNull(AerospikeFunctions.class.getMethod("pingWithAuth", String.class, String.class, String.class));
    }
}
