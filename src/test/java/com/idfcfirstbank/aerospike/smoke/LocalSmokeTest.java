package com.idfcfirstbank.aerospike.smoke;

import com.idfcfirstbank.aerospike.api.AerospikeFunctions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class LocalSmokeTest {

    private LocalSmokeTest() {
    }

    public static void main(String[] args) {
        String hosts = "localhost:3000";
        String namespace = "test";
        String setName = "customer";
        String key = "101";

        Map<String, Object> bins = new HashMap<String, Object>();
        bins.put("name", "Dileep");
        bins.put("city", "Pune");

        System.out.println("PUT:");
        System.out.println(AerospikeFunctions.putRecord(hosts, namespace, setName, key, bins, 0));

        System.out.println("GET:");
        System.out.println(AerospikeFunctions.getRecord(hosts, namespace, setName, key));

        System.out.println("EXISTS:");
        System.out.println(AerospikeFunctions.exists(hosts, namespace, setName, key));

        System.out.println("BATCH GET:");
        System.out.println(AerospikeFunctions.batchGet(hosts, namespace, setName, Arrays.asList("101", "102")));

        AerospikeFunctions.closeAllClients();
    }
}
