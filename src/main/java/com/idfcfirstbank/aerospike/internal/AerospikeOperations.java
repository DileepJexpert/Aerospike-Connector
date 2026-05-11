package com.idfcfirstbank.aerospike.internal;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.DisplayName;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.HashMap;
import java.util.Map;

/**
 * Mule operations exposed by the Aerospike connector.
 */
public class AerospikeOperations {

    @MediaType(value = "application/json", strict = false)
    @DisplayName("Get Record")
    public Map<String, Object> getRecord(@Connection AerospikeConnection connection,
                                         @Parameter String namespace,
                                         @Parameter String setName,
                                         @Parameter String key) {
        AerospikeClient client = connection.getClient();
        Record record = client.get(null, new Key(namespace, setName, key));
        return toResponse(namespace, setName, key, record);
    }

    @MediaType(value = "application/json", strict = false)
    @DisplayName("Put Record")
    public Map<String, Object> putRecord(@Connection AerospikeConnection connection,
                                         @Parameter String namespace,
                                         @Parameter String setName,
                                         @Parameter String key,
                                         @Parameter Map<String, Object> bins,
                                         @Parameter @Optional(defaultValue = "0") int ttlSeconds) {
        AerospikeClient client = connection.getClient();
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.expiration = ttlSeconds;

        client.put(writePolicy, new Key(namespace, setName, key), toBins(bins));

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("success", true);
        response.put("operation", "put-record");
        response.put("namespace", namespace);
        response.put("set", setName);
        response.put("key", key);
        return response;
    }

    @MediaType(value = "application/json", strict = false)
    @DisplayName("Delete Record")
    public Map<String, Object> deleteRecord(@Connection AerospikeConnection connection,
                                            @Parameter String namespace,
                                            @Parameter String setName,
                                            @Parameter String key) {
        AerospikeClient client = connection.getClient();
        boolean deleted = client.delete(null, new Key(namespace, setName, key));

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("success", deleted);
        response.put("operation", "delete-record");
        response.put("namespace", namespace);
        response.put("set", setName);
        response.put("key", key);
        return response;
    }

    @MediaType(value = "application/json", strict = false)
    @DisplayName("Record Exists")
    public Map<String, Object> exists(@Connection AerospikeConnection connection,
                                      @Parameter String namespace,
                                      @Parameter String setName,
                                      @Parameter String key) {
        AerospikeClient client = connection.getClient();
        boolean exists = client.exists(null, new Key(namespace, setName, key));

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("exists", exists);
        response.put("namespace", namespace);
        response.put("set", setName);
        response.put("key", key);
        return response;
    }

    private Bin[] toBins(Map<String, Object> bins) {
        if (bins == null || bins.isEmpty()) {
            throw new IllegalArgumentException("bins map is required and cannot be empty");
        }

        Bin[] result = new Bin[bins.size()];
        int index = 0;
        for (Map.Entry<String, Object> entry : bins.entrySet()) {
            result[index++] = new Bin(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private Map<String, Object> toResponse(String namespace, String setName, String key, Record record) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("namespace", namespace);
        response.put("set", setName);
        response.put("key", key);

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
}
