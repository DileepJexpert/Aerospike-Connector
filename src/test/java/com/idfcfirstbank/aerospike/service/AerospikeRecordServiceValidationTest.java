package com.idfcfirstbank.aerospike.service;

import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.exception.AerospikeErrorType;
import com.idfcfirstbank.aerospike.exception.AerospikeOperationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AerospikeRecordServiceValidationTest {

    @Test
    void rejectsNullConfig() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AerospikeRecordService(null));

        assertEquals("config must not be null", exception.getMessage());
    }

    @Test
    void getRecordRejectsBlankNamespaceBeforeConnection() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.getRecord(" ", "customer", "123"));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("namespace must not be blank"));
    }

    @Test
    void putRecordRejectsEmptyBinsBeforeConnection() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.putRecord("test", "customer", "123", Collections.<String, Object>emptyMap(), 60));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("bins map is required"));
    }

    @Test
    void getRecordWithFieldsRejectsBlankFieldNameBeforeConnection() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.getRecord("test", "customer", "123", Arrays.asList("name", " ")));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("fieldName must not be blank"));
    }

    @Test
    void queryByFieldEqualsRejectsFractionalNumberBeforeConnection() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.queryByFieldEquals("test", "customer", "salary", new BigDecimal("10.5"), Collections.singletonList("salary")));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("fieldValue must be an integer value"));
    }

    @Test
    void putRecordRejectsTtlBelowSupportedSentinel() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        Map<String, Object> bins = new LinkedHashMap<String, Object>();
        bins.put("name", "Dileep");

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.putRecord("test", "customer", "123", bins, -3));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("ttlSeconds must be >= -2"));
    }

    @Test
    void putRecordTreatsNeverExpireSentinelAsValidTtl() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        Map<String, Object> bins = new LinkedHashMap<String, Object>();
        bins.put("name", "Dileep");

        // TTL=-1 must pass validation. The call either succeeds (server running)
        // or fails with a non-validation error (server absent). Either is correct.
        try {
            service.putRecord("test", "customer", "ttl-sentinel-test", bins, -1);
        } catch (AerospikeOperationException exception) {
            assertNotEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        }
    }

    @Test
    void queryByFieldEqualsMapsIntegerOverflowToValidationFailure() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        BigInteger tooLarge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.queryByFieldEquals("test", "customer", "id", tooLarge, Collections.singletonList("id")));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("64-bit integer range"));
    }

    @Test
    void findAllRejectsBlankNamespaceBeforeConnection() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.findAll(" ", "customer", null));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("namespace must not be blank"));
    }

    @Test
    void queryRejectsNullCriteriaBeforeConnection() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.query("test", "customer", null, null));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("criteria must not be null"));
    }

    @Test
    void putRecordMapsBinIntegerOverflowToValidationFailure() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        Map<String, Object> bins = new LinkedHashMap<String, Object>();
        bins.put("balance", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.putRecord("test", "customer", "123", bins, 0));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("64-bit integer range"));
    }

    @Test
    void createRecordRejectsEmptyBins() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.createRecord("test", "customer", "123", Collections.<String, Object>emptyMap(), 0));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("bins map is required"));
    }

    @Test
    void updateRecordRejectsBlankSetName() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        Map<String, Object> bins = new LinkedHashMap<String, Object>();
        bins.put("name", "test");

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.updateRecord("test", " ", "123", bins, 0));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("setName must not be blank"));
    }

    @Test
    void putRecordIfGenerationRejectsNegativeGeneration() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        Map<String, Object> bins = new LinkedHashMap<String, Object>();
        bins.put("name", "test");

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.putRecordIfGeneration("test", "customer", "123", bins, 0, -1));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("expectedGeneration must not be negative"));
    }

    @Test
    void incrementBinsRejectsEmptyDeltas() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.incrementBins("test", "customer", "123", Collections.<String, Object>emptyMap(), 0));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("deltas map is required"));
    }

    @Test
    void incrementBinsRejectsNonNumericDelta() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        Map<String, Object> deltas = new LinkedHashMap<String, Object>();
        deltas.put("hits", "not-a-number");

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.incrementBins("test", "customer", "123", deltas, 0));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("must be numeric"));
    }

    @Test
    void touchRecordRejectsInvalidTtl() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.touchRecord("test", "customer", "123", -5));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("ttlSeconds must be >= -2"));
    }
}
