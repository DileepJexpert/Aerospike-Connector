package com.idfcfirstbank.aerospike.internal.util;

import com.idfcfirstbank.aerospike.internal.error.AerospikeError;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Shared validation helpers for connector inputs.
 */
public final class AerospikeValidation {

    private AerospikeValidation() {
    }

    public static void requireNotBlank(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new ModuleException(name + " cannot be null or blank", AerospikeError.VALIDATION_FAILED);
        }
    }

    public static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new ModuleException(name + " cannot be negative", AerospikeError.VALIDATION_FAILED);
        }
    }
}
