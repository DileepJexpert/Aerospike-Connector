package com.idfcfirstbank.aerospike;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AerospikeExceptionMapperTest {

    @Test
    void mapsValidationErrors() {
        AerospikeOperationException exception = AerospikeExceptionMapper.map(
                "putRecord",
                new IllegalArgumentException("bins must not be empty"));

        assertEquals("putRecord", exception.getOperation());
        assertEquals("VALIDATION", exception.getErrorCategory());
        assertTrue(exception.getMessage().contains("bins must not be empty"));
    }

    @Test
    void mapsAerospikeTimeoutErrors() {
        AerospikeOperationException exception = AerospikeExceptionMapper.map(
                "getRecord",
                new AerospikeException(ResultCode.TIMEOUT, "socket timeout"));

        assertEquals("TIMEOUT", exception.getErrorCategory());
        assertEquals("getRecord", exception.getOperation());
        assertTrue(exception.getMessage().contains("socket timeout"));
    }

    @Test
    void returnsExistingOperationExceptionAsIs() {
        AerospikeOperationException original = new AerospikeOperationException(
                "exists",
                "CONNECTIVITY",
                "existing",
                null);

        AerospikeOperationException mapped = AerospikeExceptionMapper.map("ignored", original);

        assertSame(original, mapped);
    }
}
