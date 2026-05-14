package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.AuthMode;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.TlsPolicy;
import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.util.AerospikeValidation;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AerospikeClientProvider {

    private static final int DEFAULT_PORT = 3000;
    private static final int DEFAULT_TLS_PORT = 4333;
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
        policy.maxConnsPerNode = config.getMaxConnectionsPerNode();

        if (config.getConnectTimeout() > 0) {
            policy.timeout = config.getConnectTimeout();
        }

        if (config.isAuthEnabled()) {
            policy.user = config.getUser();
            policy.password = config.getPassword();
            if (config.getAuthMode() != null) {
                policy.authMode = AuthMode.valueOf(config.getAuthMode().trim().toUpperCase());
            }
        }

        if (config.isTlsEnabled()) {
            policy.tlsPolicy = tlsPolicy(config);
        }

        return new AerospikeClient(policy, parseHosts(config));
    }

    private static TlsPolicy tlsPolicy(AerospikeConfig config) {
        TlsPolicy tlsPolicy = new TlsPolicy();
        if (config.getTrustStorePath() != null || config.getKeyStorePath() != null) {
            tlsPolicy.context = sslContext(config);
        }
        return tlsPolicy;
    }

    private static SSLContext sslContext(AerospikeConfig config) {
        try {
            TrustManagerFactory trustManagerFactory = null;
            KeyManagerFactory keyManagerFactory = null;

            if (config.getTrustStorePath() != null) {
                KeyStore trustStore = loadStore(config.getTrustStorePath(), config.getTrustStorePassword());
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
            }

            if (config.getKeyStorePath() != null) {
                KeyStore keyStore = loadStore(config.getKeyStorePath(), config.getKeyStorePassword());
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, passwordChars(config.getKeyStorePassword()));
            }

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(
                    keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(),
                    trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(),
                    null);
            return context;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalArgumentException("Unable to create Aerospike TLS context: " + exception.getMessage(), exception);
        }
    }

    private static KeyStore loadStore(String path, String password) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream input = new FileInputStream(path)) {
            keyStore.load(input, passwordChars(password));
        }
        return keyStore;
    }

    private static char[] passwordChars(String password) {
        return password == null ? new char[0] : password.toCharArray();
    }

    private static Host[] parseHosts(AerospikeConfig config) {
        String[] entries = config.getHosts().split(",");
        Host[] parsed = new Host[entries.length];
        for (int i = 0; i < entries.length; i++) {
            parsed[i] = parseHost(entries[i], config);
        }
        return parsed;
    }

    private static Host parseHost(String rawHost, AerospikeConfig config) {
        String value = rawHost == null ? "" : rawHost.trim();
        AerospikeValidation.requireNotBlank(value, "host");

        int defaultPort = config.isTlsEnabled() ? DEFAULT_TLS_PORT : DEFAULT_PORT;
        int separator = value.lastIndexOf(':');
        String host = value;
        int port = defaultPort;

        if (separator > 0 && separator < value.length() - 1) {
            host = value.substring(0, separator).trim();
            port = Integer.parseInt(value.substring(separator + 1).trim());
        }

        AerospikeValidation.requireNotBlank(host, "host");
        AerospikeValidation.requirePositive(port, "port");

        if (config.isTlsEnabled()) {
            return new Host(host, config.getTlsName(), port);
        }
        return new Host(host, port);
    }
}
