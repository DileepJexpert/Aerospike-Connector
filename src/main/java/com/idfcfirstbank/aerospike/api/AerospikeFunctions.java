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

    public static Map<String, Object> putRecord(
            String hosts,
            String namespace,
            String setName,
            String key,
            Map<String, Object> bins,
            int ttlSeconds) {
        return service(hosts).putRecord(namespace, setName, key, bins, ttlSeconds);
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

    public static void closeAllClients() {
        AerospikeClientProvider.closeAll();
    }

    private static AerospikeRecordService service(String hosts) {
        return new AerospikeRecordService(new AerospikeConfig(hosts));
    }

    private static AerospikeRecordService service(String hosts, String user, String password) {
        return new AerospikeRecordService(new AerospikeConfig(hosts, user, password));
    }
}
