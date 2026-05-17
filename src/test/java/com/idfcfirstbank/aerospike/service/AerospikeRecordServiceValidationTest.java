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

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.putRecord("test", "customer", "123", bins, -1));

        // -1 (never expire) must pass validation; failure here is the absent server, not a validation error.
        assertNotEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
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
    void putRecordMapsBinIntegerOverflowToValidationFailure() {
        AerospikeRecordService service = new AerospikeRecordService(new AerospikeConfig("127.0.0.1:3000"));
        Map<String, Object> bins = new LinkedHashMap<String, Object>();
        bins.put("balance", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.putRecord("test", "customer", "123", bins, 0));

        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("64-bit integer range"));
    }
}
