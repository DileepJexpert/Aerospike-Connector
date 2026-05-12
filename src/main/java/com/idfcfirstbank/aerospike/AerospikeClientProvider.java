package com.idfcfirstbank.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;

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
                // Best effort shutdown for Mule app stop/redeploy hooks.
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
        String[] hostParts = hosts.split(",");
        Host[] parsedHosts = new Host[hostParts.length];
        for (int i = 0; i < hostParts.length; i++) {
            parsedHosts[i] = parseHost(hostParts[i]);
        }
        return parsedHosts;
    }

    private static Host parseHost(String hostValue) {
        String value = hostValue == null ? "" : hostValue.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("hosts contains a blank host entry");
        }

        int separator = value.lastIndexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return new Host(value, DEFAULT_PORT);
        }

        String host = value.substring(0, separator).trim();
        int port = Integer.parseInt(value.substring(separator + 1).trim());
        return new Host(host, port);
    }
}
