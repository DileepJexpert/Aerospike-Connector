package com.idfcfirstbank.aerospike.exception;

public enum AerospikeErrorType {
    TIMEOUT,
    KEY_NOT_FOUND,
    CONNECTION_FAILED,
    INVALID_NAMESPACE,
    WRITE_FAILURE,
    DELETE_FAILURE,
    AUTHENTICATION_FAILED,
    VALIDATION_FAILED,
    UNKNOWN
}
