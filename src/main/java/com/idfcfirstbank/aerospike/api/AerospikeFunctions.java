package com.idfcfirstbank.aerospike.api;

import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.service.AerospikeClientProvider;
import com.idfcfirstbank.aerospike.service.AerospikeRecordService;

import java.util.List;
import java.util.Map;

public final class AerospikeFunctions {

    private AerospikeFunctions() {
    }

    public static Map<String, Object> getRecord(String hosts, String namespace, String setName, String key) {
        return service(hosts).getRecord(namespace, setName, key);
    }

    public static Map<String, Object> getRecordFields(
            String hosts,
            String namespace,
            String setName,
            String key,
            List<String> fieldNames) {
        return service(hosts).getRecord(namespace, setName, key, fieldNames);
    }

    public static Map<String, Object> putRecord(
            String hosts,
            String namespace,
            String setName,
            String key,
            Map<String, Object> bins,
            int ttlSeconds) {
        return service(hosts).putRecord(namespace, setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> putRecord(
            String hosts,
            String namespace,
            String setName,
            String key,
            Map<String, Object> bins,
            Number ttlSeconds) {
        return putRecord(hosts, namespace, setName, key, bins, toTtlSeconds(ttlSeconds));
    }

    public static Map<String, Object> deleteRecord(String hosts, String namespace, String setName, String key) {
        return service(hosts).deleteRecord(namespace, setName, key);
    }

    public static Map<String, Object> exists(String hosts, String namespace, String setName, String key) {
        return service(hosts).exists(namespace, setName, key);
    }

    public static List<Map<String, Object>> batchGet(String hosts, String namespace, String setName, List<String> keys) {
        return service(hosts).batchGet(namespace, setName, keys);
    }

    public static List<Map<String, Object>> batchGetFields(
            String hosts,
            String namespace,
            String setName,
            List<String> keys,
            List<String> fieldNames) {
        return service(hosts).batchGet(namespace, setName, keys, fieldNames);
    }

    public static List<Map<String, Object>> queryRecordsByFieldEquals(
            String hosts,
            String namespace,
            String setName,
            String fieldName,
            Object fieldValue,
            List<String> fieldNames) {
        return service(hosts).queryByFieldEquals(namespace, setName, fieldName, fieldValue, fieldNames);
    }

    public static List<Map<String, Object>> queryRecordsByFieldRange(
            String hosts,
            String namespace,
            String setName,
            String fieldName,
            Number rangeBegin,
            Number rangeEnd,
            List<String> fieldNames) {
        return service(hosts).queryByFieldRange(namespace, setName, fieldName, rangeBegin, rangeEnd, fieldNames);
    }

    public static Map<String, Object> getRecordWithConfig(Map<String, Object> config, String setName, String key) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).getRecord(aerospikeConfig.getNamespace(), setName, key);
    }

    public static Map<String, Object> getRecordFieldsWithConfig(
            Map<String, Object> config,
            String setName,
            String key,
            List<String> fieldNames) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).getRecord(aerospikeConfig.getNamespace(), setName, key, fieldNames);
    }

    public static Map<String, Object> putRecordWithConfig(
            Map<String, Object> config,
            String setName,
            String key,
            Map<String, Object> bins,
            int ttlSeconds) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).putRecord(aerospikeConfig.getNamespace(), setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> putRecordWithConfig(
            Map<String, Object> config,
            String setName,
            String key,
            Map<String, Object> bins,
            Number ttlSeconds) {
        return putRecordWithConfig(config, setName, key, bins, toTtlSeconds(ttlSeconds));
    }

    /**
     * Writes a record using the namespace default TTL. Note that an
     * {@code expiration} of {@code 0} means "use the namespace
     * {@code default-ttl}" in Aerospike, not "never expire". Pass {@code -1}
     * explicitly via an overload that accepts a TTL if the record must never
     * expire.
     */
    public static Map<String, Object> putRecordWithConfig(
            Map<String, Object> config,
            String setName,
            String key,
            Map<String, Object> bins) {
        return putRecordWithConfig(config, setName, key, bins, 0);
    }

    public static Map<String, Object> deleteRecordWithConfig(Map<String, Object> config, String setName, String key) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).deleteRecord(aerospikeConfig.getNamespace(), setName, key);
    }

    public static Map<String, Object> existsWithConfig(Map<String, Object> config, String setName, String key) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).exists(aerospikeConfig.getNamespace(), setName, key);
    }

    public static List<Map<String, Object>> batchGetWithConfig(Map<String, Object> config, String setName, List<String> keys) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).batchGet(aerospikeConfig.getNamespace(), setName, keys);
    }

    public static List<Map<String, Object>> batchGetFieldsWithConfig(
            Map<String, Object> config,
            String setName,
            List<String> keys,
            List<String> fieldNames) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).batchGet(aerospikeConfig.getNamespace(), setName, keys, fieldNames);
    }

    public static List<Map<String, Object>> queryRecordsByFieldEqualsWithConfig(
            Map<String, Object> config,
            String setName,
            String fieldName,
            Object fieldValue,
            List<String> fieldNames) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).queryByFieldEquals(aerospikeConfig.getNamespace(), setName, fieldName, fieldValue, fieldNames);
    }

    public static List<Map<String, Object>> queryRecordsByFieldRangeWithConfig(
            Map<String, Object> config,
            String setName,
            String fieldName,
            Number rangeBegin,
            Number rangeEnd,
            List<String> fieldNames) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).queryByFieldRange(aerospikeConfig.getNamespace(), setName, fieldName, rangeBegin, rangeEnd, fieldNames);
    }

    public static Map<String, Object> getRecordWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String key) {
        return service(hosts, user, password).getRecord(namespace, setName, key);
    }

    public static Map<String, Object> putRecordWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String key,
            Map<String, Object> bins,
            int ttlSeconds) {
        return service(hosts, user, password).putRecord(namespace, setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> putRecordWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String key,
            Map<String, Object> bins,
            Number ttlSeconds) {
        return putRecordWithAuth(hosts, user, password, namespace, setName, key, bins, toTtlSeconds(ttlSeconds));
    }

    public static Map<String, Object> deleteRecordWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String key) {
        return service(hosts, user, password).deleteRecord(namespace, setName, key);
    }

    public static Map<String, Object> existsWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String key) {
        return service(hosts, user, password).exists(namespace, setName, key);
    }

    public static List<Map<String, Object>> batchGetWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            List<String> keys) {
        return service(hosts, user, password).batchGet(namespace, setName, keys);
    }

    public static Map<String, Object> getRecordFieldsWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String key,
            List<String> fieldNames) {
        return service(hosts, user, password).getRecord(namespace, setName, key, fieldNames);
    }

    public static List<Map<String, Object>> batchGetFieldsWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            List<String> keys,
            List<String> fieldNames) {
        return service(hosts, user, password).batchGet(namespace, setName, keys, fieldNames);
    }

    public static List<Map<String, Object>> queryRecordsByFieldEqualsWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String fieldName,
            Object fieldValue,
            List<String> fieldNames) {
        return service(hosts, user, password).queryByFieldEquals(namespace, setName, fieldName, fieldValue, fieldNames);
    }

    public static List<Map<String, Object>> queryRecordsByFieldRangeWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            String fieldName,
            Number rangeBegin,
            Number rangeEnd,
            List<String> fieldNames) {
        return service(hosts, user, password).queryByFieldRange(namespace, setName, fieldName, rangeBegin, rangeEnd, fieldNames);
    }

    public static List<Map<String, Object>> findAll(String hosts, String namespace, String setName) {
        return service(hosts).findAll(namespace, setName, null);
    }

    public static List<Map<String, Object>> findAllFields(
            String hosts,
            String namespace,
            String setName,
            List<String> fieldNames) {
        return service(hosts).findAll(namespace, setName, fieldNames);
    }

    public static List<Map<String, Object>> findAllWithConfig(Map<String, Object> config, String setName) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).findAll(aerospikeConfig.getNamespace(), setName, null);
    }

    public static List<Map<String, Object>> findAllFieldsWithConfig(
            Map<String, Object> config,
            String setName,
            List<String> fieldNames) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).findAll(aerospikeConfig.getNamespace(), setName, fieldNames);
    }

    public static List<Map<String, Object>> findAllWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName) {
        return service(hosts, user, password).findAll(namespace, setName, null);
    }

    public static List<Map<String, Object>> findAllFieldsWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            List<String> fieldNames) {
        return service(hosts, user, password).findAll(namespace, setName, fieldNames);
    }

    public static List<Map<String, Object>> query(
            String hosts,
            String namespace,
            String setName,
            Map<String, Object> criteria,
            List<String> fieldNames) {
        return service(hosts).query(namespace, setName, criteria, fieldNames);
    }

    public static List<Map<String, Object>> queryWithConfig(
            Map<String, Object> config,
            String setName,
            Map<String, Object> criteria,
            List<String> fieldNames) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).query(aerospikeConfig.getNamespace(), setName, criteria, fieldNames);
    }

    public static List<Map<String, Object>> queryWithAuth(
            String hosts,
            String user,
            String password,
            String namespace,
            String setName,
            Map<String, Object> criteria,
            List<String> fieldNames) {
        return service(hosts, user, password).query(namespace, setName, criteria, fieldNames);
    }

    // -------------------------------------------------------------------------
    // createRecord
    // -------------------------------------------------------------------------

    public static Map<String, Object> createRecord(
            String hosts, String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return service(hosts).createRecord(namespace, setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> createRecordWithConfig(
            Map<String, Object> config, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).createRecord(aerospikeConfig.getNamespace(), setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> createRecordWithConfig(
            Map<String, Object> config, String setName, String key, Map<String, Object> bins) {
        return createRecordWithConfig(config, setName, key, bins, 0);
    }

    public static Map<String, Object> createRecordWithAuth(
            String hosts, String user, String password,
            String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return service(hosts, user, password).createRecord(namespace, setName, key, bins, ttlSeconds);
    }

    // -------------------------------------------------------------------------
    // replaceRecord
    // -------------------------------------------------------------------------

    public static Map<String, Object> replaceRecord(
            String hosts, String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return service(hosts).replaceRecord(namespace, setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> replaceRecordWithConfig(
            Map<String, Object> config, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).replaceRecord(aerospikeConfig.getNamespace(), setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> replaceRecordWithConfig(
            Map<String, Object> config, String setName, String key, Map<String, Object> bins) {
        return replaceRecordWithConfig(config, setName, key, bins, 0);
    }

    public static Map<String, Object> replaceRecordWithAuth(
            String hosts, String user, String password,
            String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return service(hosts, user, password).replaceRecord(namespace, setName, key, bins, ttlSeconds);
    }

    // -------------------------------------------------------------------------
    // updateRecord
    // -------------------------------------------------------------------------

    public static Map<String, Object> updateRecord(
            String hosts, String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return service(hosts).updateRecord(namespace, setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> updateRecordWithConfig(
            Map<String, Object> config, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).updateRecord(aerospikeConfig.getNamespace(), setName, key, bins, ttlSeconds);
    }

    public static Map<String, Object> updateRecordWithConfig(
            Map<String, Object> config, String setName, String key, Map<String, Object> bins) {
        return updateRecordWithConfig(config, setName, key, bins, 0);
    }

    public static Map<String, Object> updateRecordWithAuth(
            String hosts, String user, String password,
            String namespace, String setName, String key, Map<String, Object> bins, int ttlSeconds) {
        return service(hosts, user, password).updateRecord(namespace, setName, key, bins, ttlSeconds);
    }

    // -------------------------------------------------------------------------
    // putRecordIfGeneration (optimistic locking / CAS)
    // -------------------------------------------------------------------------

    public static Map<String, Object> putRecordIfGeneration(
            String hosts, String namespace, String setName, String key,
            Map<String, Object> bins, int ttlSeconds, int expectedGeneration) {
        return service(hosts).putRecordIfGeneration(namespace, setName, key, bins, ttlSeconds, expectedGeneration);
    }

    public static Map<String, Object> putRecordIfGenerationWithConfig(
            Map<String, Object> config, String setName, String key,
            Map<String, Object> bins, int ttlSeconds, int expectedGeneration) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).putRecordIfGeneration(
                aerospikeConfig.getNamespace(), setName, key, bins, ttlSeconds, expectedGeneration);
    }

    public static Map<String, Object> putRecordIfGenerationWithAuth(
            String hosts, String user, String password,
            String namespace, String setName, String key,
            Map<String, Object> bins, int ttlSeconds, int expectedGeneration) {
        return service(hosts, user, password).putRecordIfGeneration(
                namespace, setName, key, bins, ttlSeconds, expectedGeneration);
    }

    // -------------------------------------------------------------------------
    // incrementBins (atomic counter increment)
    // -------------------------------------------------------------------------

    public static Map<String, Object> incrementBins(
            String hosts, String namespace, String setName, String key,
            Map<String, Object> deltas, int ttlSeconds) {
        return service(hosts).incrementBins(namespace, setName, key, deltas, ttlSeconds);
    }

    public static Map<String, Object> incrementBinsWithConfig(
            Map<String, Object> config, String setName, String key,
            Map<String, Object> deltas, int ttlSeconds) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).incrementBins(
                aerospikeConfig.getNamespace(), setName, key, deltas, ttlSeconds);
    }

    public static Map<String, Object> incrementBinsWithConfig(
            Map<String, Object> config, String setName, String key, Map<String, Object> deltas) {
        return incrementBinsWithConfig(config, setName, key, deltas, 0);
    }

    public static Map<String, Object> incrementBinsWithAuth(
            String hosts, String user, String password,
            String namespace, String setName, String key,
            Map<String, Object> deltas, int ttlSeconds) {
        return service(hosts, user, password).incrementBins(namespace, setName, key, deltas, ttlSeconds);
    }

    // -------------------------------------------------------------------------
    // touchRecord (reset TTL without rewriting bins)
    // -------------------------------------------------------------------------

    public static Map<String, Object> touchRecord(
            String hosts, String namespace, String setName, String key, int ttlSeconds) {
        return service(hosts).touchRecord(namespace, setName, key, ttlSeconds);
    }

    public static Map<String, Object> touchRecordWithConfig(
            Map<String, Object> config, String setName, String key, int ttlSeconds) {
        AerospikeConfig aerospikeConfig = AerospikeConfig.fromMap(config);
        return service(aerospikeConfig).touchRecord(aerospikeConfig.getNamespace(), setName, key, ttlSeconds);
    }

    public static Map<String, Object> touchRecordWithAuth(
            String hosts, String user, String password,
            String namespace, String setName, String key, int ttlSeconds) {
        return service(hosts, user, password).touchRecord(namespace, setName, key, ttlSeconds);
    }

    // -------------------------------------------------------------------------
    // ping / health check
    // -------------------------------------------------------------------------

    /**
     * Returns cluster connectivity information: {@code connected} (boolean) and
     * {@code nodes} (list of node names). No new network connection is opened;
     * the method queries the already-cached client's node list.
     */
    public static Map<String, Object> ping(String hosts) {
        return service(hosts).ping();
    }

    public static Map<String, Object> pingWithConfig(Map<String, Object> config) {
        return service(AerospikeConfig.fromMap(config)).ping();
    }

    public static Map<String, Object> pingWithAuth(String hosts, String user, String password) {
        return service(hosts, user, password).ping();
    }

    public static void closeAllClients() {
        AerospikeClientProvider.closeAll();
    }

    private static AerospikeRecordService service(String hosts) {
        return new AerospikeRecordService(new AerospikeConfig(hosts));
    }

    private static AerospikeRecordService service(String hosts, String user, String password) {
        return new AerospikeRecordService(new AerospikeConfig(hosts, user, password));
    }

    private static AerospikeRecordService service(AerospikeConfig config) {
        return new AerospikeRecordService(config);
    }

    private static int toTtlSeconds(Number ttlSeconds) {
        return ttlSeconds == null ? 0 : ttlSeconds.intValue();
    }
}
