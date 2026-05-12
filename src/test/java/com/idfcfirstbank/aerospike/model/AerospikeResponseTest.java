package com.idfcfirstbank.aerospike.model;

import com.aerospike.client.Record;
import org.junit.jupiter.api.Test;

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

        Map<String, Object> response = AerospikeResponse.record("test", "customer", "123", record);

        assertTrue((Boolean) response.get("success"));
        assertTrue((Boolean) response.get("found"));
        assertEquals("test", response.get("namespace"));
        assertEquals("customer", response.get("set"));
        assertEquals("123", response.get("key"));
        assertEquals(bins, response.get("bins"));
    }

    @Test
    void buildsNotFoundResponse() {
        Map<String, Object> response = AerospikeResponse.record("test", "customer", "123", null);

        assertFalse((Boolean) response.get("success"));
        assertFalse((Boolean) response.get("found"));
        assertEquals("123", response.get("key"));
    }
}
