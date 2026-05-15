package com.idfcfirstbank.aerospike.service;

import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.exception.AerospikeErrorType;
import com.idfcfirstbank.aerospike.exception.AerospikeOperationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
