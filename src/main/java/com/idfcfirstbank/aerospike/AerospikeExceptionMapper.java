package com.idfcfirstbank.aerospike;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;

public final class AerospikeExceptionMapper {

    private AerospikeExceptionMapper() {
    }

    public static AerospikeOperationException map(String operation, RuntimeException exception) {
        if (exception instanceof AerospikeOperationException) {
            return (AerospikeOperationException) exception;
        }

        String category = "UNKNOWN";
        if (exception instanceof AerospikeException) {
            category = category((AerospikeException) exception);
        } else if (exception instanceof IllegalArgumentException) {
            category = "VALIDATION";
        }

        return new AerospikeOperationException(
                operation,
                category,
                "Aerospike " + operation + " failed: " + exception.getMessage(),
                exception);
    }

    private static String category(AerospikeException exception) {
        switch (exception.getResultCode()) {
            case ResultCode.TIMEOUT:
                return "TIMEOUT";
            case ResultCode.SECURITY_NOT_SUPPORTED:
            case ResultCode.SECURITY_NOT_ENABLED:
            case ResultCode.INVALID_USER:
            case ResultCode.INVALID_PASSWORD:
                return "AUTHENTICATION";
            case ResultCode.KEY_NOT_FOUND_ERROR:
                return "RECORD_NOT_FOUND";
            case ResultCode.NO_MORE_CONNECTIONS:
            case ResultCode.SERVER_NOT_AVAILABLE:
                return "CONNECTIVITY";
            default:
                return "AEROSPIKE";
        }
    }
}
