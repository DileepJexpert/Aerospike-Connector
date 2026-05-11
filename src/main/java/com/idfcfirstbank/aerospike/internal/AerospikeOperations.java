package com.idfcfirstbank.aerospike.internal;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.idfcfirstbank.aerospike.internal.error.AerospikeError;
import com.idfcfirstbank.aerospike.internal.error.AerospikeErrorTypeProvider;
import com.idfcfirstbank.aerospike.internal.model.RecordKey;
import com.idfcfirstbank.aerospike.internal.util.AerospikeValidation;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.DisplayName;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mule operations exposed by the Aerospike connector.
 */
public class AerospikeOperations {

    private static final Logger LOGGER = Logger.getLogger(AerospikeOperations.class.getName());

    @Throws(AerospikeErrorTypeProvider.class)
    @MediaType(value = "application/json", strict = false)
    @DisplayName("Get Record")
    public Map<String, Object> getRecord(@Connection AerospikeConnection connection,
                                         @ParameterGroup(name = "Record Key") RecordKey recordKey) {
        validateRecordKey(recordKey);
        try {
            AerospikeClient client = connection.getClient();
            LOGGER.fine("Aerospike GET ns=" + recordKey.getNamespace() + ", set=" + recordKey.getSetName());
            Record record = client.get(null, toKey(recordKey));
            return toResponse(recordKey, record);
        } catch (AerospikeException ex) {
            throw mapAerospikeException(ex);
        }
    }

    @Throws(AerospikeErrorTypeProvider.class)
    @MediaType(value = "application/json", strict = false)
    @DisplayName("Put Record")
    public Map<String, Object> putRecord(@Connection AerospikeConnection connection,
                                         @ParameterGroup(name = "Record Key") RecordKey recordKey,
                                         @Parameter Map<String, Object> bins,
                                         @Parameter @Optional(defaultValue = "0") int ttlSeconds) {
        validateRecordKey(recordKey);
        AerospikeValidation.requireNonNegative(ttlSeconds, "ttlSeconds");

        try {
            AerospikeClient client = connection.getClient();
            WritePolicy writePolicy = new WritePolicy();
            writePolicy.expiration = ttlSeconds;

            LOGGER.fine("Aerospike PUT ns=" + recordKey.getNamespace() + ", set=" + recordKey.getSetName());
            client.put(writePolicy, toKey(recordKey), toBins(bins));

            Map<String, Object> response = baseResponse(recordKey);
            response.put("success", true);
            response.put("operation", "put-record");
            return response;
        } catch (AerospikeException ex) {
            throw mapAerospikeException(ex);
        }
    }

    @Throws(AerospikeErrorTypeProvider.class)
    @MediaType(value = "application/json", strict = false)
    @DisplayName("Delete Record")
    public Map<String, Object> deleteRecord(@Connection AerospikeConnection connection,
                                            @ParameterGroup(name = "Record Key") RecordKey recordKey) {
        validateRecordKey(recordKey);
        try {
            AerospikeClient client = connection.getClient();
            LOGGER.fine("Aerospike DELETE ns=" + recordKey.getNamespace() + ", set=" + recordKey.getSetName());
            boolean deleted = client.delete(null, toKey(recordKey));

            Map<String, Object> response = baseResponse(recordKey);
            response.put("success", deleted);
            response.put("operation", "delete-record");
            return response;
        } catch (AerospikeException ex) {
            throw mapAerospikeException(ex);
        }
    }

    @Throws(AerospikeErrorTypeProvider.class)
    @MediaType(value = "application/json", strict = false)
    @DisplayName("Record Exists")
    public Map<String, Object> exists(@Connection AerospikeConnection connection,
                                      @ParameterGroup(name = "Record Key") RecordKey recordKey) {
        validateRecordKey(recordKey);
        try {
            AerospikeClient client = connection.getClient();
            LOGGER.fine("Aerospike EXISTS ns=" + recordKey.getNamespace() + ", set=" + recordKey.getSetName());
            boolean exists = client.exists(null, toKey(recordKey));

            Map<String, Object> response = baseResponse(recordKey);
            response.put("exists", exists);
            return response;
        } catch (AerospikeException ex) {
            throw mapAerospikeException(ex);
        }
    }

    private Key toKey(RecordKey recordKey) {
        return new Key(recordKey.getNamespace(), recordKey.getSetName(), recordKey.getKey());
    }

    private void validateRecordKey(RecordKey recordKey) {
        if (recordKey == null) {
            throw new ModuleException("recordKey is required", AerospikeError.VALIDATION_FAILED);
        }
        AerospikeValidation.requireNotBlank(recordKey.getNamespace(), "namespace");
        AerospikeValidation.requireNotBlank(recordKey.getSetName(), "setName");
        AerospikeValidation.requireNotBlank(recordKey.getKey(), "key");
    }

    private Bin[] toBins(Map<String, Object> bins) {
        if (bins == null || bins.isEmpty()) {
            throw new ModuleException("bins map is required and cannot be empty", AerospikeError.VALIDATION_FAILED);
        }

        Bin[] result = new Bin[bins.size()];
        int index = 0;
        for (Map.Entry<String, Object> entry : bins.entrySet()) {
            AerospikeValidation.requireNotBlank(entry.getKey(), "bin name");
            result[index++] = new Bin(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private Map<String, Object> toResponse(RecordKey recordKey, Record record) {
        Map<String, Object> response = baseResponse(recordKey);

        if (record == null) {
            response.put("found", false);
            response.put("bins", null);
            return response;
        }

        response.put("found", true);
        response.put("generation", record.generation);
        response.put("expiration", record.expiration);
        response.put("bins", record.bins);
        return response;
    }

    private Map<String, Object> baseResponse(RecordKey recordKey) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("namespace", recordKey.getNamespace());
        response.put("set", recordKey.getSetName());
        response.put("key", recordKey.getKey());
        return response;
    }

    private ModuleException mapAerospikeException(AerospikeException ex) {
        LOGGER.log(Level.WARNING, "Aerospike operation failed", ex);

        if (ex instanceof AerospikeException.Timeout) {
            return new ModuleException("Aerospike operation timed out", AerospikeError.TIMEOUT, ex);
        }
        if (ex instanceof AerospikeException.Connection) {
            return new ModuleException("Aerospike connection failed", AerospikeError.CONNECTION_FAILED, ex);
        }

        String message = ex.getMessage() == null ? "Aerospike operation failed" : ex.getMessage();
        return new ModuleException(message, AerospikeError.UNKNOWN, ex);
    }
}
