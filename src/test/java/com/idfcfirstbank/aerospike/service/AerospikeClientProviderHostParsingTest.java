package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.Host;
import com.idfcfirstbank.aerospike.config.AerospikeConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AerospikeClientProviderHostParsingTest {

    private static Host parse(String raw, AerospikeConfig config) throws Exception {
        Method method = AerospikeClientProvider.class.getDeclaredMethod(
                "parseHost", String.class, AerospikeConfig.class);
        method.setAccessible(true);
        return (Host) method.invoke(null, raw, config);
    }

    private static RuntimeException parseFailure(String raw, AerospikeConfig config) {
        return assertThrows(RuntimeException.class, () -> {
            try {
                parse(raw, config);
            } catch (InvocationTargetException exception) {
                throw (RuntimeException) exception.getCause();
            }
        });
    }

    @Test
    void parsesHostnameWithExplicitPort() throws Exception {
        Host host = parse("db.example.com:3100", new AerospikeConfig("seed:3000"));

        assertEquals("db.example.com", host.name);
        assertEquals(3100, host.port);
    }

    @Test
    void appliesDefaultPortWhenAbsent() throws Exception {
        Host host = parse("db.example.com", new AerospikeConfig("seed:3000"));

        assertEquals("db.example.com", host.name);
        assertEquals(3000, host.port);
    }

    @Test
    void appliesDefaultTlsPortWhenTlsEnabled() throws Exception {
        AerospikeConfig config = AerospikeConfig.builder()
                .hosts("seed:4333")
                .tlsEnabled(true)
                .tlsName("cluster-tls")
                .build();

        Host host = parse("secure-node", config);

        assertEquals("secure-node", host.name);
        assertEquals(4333, host.port);
        assertEquals("cluster-tls", host.tlsName);
    }

    @Test
    void parsesBracketedIpv6WithPort() throws Exception {
        Host host = parse("[2001:db8::1]:4333", new AerospikeConfig("seed:3000"));

        assertEquals("2001:db8::1", host.name);
        assertEquals(4333, host.port);
    }

    @Test
    void parsesBracketedIpv6WithoutPort() throws Exception {
        Host host = parse("[::1]", new AerospikeConfig("seed:3000"));

        assertEquals("::1", host.name);
        assertEquals(3000, host.port);
    }

    @Test
    void treatsBareIpv6AsHostWithDefaultPort() throws Exception {
        Host host = parse("2001:db8::1", new AerospikeConfig("seed:3000"));

        assertEquals("2001:db8::1", host.name);
        assertEquals(3000, host.port);
    }

    @Test
    void rejectsNonNumericPort() {
        RuntimeException exception = parseFailure("host:abc", new AerospikeConfig("seed:3000"));

        assertInstanceOf(IllegalArgumentException.class, exception);
    }

    @Test
    void rejectsUnterminatedIpv6Bracket() {
        RuntimeException exception = parseFailure("[::1:3000", new AerospikeConfig("seed:3000"));

        assertInstanceOf(IllegalArgumentException.class, exception);
    }
}
