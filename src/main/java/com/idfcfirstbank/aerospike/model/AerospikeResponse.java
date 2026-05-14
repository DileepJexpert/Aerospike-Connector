package com.idfcfirstbank.aerospike.model;

import com.aerospike.client.Record;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AerospikeResponse {

    private AerospikeResponse() {
    }

    public static Map<String, Object> record(String namespace, String setName, String key, Record record) {
        Map<String, Object> response = base(namespace, setName, key);
        response.put("found", record != null);

        if (record != null) {
            response.put("success", true);
            response.put("bins", record.bins);
            response.put("generation", record.generation);
            response.put("expiration", record.expiration);
        } else {
            response.put("success", false);
        }

        return response;
    }

    public static Map<String, Object> success(String namespace, String setName, String key, String operation, boolean value) {
        Map<String, Object> response = base(namespace, setName, key);
        response.put("success", value);
        response.put("operation", operation);
        return response;
    }

    public static Map<String, Object> exists(String namespace, String setName, String key, boolean value) {
        Map<String, Object> response = base(namespace, setName, key);
        response.put("success", true);
        response.put("operation", "exists");
        response.put("exists", value);
        return response;
    }

    private static Map<String, Object> base(String namespace, String setName, String key) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("namespace", namespace);
        response.put("set", setName);
        response.put("key", key);
        return response;
    }
}
