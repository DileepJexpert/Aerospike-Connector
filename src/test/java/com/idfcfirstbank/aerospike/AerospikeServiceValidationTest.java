package com.idfcfirstbank.aerospike;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AerospikeServiceValidationTest {

    @Test
    void rejectsNullConfig() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AerospikeService(null));

        assertEquals("config must not be null", exception.getMessage());
    }

    @Test
    void getRecordRejectsBlankNamespaceBeforeConnection() {
        AerospikeService service = new AerospikeService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.getRecord(" ", "customer", "123"));

        assertEquals("VALIDATION", exception.getErrorCategory());
        assertTrue(exception.getMessage().contains("namespace must not be blank"));
    }

    @Test
    void putRecordRejectsEmptyBinsBeforeConnection() {
        AerospikeService service = new AerospikeService(new AerospikeConfig("127.0.0.1:3000"));

        AerospikeOperationException exception = assertThrows(AerospikeOperationException.class,
                () -> service.putRecord("test", "customer", "123", Collections.<String, Object>emptyMap(), 60));

        assertEquals("VALIDATION", exception.getErrorCategory());
        assertTrue(exception.getMessage().contains("bins must not be empty"));
    }
}
