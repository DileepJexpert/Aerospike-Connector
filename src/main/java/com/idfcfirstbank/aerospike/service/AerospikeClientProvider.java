package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.AuthMode;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.TlsPolicy;
import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import com.idfcfirstbank.aerospike.util.AerospikeValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(AerospikeClientProvider.class);

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

    /**
     * Closes and evicts every cached client. Intended for application shutdown.
     * Each entry is removed from the cache before it is closed so a concurrent
     * {@link #getClient(AerospikeConfig)} cannot hand back a client that this
     * method is about to close; it will instead build a fresh one. Callers
     * should still avoid invoking this while operations are actively in flight
     * on the same configuration.
     */
    public static void closeAll() {
        LOG.info("Closing all cached Aerospike clients");
        int closed = 0;
        for (String key : CLIENTS.keySet()) {
            AerospikeClient client = CLIENTS.remove(key);
            if (client == null) {
                continue;
            }
            try {
                client.close();
                closed++;
            } catch (RuntimeException ignored) {
                // Best effort shutdown.
            }
        }
        LOG.info("Closed {} Aerospike client(s)", closed);
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

        AerospikeClient client = new AerospikeClient(policy, parseHosts(config));
        LOG.info("Created Aerospike client for hosts={} namespace={} tls={} auth={}",
                config.getHosts(), config.getNamespace(), config.isTlsEnabled(), config.isAuthEnabled());
        return client;
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
        return password == null ? null : password.toCharArray();
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
        String host;
        int port = defaultPort;

        if (value.charAt(0) == '[') {
            int closing = value.indexOf(']');
            if (closing < 0) {
                throw new IllegalArgumentException("IPv6 host must be enclosed in brackets, e.g. [::1]:3000");
            }
            host = value.substring(1, closing).trim();
            String remainder = value.substring(closing + 1).trim();
            if (!remainder.isEmpty()) {
                if (remainder.charAt(0) != ':') {
                    throw new IllegalArgumentException("unexpected characters after IPv6 host: " + remainder);
                }
                port = parsePort(remainder.substring(1).trim());
            }
        } else if (value.indexOf(':') == value.lastIndexOf(':') && value.indexOf(':') > 0) {
            int separator = value.lastIndexOf(':');
            host = value.substring(0, separator).trim();
            port = parsePort(value.substring(separator + 1).trim());
        } else {
            // No port, or a bare (unbracketed) IPv6 literal with multiple colons.
            host = value;
        }

        AerospikeValidation.requireNotBlank(host, "host");
        AerospikeValidation.requirePositive(port, "port");

        if (config.isTlsEnabled()) {
            return new Host(host, config.getTlsName(), port);
        }
        return new Host(host, port);
    }

    private static int parsePort(String text) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("port must not be blank");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("port must be an integer but was '" + text + "'", exception);
        }
    }
}
