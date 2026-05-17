package com.idfcfirstbank.aerospike.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AerospikeConfigTest {

    @Test
    void trimsValuesAndKeepsAuthFields() {
        AerospikeConfig config = new AerospikeConfig(" localhost:3000 ", " user ", " secret ");

        assertEquals("localhost:3000", config.getHosts());
        assertEquals("user", config.getUser());
        assertEquals("secret", config.getPassword());
    }

    @Test
    void blanksBecomeNullForOptionalAuthFields() {
        AerospikeConfig config = new AerospikeConfig("localhost:3000", " ", null);

        assertNull(config.getUser());
        assertNull(config.getPassword());
    }

    @Test
    void rejectsBlankHosts() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AerospikeConfig("   "));

        assertEquals("hosts must not be blank", exception.getMessage());
    }

    @Test
    void buildsProdStyleConfigFromMap() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("hosts", "prod-aerospike:4333");
        values.put("namespace", "prod_ns");
        values.put("tlsEnabled", "true");
        values.put("authEnabled", true);
        values.put("tlsName", "prod-tls-name");
        values.put("userName", "prod_user");
        values.put("password", "prod_password");
        values.put("maxConnectionsPerNode", "500");
        values.put("maxCommandsInProcess", 10);
        values.put("maxCommandsInQueue", 20);
        values.put("readTimeout", "1000");
        values.put("writeTimeout", 2000);
        values.put("connectTimeout", 3000);

        AerospikeConfig config = AerospikeConfig.fromMap(values);

        assertEquals("prod-aerospike:4333", config.getHosts());
        assertEquals("prod_ns", config.getNamespace());
        assertTrue(config.isTlsEnabled());
        assertTrue(config.isAuthEnabled());
        assertEquals("prod-tls-name", config.getTlsName());
        assertEquals("prod_user", config.getUser());
        assertEquals("prod_password", config.getPassword());
        assertEquals(500, config.getMaxConnectionsPerNode());
        assertEquals(10, config.getMaxCommandsInProcess());
        assertEquals(20, config.getMaxCommandsInQueue());
        assertEquals(1000, config.getReadTimeout());
        assertEquals(2000, config.getWriteTimeout());
        assertEquals(3000, config.getConnectTimeout());
    }

    @Test
    void clientKeyDistinguishesDifferentStorePasswords() {
        AerospikeConfig first = AerospikeConfig.builder()
                .hosts("h:3000")
                .trustStorePath("/t.jks")
                .trustStorePassword("one")
                .build();
        AerospikeConfig second = AerospikeConfig.builder()
                .hosts("h:3000")
                .trustStorePath("/t.jks")
                .trustStorePassword("two")
                .build();

        assertNotEquals(first.clientKey(), second.clientKey());
    }

    @Test
    void clientKeyDoesNotLeakPlaintextSecrets() {
        AerospikeConfig config = new AerospikeConfig("h:3000", "user", "sup3rSecret");

        assertFalse(config.clientKey().contains("sup3rSecret"));
    }

    @Test
    void invalidIntegerConfigValueReportsKey() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("hosts", "h:3000");
        values.put("readTimeout", "not-a-number");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeConfig.fromMap(values));

        assertTrue(exception.getMessage().contains("readTimeout"));
    }

    @Test
    void rejectsInvalidAuthModeEarly() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("hosts", "h:3000");
        values.put("authEnabled", true);
        values.put("username", "u");
        values.put("password", "p");
        values.put("authMode", "NOPE");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeConfig.fromMap(values));

        assertTrue(exception.getMessage().contains("authMode"));
    }

    @Test
    void acceptsValidAuthModeCaseInsensitively() {
        AerospikeConfig config = AerospikeConfig.builder()
                .hosts("h:3000")
                .authEnabled(true)
                .user("u")
                .password("p")
                .authMode("external")
                .build();

        assertEquals("external", config.getAuthMode());
    }

    @Test
    void fromMapAutoEnablesAuthWhenCredentialsPresentAndFlagAbsent() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("hosts", "h:3000");
        values.put("username", "u");
        values.put("password", "p");

        AerospikeConfig config = AerospikeConfig.fromMap(values);

        assertTrue(config.isAuthEnabled());
    }

    @Test
    void fromMapRespectsExplicitAuthDisabledEvenWithCredentials() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("hosts", "h:3000");
        values.put("username", "u");
        values.put("password", "p");
        values.put("authEnabled", false);

        AerospikeConfig config = AerospikeConfig.fromMap(values);

        assertFalse(config.isAuthEnabled());
    }

    @Test
    void rejectsTlsWithoutTlsName() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("hosts", "prod-aerospike:4333");
        values.put("tlsEnabled", true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeConfig.fromMap(values));

        assertEquals("tlsName is required when tlsEnabled is true", exception.getMessage());
    }
}
