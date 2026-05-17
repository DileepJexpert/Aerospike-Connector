package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.Value;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.exception.AerospikeErrorType;
import com.idfcfirstbank.aerospike.exception.AerospikeExceptionMapper;
import com.idfcfirstbank.aerospike.exception.AerospikeOperationException;
import com.idfcfirstbank.aerospike.model.AerospikeResponse;
import com.idfcfirstbank.aerospike.util.AerospikeValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AerospikeRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(AerospikeRecordService.class);

    private final AerospikeConfig config;

    public AerospikeRecordService(AerospikeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    private static AerospikeOperationException fail(String operation, AerospikeErrorType defaultType, RuntimeException exception) {
        AerospikeOperationException mapped = AerospikeExceptionMapper.map(operation, defaultType, exception);
        if (mapped.getErrorType() == AerospikeErrorType.VALIDATION_FAILED) {
            LOG.debug("Aerospike {} rejected by validation: {}", operation, mapped.getMessage());
        } else {
            LOG.warn("Aerospike {} failed [{}]: {}", operation, mapped.getErrorType(), mapped.getMessage());
        }
        return mapped;
    }

    public Map<String, Object> getRecord(String namespace, String setName, String key) {
        try {
            Key recordKey = key(namespace, setName, key);
            Record record = client().get(readPolicy(), recordKey);
            return AerospikeResponse.record(namespace, setName, key, record);
        } catch (RuntimeException exception) {
            throw fail("getRecord", AerospikeErrorType.UNKNOWN, exception);
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
            throw fail("getRecordFields", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    public Map<String, Object> putRecord(String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return write("putRecord", "put-record", namespace, setName, key, bins, ttlSeconds,
                RecordExistsAction.UPDATE, -1);
    }

    /**
     * Inserts a record only if it does not already exist. Fails with
     * {@link AerospikeErrorType#RECORD_ALREADY_EXISTS} if the key is present.
     */
    public Map<String, Object> createRecord(String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return write("createRecord", "create-record", namespace, setName, key, bins, ttlSeconds,
                RecordExistsAction.CREATE_ONLY, -1);
    }

    /**
     * Replaces a record entirely. Bins not present in {@code bins} are removed.
     */
    public Map<String, Object> replaceRecord(String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return write("replaceRecord", "replace-record", namespace, setName, key, bins, ttlSeconds,
                RecordExistsAction.REPLACE, -1);
    }

    /**
     * Updates a record only if it already exists. Fails with
     * {@link AerospikeErrorType#KEY_NOT_FOUND} if the key is absent.
     */
    public Map<String, Object> updateRecord(String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return write("updateRecord", "update-record", namespace, setName, key, bins, ttlSeconds,
                RecordExistsAction.UPDATE_ONLY, -1);
    }

    /**
     * Optimistic-locking write: the record is only updated if its current
     * generation equals {@code expectedGeneration}. Fails with
     * {@link AerospikeErrorType#GENERATION_MISMATCH} if it was modified
     * concurrently.
     */
    public Map<String, Object> putRecordIfGeneration(
            String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds, int expectedGeneration) {
        if (expectedGeneration < 0) {
            return failFast("putRecordIfGeneration", "expectedGeneration must not be negative");
        }
        return write("putRecordIfGeneration", "put-record", namespace, setName, key, bins, ttlSeconds,
                RecordExistsAction.UPDATE, expectedGeneration);
    }

    private Map<String, Object> write(
            String operation,
            String responseOperation,
            String namespace,
            String setName,
            String key,
            Map<String, Object> bins,
            int ttlSeconds,
            RecordExistsAction action,
            int expectedGeneration) {
        try {
            requireValidTtl(ttlSeconds);
            Key recordKey = key(namespace, setName, key);
            Bin[] recordBins = toBins(bins);
            WritePolicy writePolicy = writePolicy();
            writePolicy.expiration = ttlSeconds;
            writePolicy.recordExistsAction = action;
            if (expectedGeneration >= 0) {
                writePolicy.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;
                writePolicy.generation = expectedGeneration;
            }
            client().put(writePolicy, recordKey, recordBins);
            return AerospikeResponse.success(namespace, setName, key, responseOperation, true);
        } catch (RuntimeException exception) {
            throw fail(operation, AerospikeErrorType.WRITE_FAILURE, exception);
        }
    }

    /**
     * Atomically adds the supplied numeric deltas to integer/float bins and
     * returns the updated record. Missing bins are created starting from the
     * delta. Use this instead of read-modify-write for counters.
     */
    public Map<String, Object> incrementBins(
            String namespace, String setName, String key, Map<String, Object> deltas, int ttlSeconds) {
        try {
            requireValidTtl(ttlSeconds);
            Key recordKey = key(namespace, setName, key);
            if (deltas == null || deltas.isEmpty()) {
                throw new IllegalArgumentException("deltas map is required and cannot be empty");
            }

            List<Operation> operations = new ArrayList<Operation>(deltas.size() + 1);
            for (Map.Entry<String, Object> entry : deltas.entrySet()) {
                AerospikeValidation.requireNotBlank(entry.getKey(), "bin name");
                Object value = entry.getValue();
                if (!(value instanceof Number)) {
                    throw new IllegalArgumentException("increment delta for '" + entry.getKey() + "' must be numeric");
                }
                operations.add(Operation.add(new Bin(entry.getKey(), Value.get(normalizeBinValue(value)))));
            }
            operations.add(Operation.get());

            WritePolicy writePolicy = writePolicy();
            writePolicy.expiration = ttlSeconds;
            Record record = client().operate(writePolicy, recordKey, operations.toArray(new Operation[0]));
            return AerospikeResponse.record(namespace, setName, key, record);
        } catch (RuntimeException exception) {
            throw fail("incrementBins", AerospikeErrorType.WRITE_FAILURE, exception);
        }
    }

    /**
     * Resets a record's time-to-live without rewriting its bins. Fails with
     * {@link AerospikeErrorType#KEY_NOT_FOUND} if the record does not exist.
     */
    public Map<String, Object> touchRecord(String namespace, String setName, String key, int ttlSeconds) {
        try {
            requireValidTtl(ttlSeconds);
            Key recordKey = key(namespace, setName, key);
            WritePolicy writePolicy = writePolicy();
            writePolicy.expiration = ttlSeconds;
            client().touch(writePolicy, recordKey);
            return AerospikeResponse.success(namespace, setName, key, "touch-record", true);
        } catch (RuntimeException exception) {
            throw fail("touchRecord", AerospikeErrorType.WRITE_FAILURE, exception);
        }
    }

    private Map<String, Object> failFast(String operation, String message) {
        throw fail(operation, AerospikeErrorType.VALIDATION_FAILED, new IllegalArgumentException(message));
    }

    /**
     * Checks connectivity to the Aerospike cluster. Returns a map with:
     * <ul>
     *   <li>{@code connected} – true when the client has at least one active node</li>
     *   <li>{@code nodes} – list of node names reported by the cluster</li>
     * </ul>
     * This does NOT issue a real network request beyond what the client already
     * has open; use it for health-check endpoints or Mule startup probes.
     */
    public Map<String, Object> ping() {
        try {
            AerospikeClient aerospikeClient = client();
            boolean connected = aerospikeClient.isConnected();
            List<String> nodeNames = aerospikeClient.getNodeNames();
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("connected", connected);
            result.put("nodes", nodeNames != null ? nodeNames : Collections.<String>emptyList());
            return result;
        } catch (RuntimeException exception) {
            throw fail("ping", AerospikeErrorType.CONNECTION_FAILED, exception);
        }
    }

    public Map<String, Object> deleteRecord(String namespace, String setName, String key) {
        try {
            Key recordKey = key(namespace, setName, key);
            boolean deleted = client().delete(writePolicy(), recordKey);
            return AerospikeResponse.success(namespace, setName, key, "delete-record", deleted);
        } catch (RuntimeException exception) {
            throw fail("deleteRecord", AerospikeErrorType.DELETE_FAILURE, exception);
        }
    }

    public Map<String, Object> exists(String namespace, String setName, String key) {
        try {
            Key recordKey = key(namespace, setName, key);
            boolean exists = client().exists(readPolicy(), recordKey);
            return AerospikeResponse.exists(namespace, setName, key, exists);
        } catch (RuntimeException exception) {
            throw fail("exists", AerospikeErrorType.UNKNOWN, exception);
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
            throw fail("batchGet", AerospikeErrorType.UNKNOWN, exception);
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
            throw fail("batchGetFields", AerospikeErrorType.UNKNOWN, exception);
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
            throw fail("queryByFieldEquals", AerospikeErrorType.UNKNOWN, exception);
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
            throw fail("queryByFieldRange", AerospikeErrorType.UNKNOWN, exception);
        } finally {
            close(recordSet);
        }
    }

    public List<Map<String, Object>> findAll(String namespace, String setName, List<String> binNames) {
        try {
            AerospikeValidation.requireNotBlank(namespace, "namespace");
            AerospikeValidation.requireNotBlank(setName, "setName");
            String[] selectedBins = selectedBins(binNames);
            final String ns = namespace.trim();
            final String set = setName.trim();
            final ConcurrentLinkedQueue<Map<String, Object>> collected =
                    new ConcurrentLinkedQueue<Map<String, Object>>();
            ScanCallback callback = (key, record) ->
                    collected.add(AerospikeResponse.record(ns, set, responseKey(key), record));

            if (selectedBins.length == 0) {
                client().scanAll(scanPolicy(), ns, set, callback);
            } else {
                client().scanAll(scanPolicy(), ns, set, callback, selectedBins);
            }
            return new ArrayList<Map<String, Object>>(collected);
        } catch (RuntimeException exception) {
            throw fail("findAll", AerospikeErrorType.UNKNOWN, exception);
        }
    }

    public List<Map<String, Object>> query(
            String namespace,
            String setName,
            Map<String, Object> criteria,
            List<String> binNames) {
        RecordSet recordSet = null;
        try {
            Expression filterExp = AerospikeQuerySupport.toExpression(criteria);
            Filter indexFilter = AerospikeQuerySupport.indexFilter(criteria);
            Statement statement = statement(namespace, setName, binNames);
            if (indexFilter != null) {
                statement.setFilter(indexFilter);
            }
            QueryPolicy policy = queryPolicy();
            policy.filterExp = filterExp;
            recordSet = client().query(policy, statement);
            return recordSetToResponses(namespace.trim(), setName.trim(), recordSet);
        } catch (RuntimeException exception) {
            throw fail("query", AerospikeErrorType.UNKNOWN, exception);
        } finally {
            close(recordSet);
        }
    }

    private AerospikeClient client() {
        return AerospikeClientProvider.getClient(config);
    }

    private Policy readPolicy() {
        Policy policy = new Policy();
        applyCommon(policy, config.getReadTimeout());
        return policy;
    }

    private WritePolicy writePolicy() {
        WritePolicy policy = new WritePolicy();
        applyCommon(policy, config.getWriteTimeout());
        return policy;
    }

    private BatchPolicy batchPolicy() {
        BatchPolicy policy = new BatchPolicy();
        applyCommon(policy, config.getReadTimeout());
        return policy;
    }

    private QueryPolicy queryPolicy() {
        QueryPolicy policy = new QueryPolicy();
        applyCommon(policy, config.getReadTimeout());
        return policy;
    }

    private ScanPolicy scanPolicy() {
        ScanPolicy policy = new ScanPolicy();
        applyCommon(policy, config.getReadTimeout());
        return policy;
    }

    private void applyCommon(Policy policy, int operationTimeout) {
        policy.sendKey = config.isSendKey();
        if (config.getConnectTimeout() > 0) {
            policy.connectTimeout = config.getConnectTimeout();
        }
        if (operationTimeout > 0) {
            policy.socketTimeout = operationTimeout;
            policy.totalTimeout = operationTimeout;
        }
    }

    private static void requireValidTtl(int ttlSeconds) {
        // Aerospike WritePolicy.expiration sentinels: -2 keep current TTL,
        // -1 never expire, 0 namespace default-ttl, >0 seconds.
        if (ttlSeconds < -2) {
            throw new IllegalArgumentException(
                    "ttlSeconds must be >= -2 (-2 keep, -1 never expire, 0 namespace default, >0 seconds)");
        }
    }

    private static Key key(String namespace, String setName, String key) {
        AerospikeValidation.requireNotBlank(namespace, "namespace");
        AerospikeValidation.requireNotBlank(setName, "setName");
        AerospikeValidation.requireNotBlank(key, "key");
        return new Key(namespace.trim(), setName.trim(), key.trim());
    }

    private static Statement statement(String namespace, String setName, List<String> binNames) {
        AerospikeValidation.requireNotBlank(namespace, "namespace");
        AerospikeValidation.requireNotBlank(setName, "setName");

        Statement statement = new Statement();
        statement.setNamespace(namespace.trim());
        statement.setSetName(setName.trim());

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
            try {
                return decimal.longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(fieldName + " exceeds the supported 64-bit integer range", exception);
            }
        }

        if (value instanceof BigInteger) {
            try {
                return ((BigInteger) value).longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(fieldName + " exceeds the supported 64-bit integer range", exception);
            }
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
                try {
                    return decimal.longValueExact();
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException(
                            "bin value exceeds the supported 64-bit integer range", exception);
                }
            }
            return decimal.doubleValue();
        }
        if (value instanceof BigInteger) {
            try {
                return ((BigInteger) value).longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "bin value exceeds the supported 64-bit integer range", exception);
            }
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
