package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.exception.AerospikeErrorType;
import com.idfcfirstbank.aerospike.exception.AerospikeExceptionMapper;
import com.idfcfirstbank.aerospike.model.AerospikeResponse;
import com.idfcfirstbank.aerospike.util.AerospikeValidation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            Key recordKey = key(namespace, setName, key);
            Record record = client().get(readPolicy(), recordKey);
            return AerospikeResponse.record(namespace, setName, key, record);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("getRecord", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    public Map<String, Object> getRecord(String namespace, String setName, String key, List<String> binNames) {
        try {
            Key recordKey = key(namespace, setName, key);
            String[] selectedBins = selectedBins(binNames);
            Record record = selectedBins.length == 0
                    ? client().get(readPolicy(), recordKey)
                    : client().get(readPolicy(), recordKey, selectedBins);
            return AerospikeResponse.record(namespace, setName, key, record);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("getRecordFields", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    public Map<String, Object> putRecord(String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        try {
            AerospikeValidation.requireNonNegative(ttlSeconds, "ttlSeconds");
            Key recordKey = key(namespace, setName, key);
            Bin[] recordBins = toBins(bins);
            WritePolicy writePolicy = writePolicy();
            writePolicy.expiration = ttlSeconds;
            client().put(writePolicy, recordKey, recordBins);
            return AerospikeResponse.success(namespace, setName, key, "put-record", true);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("putRecord", AerospikeErrorType.WRITE_FAILURE, exception);
        }
    }

    public Map<String, Object> deleteRecord(String namespace, String setName, String key) {
        try {
            Key recordKey = key(namespace, setName, key);
            boolean deleted = client().delete(writePolicy(), recordKey);
            return AerospikeResponse.success(namespace, setName, key, "delete-record", deleted);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("deleteRecord", AerospikeErrorType.DELETE_FAILURE, exception);
        }
    }

    public Map<String, Object> exists(String namespace, String setName, String key) {
        try {
            Key recordKey = key(namespace, setName, key);
            boolean exists = client().exists(readPolicy(), recordKey);
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

    public List<Map<String, Object>> batchGet(String namespace, String setName, List<String> keys, List<String> binNames) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Key[] aerospikeKeys = new Key[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                aerospikeKeys[i] = key(namespace, setName, keys.get(i));
            }

            String[] selectedBins = selectedBins(binNames);
            Record[] records = selectedBins.length == 0
                    ? client().get(batchPolicy(), aerospikeKeys)
                    : client().get(batchPolicy(), aerospikeKeys, selectedBins);

            List<Map<String, Object>> responses = new ArrayList<Map<String, Object>>(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                responses.add(AerospikeResponse.record(namespace, setName, keys.get(i), records[i]));
            }
            return responses;
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("batchGetFields", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    public List<Map<String, Object>> queryByFieldEquals(
            String namespace,
            String setName,
            String fieldName,
            Object fieldValue,
            List<String> binNames) {
        RecordSet recordSet = null;
        try {
            Statement statement = statement(namespace, setName, binNames);
            statement.setFilter(equalFilter(fieldName, fieldValue));
            recordSet = client().query(queryPolicy(), statement);
            return recordSetToResponses(namespace, setName, recordSet);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("queryByFieldEquals", AerospikeErrorType.UNKNOWN, exception);
        } finally {
            close(recordSet);
        }
    }

    public List<Map<String, Object>> queryByFieldRange(
            String namespace,
            String setName,
            String fieldName,
            Number begin,
            Number end,
            List<String> binNames) {
        RecordSet recordSet = null;
        try {
            long rangeBegin = toLongFilterValue(begin, "range begin");
            long rangeEnd = toLongFilterValue(end, "range end");
            if (rangeBegin > rangeEnd) {
                throw new IllegalArgumentException("range begin must be less than or equal to range end");
            }
            Statement statement = statement(namespace, setName, binNames);
            statement.setFilter(Filter.range(validatedFieldName(fieldName), rangeBegin, rangeEnd));
            recordSet = client().query(queryPolicy(), statement);
            return recordSetToResponses(namespace, setName, recordSet);
        } catch (RuntimeException exception) {
            throw AerospikeExceptionMapper.map("queryByFieldRange", AerospikeErrorType.UNKNOWN, exception);
        } finally {
            close(recordSet);
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

    private QueryPolicy queryPolicy() {
        QueryPolicy policy = new QueryPolicy();
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

    private static Statement statement(String namespace, String setName, List<String> binNames) {
        AerospikeValidation.requireNotBlank(namespace, "namespace");
        AerospikeValidation.requireNotBlank(setName, "setName");

        Statement statement = new Statement();
        statement.setNamespace(namespace);
        statement.setSetName(setName);

        String[] selectedBins = selectedBins(binNames);
        if (selectedBins.length > 0) {
            statement.setBinNames(selectedBins);
        }
        return statement;
    }

    private static String[] selectedBins(List<String> binNames) {
        if (binNames == null || binNames.isEmpty()) {
            return new String[0];
        }

        Set<String> normalizedBins = new LinkedHashSet<String>();
        for (String binName : binNames) {
            normalizedBins.add(validatedFieldName(binName));
        }
        return normalizedBins.toArray(new String[normalizedBins.size()]);
    }

    private static String validatedFieldName(String fieldName) {
        AerospikeValidation.requireNotBlank(fieldName, "fieldName");
        return fieldName.trim();
    }

    private static Filter equalFilter(String fieldName, Object fieldValue) {
        String validatedFieldName = validatedFieldName(fieldName);
        if (fieldValue == null) {
            throw new IllegalArgumentException("fieldValue must not be null");
        }
        if (fieldValue instanceof String) {
            String value = ((String) fieldValue).trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("fieldValue must not be blank");
            }
            return Filter.equal(validatedFieldName, value);
        }
        if (fieldValue instanceof Number) {
            return Filter.equal(validatedFieldName, toLongFilterValue((Number) fieldValue, "fieldValue"));
        }
        throw new IllegalArgumentException("fieldValue must be a non-blank String or integral Number");
    }

    private static long toLongFilterValue(Number value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        if (value instanceof BigDecimal) {
            BigDecimal decimal = ((BigDecimal) value).stripTrailingZeros();
            if (decimal.scale() > 0) {
                throw new IllegalArgumentException(fieldName + " must be an integer value");
            }
            return decimal.longValueExact();
        }

        if (value instanceof BigInteger) {
            return ((BigInteger) value).longValueExact();
        }

        double doubleValue = value.doubleValue();
        long longValue = value.longValue();
        if (doubleValue != (double) longValue) {
            throw new IllegalArgumentException(fieldName + " must be an integer value");
        }
        return longValue;
    }

    private static List<Map<String, Object>> recordSetToResponses(String namespace, String setName, RecordSet recordSet) {
        List<Map<String, Object>> responses = new ArrayList<Map<String, Object>>();
        while (recordSet.next()) {
            Key key = recordSet.getKey();
            responses.add(AerospikeResponse.record(namespace, setName, responseKey(key), recordSet.getRecord()));
        }
        return responses;
    }

    private static void close(RecordSet recordSet) {
        if (recordSet != null) {
            recordSet.close();
        }
    }

    private static String responseKey(Key key) {
        if (key == null) {
            return null;
        }
        if (key.userKey != null) {
            return String.valueOf(key.userKey.getObject());
        }
        if (key.digest == null || key.digest.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder(key.digest.length * 2);
        for (byte digestByte : key.digest) {
            builder.append(Character.forDigit((digestByte >> 4) & 0xF, 16));
            builder.append(Character.forDigit(digestByte & 0xF, 16));
        }
        return builder.toString().toUpperCase();
    }

    private static Bin[] toBins(Map<String, Object> bins) {
        if (bins == null || bins.isEmpty()) {
            throw new IllegalArgumentException("bins map is required and cannot be empty");
        }

        List<Bin> result = new ArrayList<Bin>(bins.size());
        for (Map.Entry<String, Object> entry : bins.entrySet()) {
            AerospikeValidation.requireNotBlank(entry.getKey(), "bin name");
            Object value = normalizeBinValue(entry.getValue());
            result.add(value == null ? Bin.asNull(entry.getKey()) : new Bin(entry.getKey(), Value.get(value)));
        }
        return result.toArray(new Bin[result.size()]);
    }

    private static Object normalizeBinValue(Object value) {
        if (value instanceof BigDecimal) {
            BigDecimal decimal = ((BigDecimal) value).stripTrailingZeros();
            if (decimal.scale() <= 0) {
                return decimal.longValueExact();
            }
            return decimal.doubleValue();
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).longValueExact();
        }
        if (value instanceof Map<?, ?>) {
            Map<Object, Object> normalized = new LinkedHashMap<Object, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                normalized.put(entry.getKey(), normalizeBinValue(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof List<?>) {
            List<Object> normalized = new ArrayList<Object>();
            for (Object item : (List<?>) value) {
                normalized.add(normalizeBinValue(item));
            }
            return normalized;
        }
        return value;
    }
}
