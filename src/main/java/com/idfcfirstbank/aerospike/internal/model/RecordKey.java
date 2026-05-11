package com.idfcfirstbank.aerospike.internal.model;

import org.mule.runtime.extension.api.annotation.param.DisplayName;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Reusable Mule parameter group for Aerospike record identity.
 */
public class RecordKey {

    @Parameter
    @DisplayName("Namespace")
    private String namespace;

    @Parameter
    @DisplayName("Set Name")
    private String setName;

    @Parameter
    @DisplayName("Key")
    private String key;

    public String getNamespace() {
        return namespace;
    }

    public String getSetName() {
        return setName;
    }

    public String getKey() {
        return key;
    }
}
