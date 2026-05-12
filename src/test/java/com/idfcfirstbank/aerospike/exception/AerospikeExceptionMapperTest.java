package com.idfcfirstbank.aerospike.exception;

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
                AerospikeErrorType.WRITE_FAILURE,
                new IllegalArgumentException("bins map is required and cannot be empty"));

        assertEquals("putRecord", exception.getOperation());
        assertEquals(AerospikeErrorType.VALIDATION_FAILED, exception.getErrorType());
        assertTrue(exception.getMessage().contains("bins map is required"));
    }

    @Test
    void mapsAerospikeTimeoutErrors() {
        AerospikeOperationException exception = AerospikeExceptionMapper.map(
                "getRecord",
                AerospikeErrorType.UNKNOWN,
                new AerospikeException(ResultCode.TIMEOUT, "socket timeout"));

        assertEquals(AerospikeErrorType.TIMEOUT, exception.getErrorType());
        assertEquals("getRecord", exception.getOperation());
        assertTrue(exception.getMessage().contains("timed out"));
    }

    @Test
    void returnsExistingOperationExceptionAsIs() {
        AerospikeOperationException original = new AerospikeOperationException(
                "exists",
                AerospikeErrorType.CONNECTION_FAILED,
                "existing",
                null);

        AerospikeOperationException mapped = AerospikeExceptionMapper.map("ignored", AerospikeErrorType.UNKNOWN, original);

        assertSame(original, mapped);
    }
}
