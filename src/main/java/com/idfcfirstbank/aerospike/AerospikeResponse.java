package com.idfcfirstbank.aerospike;

import com.aerospike.client.Record;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AerospikeResponse {

    private AerospikeResponse() {
    }

    public static Map<String, Object> record(Object key, Record record) {
        Map<String, Object> response = base(key, record != null);
        response.put("found", record != null);
        if (record != null) {
            response.put("bins", record.bins == null ? new LinkedHashMap<String, Object>() : record.bins);
            response.put("generation", record.generation);
            response.put("expiration", record.expiration);
        }
        return response;
    }

    public static Map<String, Object> success(Object key, boolean value) {
        Map<String, Object> response = base(key, true);
        response.put("value", value);
        return response;
    }

    private static Map<String, Object> base(Object key, boolean success) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("success", success);
        response.put("key", key);
        return response;
    }
}
