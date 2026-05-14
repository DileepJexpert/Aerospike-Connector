package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.exception.AerospikeErrorType;
import com.idfcfirstbank.aerospike.exception.AerospikeExceptionMapper;
import com.idfcfirstbank.aerospike.model.AerospikeResponse;
import com.idfcfirstbank.aerospike.util.AerospikeValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class AerospikeRecordService {

    private final AerospikeConfig config;

    public AerospikeRecordService(AerospikeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    public Map<String, Object> getRecord(String namespace, String setName, String key) {
        try {
            Record record = client().get(readPolicy(), key(namespace, setName, key));
            return AerospikeResponse.record(namespace, setName, key, record);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("getRecord", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    public Map<String, Object> putRecord(String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        try {
            AerospikeValidation.requireNonNegative(ttlSeconds, "ttlSeconds");
            WritePolicy writePolicy = writePolicy();
            writePolicy.expiration = ttlSeconds;
            client().put(writePolicy, key(namespace, setName, key), toBins(bins));
            return AerospikeResponse.success(namespace, setName, key, "put-record", true);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("putRecord", AerospikeErrorType.WRITE_FAILURE, exception);
        }
    }

    public Map<String, Object> deleteRecord(String namespace, String setName, String key) {
        try {
            boolean deleted = client().delete(writePolicy(), key(namespace, setName, key));
            return AerospikeResponse.success(namespace, setName, key, "delete-record", deleted);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("deleteRecord", AerospikeErrorType.DELETE_FAILURE, exception);
        }
    }

    public Map<String, Object> exists(String namespace, String setName, String key) {
        try {
            boolean exists = client().exists(readPolicy(), key(namespace, setName, key));
            return AerospikeResponse.exists(namespace, setName, key, exists);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("exists", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    public List<Map<String, Object>> batchGet(String namespace, String setName, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Key[] aerospikeKeys = new Key[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                aerospikeKeys[i] = key(namespace, setName, keys.get(i));
            }

            Record[] records = client().get(batchPolicy(), aerospikeKeys);
            List<Map<String, Object>> responses = new ArrayList<Map<String, Object>>(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                responses.add(AerospikeResponse.record(namespace, setName, keys.get(i), records[i]));
            }
            return responses;
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("batchGet", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    private AerospikeClient client() {
        return AerospikeClientProvider.getClient(config);
    }

    private Policy readPolicy() {
        Policy policy = new Policy();
        applyTimeouts(policy, config.getReadTimeout());
        return policy;
    }

    private WritePolicy writePolicy() {
        WritePolicy policy = new WritePolicy();
        applyTimeouts(policy, config.getWriteTimeout());
        return policy;
    }

    private BatchPolicy batchPolicy() {
        BatchPolicy policy = new BatchPolicy();
        applyTimeouts(policy, config.getReadTimeout());
        return policy;
    }

    private void applyTimeouts(Policy policy, int operationTimeout) {
        if (config.getConnectTimeout() > 0) {
            policy.connectTimeout = config.getConnectTimeout();
        }
        if (operationTimeout > 0) {
            policy.socketTimeout = operationTimeout;
            policy.totalTimeout = operationTimeout;
        }
    }

    private static Key key(String namespace, String setName, String key) {
        AerospikeValidation.requireNotBlank(namespace, "namespace");
        AerospikeValidation.requireNotBlank(setName, "setName");
        AerospikeValidation.requireNotBlank(key, "key");
        return new Key(namespace, setName, key);
    }

    private static Bin[] toBins(Map<String, Object> bins) {
        if (bins == null || bins.isEmpty()) {
            throw new IllegalArgumentException("bins map is required and cannot be empty");
        }

        List<Bin> result = new ArrayList<Bin>(bins.size());
        for (Map.Entry<String, Object> entry : bins.entrySet()) {
            AerospikeValidation.requireNotBlank(entry.getKey(), "bin name");
            result.add(entry.getValue() == null ? Bin.asNull(entry.getKey()) : new Bin(entry.getKey(), Value.get(entry.getValue())));
        }
        return result.toArray(new Bin[result.size()]);
    }
}
