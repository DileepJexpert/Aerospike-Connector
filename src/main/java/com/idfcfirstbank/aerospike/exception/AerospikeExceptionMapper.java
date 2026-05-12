package com.idfcfirstbank.aerospike.exception;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;

public final class AerospikeExceptionMapper {

    private AerospikeExceptionMapper() {
    }

    public static AerospikeOperationException map(String operation, AerospikeErrorType defaultType, RuntimeException exception) {
        if (exception instanceof AerospikeOperationException) {
            return (AerospikeOperationException) exception;
        }

        if (exception instanceof IllegalArgumentException) {
            return new AerospikeOperationException(
                    operation,
                    AerospikeErrorType.VALIDATION_FAILED,
                    "Aerospike " + operation + " failed: " + exception.getMessage(),
                    exception);
        }

        if (exception instanceof AerospikeException.Timeout) {
            return new AerospikeOperationException(operation, AerospikeErrorType.TIMEOUT, "Aerospike " + operation + " timed out", exception);
        }

        if (exception instanceof AerospikeException.Connection) {
            return new AerospikeOperationException(operation, AerospikeErrorType.CONNECTION_FAILED, "Aerospike " + operation + " connection failed", exception);
        }

        if (exception instanceof AerospikeException) {
            AerospikeException aerospikeException = (AerospikeException) exception;
            int resultCode = aerospikeException.getResultCode();

            if (resultCode == ResultCode.TIMEOUT) {
                return new AerospikeOperationException(operation, AerospikeErrorType.TIMEOUT, "Aerospike " + operation + " timed out", exception);
            }
            if (resultCode == ResultCode.KEY_NOT_FOUND_ERROR) {
                return new AerospikeOperationException(operation, AerospikeErrorType.KEY_NOT_FOUND, "Aerospike " + operation + " record not found", exception);
            }
            if (resultCode == ResultCode.INVALID_NAMESPACE) {
                return new AerospikeOperationException(operation, AerospikeErrorType.INVALID_NAMESPACE, "Aerospike " + operation + " namespace is invalid", exception);
            }
            if (resultCode == ResultCode.INVALID_USER
                    || resultCode == ResultCode.INVALID_PASSWORD
                    || resultCode == ResultCode.SECURITY_NOT_ENABLED
                    || resultCode == ResultCode.SECURITY_NOT_SUPPORTED) {
                return new AerospikeOperationException(operation, AerospikeErrorType.AUTHENTICATION_FAILED, "Aerospike " + operation + " authentication failed", exception);
            }
        }

        String message = exception.getMessage() == null ? "Aerospike " + operation + " failed" : "Aerospike " + operation + " failed: " + exception.getMessage();
        return new AerospikeOperationException(operation, defaultType, message, exception);
    }
}
