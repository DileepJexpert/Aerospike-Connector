package com.idfcfirstbank.aerospike;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
