package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;
import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.util.AerospikeValidation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AerospikeClientProvider {

    private static final int DEFAULT_PORT = 3000;
    private static final ConcurrentMap<String, AerospikeClient> CLIENTS = new ConcurrentHashMap<String, AerospikeClient>();

    private AerospikeClientProvider() {
    }

    public static AerospikeClient getClient(AerospikeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        return CLIENTS.computeIfAbsent(config.clientKey(), key -> createClient(config));
    }

    public static void closeAll() {
        for (AerospikeClient client : CLIENTS.values()) {
            try {
                client.close();
            } catch (RuntimeException ignored) {
                // Best effort shutdown.
            }
        }
        CLIENTS.clear();
    }

    private static AerospikeClient createClient(AerospikeConfig config) {
        ClientPolicy policy = new ClientPolicy();
        policy.user = config.getUser();
        policy.password = config.getPassword();
        return new AerospikeClient(policy, parseHosts(config.getHosts()));
    }

    private static Host[] parseHosts(String hosts) {
        String[] entries = hosts.split(",");
        Host[] parsed = new Host[entries.length];
        for (int i = 0; i < entries.length; i++) {
            parsed[i] = parseHost(entries[i]);
        }
        return parsed;
    }

    private static Host parseHost(String rawHost) {
        String value = rawHost == null ? "" : rawHost.trim();
        AerospikeValidation.requireNotBlank(value, "host");

        int separator = value.lastIndexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return new Host(value, DEFAULT_PORT);
        }

        String host = value.substring(0, separator).trim();
        int port = Integer.parseInt(value.substring(separator + 1).trim());
        AerospikeValidation.requirePositive(port, "port");
        return new Host(host, port);
    }
}
