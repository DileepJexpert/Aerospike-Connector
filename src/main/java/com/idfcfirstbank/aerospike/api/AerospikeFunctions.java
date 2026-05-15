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
