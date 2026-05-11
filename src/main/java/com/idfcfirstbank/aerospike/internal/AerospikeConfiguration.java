package com.idfcfirstbank.aerospike.internal;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

/**
 * Global connector configuration.
 */
@Configuration(name = "config")
@Operations(AerospikeOperations.class)
@ConnectionProviders(AerospikeConnectionProvider.class)
public class AerospikeConfiguration {
}
