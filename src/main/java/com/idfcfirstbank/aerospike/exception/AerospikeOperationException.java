package com.idfcfirstbank.aerospike.exception;

public final class AerospikeOperationException extends RuntimeException {

    private final String operation;
    private final AerospikeErrorType errorType;

    public AerospikeOperationException(String operation, AerospikeErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.errorType = errorType;
    }

    public String getOperation() {
        return operation;
    }

    public AerospikeErrorType getErrorType() {
        return errorType;
    }
}
