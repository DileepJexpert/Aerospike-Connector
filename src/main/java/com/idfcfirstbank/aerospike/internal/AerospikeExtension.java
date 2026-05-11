package com.idfcfirstbank.aerospike.internal;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * Main entry point for the Aerospike Mule connector.
 */
@Xml(prefix = "aerospike")
@Extension(name = "Aerospike")
@Configurations(AerospikeConfiguration.class)
public class AerospikeExtension {
}
