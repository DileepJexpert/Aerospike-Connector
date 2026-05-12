package com.idfcfirstbank.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class AerospikeService {

    private final AerospikeConfig config;

    public AerospikeService(AerospikeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    public Map<String, Object> getRecord(String namespace, String setName, Object key) {
        try {
            Record record = client().get(new Policy(), key(namespace, setName, key));
            return AerospikeResponse.record(key, record);
        } catch (RuntimeException e) {
            throw AerospikeExceptionMapper.map("getRecord", e);
        }
    }

    public Map<String, Object> putRecord(String namespace, String setName, Object key, Map<String, Object> bins, int ttlSeconds) {
        try {
            WritePolicy policy = new WritePolicy();
            policy.expiration = ttlSeconds;
            client().put(policy, key(namespace, setName, key), toBins(bins));
            return AerospikeResponse.success(key, true);
        } catch (RuntimeException e) {
            throw AerospikeExceptionMapper.map("putRecord", e);
        }
    }

    public Map<String, Object> deleteRecord(String namespace, String setName, Object key) {
        try {
            boolean existed = client().delete(new WritePolicy(), key(namespace, setName, key));
            return AerospikeResponse.success(key, existed);
        } catch (RuntimeException e) {
            throw AerospikeExceptionMapper.map("deleteRecord", e);
        }
    }

    public Map<String, Object> exists(String namespace, String setName, Object key) {
        try {
            boolean exists = client().exists(new Policy(), key(namespace, setName, key));
            return AerospikeResponse.success(key, exists);
        } catch (RuntimeException e) {
            throw AerospikeExceptionMapper.map("exists", e);
        }
    }

    public List<Map<String, Object>> batchGet(String namespace, String setName, List<?> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Key[] aerospikeKeys = new Key[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                aerospikeKeys[i] = key(namespace, setName, keys.get(i));
            }

            Record[] records = client().get(new BatchPolicy(), aerospikeKeys);
            List<Map<String, Object>> response = new ArrayList<Map<String, Object>>(keys.size());
            for (int i = 0; i < records.length; i++) {
                response.add(AerospikeResponse.record(keys.get(i), records[i]));
            }
            return response;
        } catch (RuntimeException e) {
            throw AerospikeExceptionMapper.map("batchGet", e);
        }
    }

    private AerospikeClient client() {
        return AerospikeClientProvider.getClient(config);
    }

    private static Key key(String namespace, String setName, Object key) {
        validateRequired("namespace", namespace);
        validateRequired("setName", setName);
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return new Key(namespace, setName, key.toString());
    }

    private static Bin[] toBins(Map<String, Object> bins) {
        if (bins == null || bins.isEmpty()) {
            throw new IllegalArgumentException("bins must not be empty");
        }

        List<Bin> result = new ArrayList<Bin>(bins.size());
        for (Map.Entry<String, Object> entry : bins.entrySet()) {
            validateRequired("bin name", entry.getKey());
            result.add(entry.getValue() == null ? Bin.asNull(entry.getKey()) : new Bin(entry.getKey(), Value.get(entry.getValue())));
        }
        return result.toArray(new Bin[0]);
    }

    private static void validateRequired(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
