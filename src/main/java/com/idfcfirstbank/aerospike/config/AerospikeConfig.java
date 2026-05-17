package com.idfcfirstbank.aerospike.config;

import com.aerospike.client.policy.AuthMode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;

public final class AerospikeConfig {

    private static final int DEFAULT_MAX_CONNECTIONS_PER_NODE = 300;

    private final String hosts;
    private final String namespace;
    private final boolean tlsEnabled;
    private final boolean authEnabled;
    private final String tlsName;
    private final String user;
    private final String password;
    private final int maxConnectionsPerNode;
    private final int maxCommandsInProcess;
    private final int maxCommandsInQueue;
    private final int readTimeout;
    private final int writeTimeout;
    private final int connectTimeout;
    private final String trustStorePath;
    private final String trustStorePassword;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String authMode;
    private final boolean sendKey;

    public AerospikeConfig(String hosts) {
        this(builder().hosts(hosts));
    }

    public AerospikeConfig(String hosts, String user, String password) {
        this(builder().hosts(hosts).authEnabled(hasText(user) || hasText(password)).user(user).password(password));
    }

    private AerospikeConfig(Builder builder) {
        if (!hasText(builder.hosts)) {
            throw new IllegalArgumentException("hosts must not be blank");
        }
        if (builder.authEnabled && (!hasText(builder.user) || !hasText(builder.password))) {
            throw new IllegalArgumentException("username and password are required when authEnabled is true");
        }
        if (builder.tlsEnabled && !hasText(builder.tlsName)) {
            throw new IllegalArgumentException("tlsName is required when tlsEnabled is true");
        }
        if (hasText(builder.authMode)) {
            validateAuthMode(builder.authMode);
        }

        this.hosts = builder.hosts.trim();
        this.namespace = trimToNull(builder.namespace);
        this.tlsEnabled = builder.tlsEnabled;
        this.authEnabled = builder.authEnabled;
        this.tlsName = trimToNull(builder.tlsName);
        this.user = trimToNull(builder.user);
        this.password = trimToNull(builder.password);
        this.maxConnectionsPerNode = positiveOrDefault(builder.maxConnectionsPerNode, DEFAULT_MAX_CONNECTIONS_PER_NODE);
        this.maxCommandsInProcess = nonNegativeOrZero(builder.maxCommandsInProcess);
        this.maxCommandsInQueue = nonNegativeOrZero(builder.maxCommandsInQueue);
        this.readTimeout = nonNegativeOrZero(builder.readTimeout);
        this.writeTimeout = nonNegativeOrZero(builder.writeTimeout);
        this.connectTimeout = nonNegativeOrZero(builder.connectTimeout);
        this.trustStorePath = trimToNull(builder.trustStorePath);
        this.trustStorePassword = trimToNull(builder.trustStorePassword);
        this.keyStorePath = trimToNull(builder.keyStorePath);
        this.keyStorePassword = trimToNull(builder.keyStorePassword);
        this.authMode = trimToNull(builder.authMode);
        this.sendKey = builder.sendKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AerospikeConfig fromMap(Map<String, Object> values) {
        if (values == null) {
            throw new IllegalArgumentException("config map must not be null");
        }

        String user = firstStringValue(values, "username", "userName", "user");
        String password = stringValue(values, "password", null);
        boolean credentialsPresent = hasText(user) && hasText(password);
        boolean authEnabled = values.containsKey("authEnabled")
                ? booleanValue(values, "authEnabled", false)
                : credentialsPresent;

        return builder()
                .hosts(stringValue(values, "hosts", null))
                .namespace(stringValue(values, "namespace", null))
                .tlsEnabled(booleanValue(values, "tlsEnabled", false))
                .authEnabled(authEnabled)
                .tlsName(stringValue(values, "tlsName", null))
                .user(user)
                .password(password)
                .maxConnectionsPerNode(intValue(values, "maxConnectionsPerNode", DEFAULT_MAX_CONNECTIONS_PER_NODE))
                .maxCommandsInProcess(intValue(values, "maxCommandsInProcess", 0))
                .maxCommandsInQueue(intValue(values, "maxCommandsInQueue", 0))
                .readTimeout(intValue(values, "readTimeout", 0))
                .writeTimeout(intValue(values, "writeTimeout", 0))
                .connectTimeout(intValue(values, "connectTimeout", 0))
                .trustStorePath(stringValue(values, "trustStorePath", null))
                .trustStorePassword(stringValue(values, "trustStorePassword", null))
                .keyStorePath(stringValue(values, "keyStorePath", null))
                .keyStorePassword(stringValue(values, "keyStorePassword", null))
                .authMode(stringValue(values, "authMode", null))
                .sendKey(booleanValue(values, "sendKey", true))
                .build();
    }

    public String getHosts() {
        return hosts;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public String getTlsName() {
        return tlsName;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getMaxConnectionsPerNode() {
        return maxConnectionsPerNode;
    }

    public int getMaxCommandsInProcess() {
        return maxCommandsInProcess;
    }

    public int getMaxCommandsInQueue() {
        return maxCommandsInQueue;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getAuthMode() {
        return authMode;
    }

    /**
     * When true the user key is stored alongside the record on writes and
     * requested back on reads/queries/scans, so query and findAll results
     * carry the real key instead of only the record digest. Defaults to true.
     */
    public boolean isSendKey() {
        return sendKey;
    }

    /**
     * Cache key identifying a unique effective client configuration. Secret
     * material (user password, trust/key store passwords) is folded into a
     * one-way fingerprint so that rotating any secret produces a distinct key
     * while no plaintext credential is ever held as a map key.
     */
    public String clientKey() {
        return hosts
                + "|" + nullToEmpty(namespace)
                + "|" + tlsEnabled
                + "|" + authEnabled
                + "|" + nullToEmpty(tlsName)
                + "|" + nullToEmpty(user)
                + "|" + maxConnectionsPerNode
                + "|" + maxCommandsInProcess
                + "|" + maxCommandsInQueue
                + "|" + readTimeout
                + "|" + writeTimeout
                + "|" + connectTimeout
                + "|" + nullToEmpty(trustStorePath)
                + "|" + nullToEmpty(keyStorePath)
                + "|" + nullToEmpty(authMode)
                + "|" + sendKey
                + "|" + secretFingerprint();
    }

    private String secretFingerprint() {
        String material = nullToEmpty(password)
                + '\u001f' + nullToEmpty(trustStorePassword)
                + '\u001f' + nullToEmpty(keyStorePassword);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte hashByte : hash) {
                builder.append(Character.forDigit((hashByte >> 4) & 0xF, 16));
                builder.append(Character.forDigit(hashByte & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required but unavailable", exception);
        }
    }

    private static void validateAuthMode(String authMode) {
        try {
            AuthMode.valueOf(authMode.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            StringBuilder allowed = new StringBuilder();
            for (AuthMode mode : AuthMode.values()) {
                if (allowed.length() > 0) {
                    allowed.append(", ");
                }
                allowed.append(mode.name());
            }
            throw new IllegalArgumentException(
                    "authMode '" + authMode + "' is invalid; must be one of: " + allowed);
        }
    }

    private static String firstStringValue(Map<String, Object> values, String firstKey, String secondKey, String thirdKey) {
        String value = stringValue(values, firstKey, null);
        if (value != null) {
            return value;
        }
        value = stringValue(values, secondKey, null);
        return value == null ? stringValue(values, thirdKey, null) : value;
    }

    private static String stringValue(Map<String, Object> values, String key, String defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private static boolean booleanValue(Map<String, Object> values, String key, boolean defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private static int intValue(Map<String, Object> values, String key, int defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "config value for '" + key + "' must be an integer but was '" + text + "'", exception);
        }
    }

    private static String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "numeric config values must not be negative (use 0 to fall back to the default)");
        }
        return value == 0 ? defaultValue : value;
    }

    private static int nonNegativeOrZero(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("numeric config values must not be negative");
        }
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AerospikeConfig)) {
            return false;
        }
        AerospikeConfig that = (AerospikeConfig) object;
        return tlsEnabled == that.tlsEnabled
                && authEnabled == that.authEnabled
                && maxConnectionsPerNode == that.maxConnectionsPerNode
                && maxCommandsInProcess == that.maxCommandsInProcess
                && maxCommandsInQueue == that.maxCommandsInQueue
                && readTimeout == that.readTimeout
                && writeTimeout == that.writeTimeout
                && connectTimeout == that.connectTimeout
                && hosts.equals(that.hosts)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(tlsName, that.tlsName)
                && Objects.equals(user, that.user)
                && Objects.equals(password, that.password)
                && Objects.equals(trustStorePath, that.trustStorePath)
                && Objects.equals(trustStorePassword, that.trustStorePassword)
                && Objects.equals(keyStorePath, that.keyStorePath)
                && Objects.equals(keyStorePassword, that.keyStorePassword)
                && Objects.equals(authMode, that.authMode)
                && sendKey == that.sendKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                hosts,
                namespace,
                tlsEnabled,
                authEnabled,
                tlsName,
                user,
                password,
                maxConnectionsPerNode,
                maxCommandsInProcess,
                maxCommandsInQueue,
                readTimeout,
                writeTimeout,
                connectTimeout,
                trustStorePath,
                trustStorePassword,
                keyStorePath,
                keyStorePassword,
                authMode,
                sendKey);
    }

    public static final class Builder {
        private String hosts;
        private String namespace;
        private boolean tlsEnabled;
        private boolean authEnabled;
        private String tlsName;
        private String user;
        private String password;
        private int maxConnectionsPerNode = DEFAULT_MAX_CONNECTIONS_PER_NODE;
        private int maxCommandsInProcess;
        private int maxCommandsInQueue;
        private int readTimeout;
        private int writeTimeout;
        private int connectTimeout;
        private String trustStorePath;
        private String trustStorePassword;
        private String keyStorePath;
        private String keyStorePassword;
        private String authMode;
        private boolean sendKey = true;

        private Builder() {
        }

        public Builder hosts(String hosts) {
            this.hosts = hosts;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder tlsEnabled(boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
            return this;
        }

        public Builder authEnabled(boolean authEnabled) {
            this.authEnabled = authEnabled;
            return this;
        }

        public Builder tlsName(String tlsName) {
            this.tlsName = tlsName;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder maxConnectionsPerNode(int maxConnectionsPerNode) {
            this.maxConnectionsPerNode = maxConnectionsPerNode;
            return this;
        }

        /**
         * Accepted for forward compatibility with Mule config maps. These are
         * async event-loop tuning values and are not applied to the synchronous
         * Aerospike client this library uses; they are retained so existing
         * environment property files continue to bind without error.
         */
        public Builder maxCommandsInProcess(int maxCommandsInProcess) {
            this.maxCommandsInProcess = maxCommandsInProcess;
            return this;
        }

        /**
         * Accepted for forward compatibility with Mule config maps. See
         * {@link #maxCommandsInProcess(int)} for why this is not applied to the
         * synchronous client.
         */
        public Builder maxCommandsInQueue(int maxCommandsInQueue) {
            this.maxCommandsInQueue = maxCommandsInQueue;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder writeTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder trustStorePath(String trustStorePath) {
            this.trustStorePath = trustStorePath;
            return this;
        }

        public Builder trustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }

        public Builder keyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
            return this;
        }

        public Builder keyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
            return this;
        }

        public Builder authMode(String authMode) {
            this.authMode = authMode;
            return this;
        }

        public Builder sendKey(boolean sendKey) {
            this.sendKey = sendKey;
            return this;
        }

        public AerospikeConfig build() {
            return new AerospikeConfig(this);
        }
    }
}
