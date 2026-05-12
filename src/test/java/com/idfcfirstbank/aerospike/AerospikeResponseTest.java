package com.idfcfirstbank.aerospike;

import com.aerospike.client.Record;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AerospikeResponseTest {

    @Test
    void buildsFoundRecordResponse() {
        Map<String, Object> bins = new HashMap<String, Object>();
        bins.put("name", "Dileep");
        Record record = new Record(bins, 4, 120);

        Map<String, Object> response = AerospikeResponse.record("123", record);

        assertTrue((Boolean) response.get("success"));
        assertTrue((Boolean) response.get("found"));
        assertEquals("123", response.get("key"));
        assertEquals(bins, response.get("bins"));
        assertEquals(4, response.get("generation"));
        assertEquals(120, response.get("expiration"));
    }

    @Test
    void buildsNotFoundResponse() {
        Map<String, Object> response = AerospikeResponse.record("123", null);

        assertFalse((Boolean) response.get("success"));
        assertFalse((Boolean) response.get("found"));
        assertEquals("123", response.get("key"));
    }

    @Test
    void buildsSuccessResponse() {
        Map<String, Object> response = AerospikeResponse.success("123", true);

        assertTrue((Boolean) response.get("success"));
        assertEquals("123", response.get("key"));
        assertEquals(Boolean.TRUE, response.get("value"));
        assertEquals(Collections.singleton("success"), Collections.singleton("success"));
    }
}
