package com.idfcfirstbank.aerospike.internal;

import com.aerospike.client.AerospikeClient;

/**
 * Holds the reusable AerospikeClient instance for one Mule connector connection.
 */
public class AerospikeConnection {

    private final AerospikeClient client;

    public AerospikeConnection(AerospikeClient client) {
        this.client = client;
    }

    public AerospikeClient getClient() {
        return client;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void disconnect() {
        if (client != null) {
            client.close();
        }
    }
}
