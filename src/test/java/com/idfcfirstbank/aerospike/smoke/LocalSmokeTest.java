package com.idfcfirstbank.aerospike.smoke;

import com.idfcfirstbank.aerospike.api.AerospikeFunctions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class LocalSmokeTest {

    private LocalSmokeTest() {
    }

    public static void main(String[] args) {
        String setName = "customer";
        String key = "101";

        Map<String, Object> config = new HashMap<String, Object>();
        config.put("hosts", "localhost:3000");
        config.put("namespace", "test");
        config.put("tlsEnabled", false);
        config.put("authEnabled", false);
        config.put("maxConnectionsPerNode", 300);
        config.put("maxCommandsInProcess", 0);
        config.put("maxCommandsInQueue", 0);
        config.put("readTimeout", 1000);
        config.put("writeTimeout", 1000);
        config.put("connectTimeout", 1000);

        Map<String, Object> bins = new HashMap<String, Object>();
        bins.put("name", "Dileep");
        bins.put("city", "Pune");
        bins.put("score", new BigDecimal("10.5"));
        bins.put("visits", new BigDecimal("12"));

        System.out.println("PUT:");
        System.out.println(AerospikeFunctions.putRecordWithConfig(config, setName, key, bins, 0));

        System.out.println("GET:");
        System.out.println(AerospikeFunctions.getRecordWithConfig(config, setName, key));

        System.out.println("EXISTS:");
        System.out.println(AerospikeFunctions.existsWithConfig(config, setName, key));

        System.out.println("BATCH GET:");
        System.out.println(AerospikeFunctions.batchGetWithConfig(config, setName, Arrays.asList("101", "102")));

        AerospikeFunctions.closeAllClients();
    }
}
