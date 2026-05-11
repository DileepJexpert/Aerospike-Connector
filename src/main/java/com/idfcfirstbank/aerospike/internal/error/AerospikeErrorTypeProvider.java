package com.idfcfirstbank.aerospike.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.ErrorTypeProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Registers Aerospike connector error types with Mule.
 */
public class AerospikeErrorTypeProvider implements ErrorTypeProvider {

    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
        return new HashSet<ErrorTypeDefinition>(Arrays.<ErrorTypeDefinition>asList(AerospikeError.values()));
    }
}
