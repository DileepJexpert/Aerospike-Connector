package com.idfcfirstbank.aerospike;

public final class AerospikeOperationException extends RuntimeException {

    private final String operation;
    private final String errorCategory;

    public AerospikeOperationException(String operation, String errorCategory, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.errorCategory = errorCategory;
    }

    public String getOperation() {
        return operation;
    }

    public String getErrorCategory() {
        return errorCategory;
    }
}
