package com.idfcfirstbank.aerospike.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * Mule error types exposed by the Aerospike connector.
 */
public enum AerospikeError implements ErrorTypeDefinition<AerospikeError> {
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
