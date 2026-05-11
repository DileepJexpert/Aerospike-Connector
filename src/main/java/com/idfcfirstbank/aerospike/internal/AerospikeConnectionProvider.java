package com.idfcfirstbank.aerospike.internal;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.DisplayName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Password;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and validates Aerospike connections.
 *
 * hosts format examples:
 *   localhost
 *   localhost:3000
 *   host1:3000,host2:3000
 */
@Alias("basic")
public class AerospikeConnectionProvider implements ConnectionProvider<AerospikeConnection> {

    @Parameter
    @DisplayName("Hosts")
    private String hosts;

    @Parameter
    @Optional(defaultValue = "3000")
    @DisplayName("Default Port")
    private int defaultPort;

    @Parameter
    @Optional
    @DisplayName("Username")
    private String username;

    @Parameter
    @Password
    @Optional
    @DisplayName("Password")
    private String password;

    @Parameter
    @Optional(defaultValue = "1000")
    @DisplayName("Connection Timeout Millis")
    private int connectionTimeoutMillis;

    @Parameter
    @Optional(defaultValue = "1000")
    @DisplayName("Socket Timeout Millis")
    private int socketTimeoutMillis;

    @Override
    public AerospikeConnection connect() throws ConnectionException {
        try {
            ClientPolicy policy = new ClientPolicy();
            policy.timeout = connectionTimeoutMillis;
            policy.user = emptyToNull(username);
            policy.password = emptyToNull(password);

            AerospikeClient client = new AerospikeClient(policy, parseHosts(hosts, defaultPort));
            return new AerospikeConnection(client);
        } catch (Exception ex) {
            throw new ConnectionException("Unable to connect to Aerospike: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void disconnect(AerospikeConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public ConnectionValidationResult validate(AerospikeConnection connection) {
        if (connection != null && connection.isConnected()) {
            return ConnectionValidationResult.success();
        }
        return ConnectionValidationResult.failure("Aerospike client is not connected", null);
    }

    private Host[] parseHosts(String hostsText, int defaultPort) {
        if (hostsText == null || hostsText.trim().isEmpty()) {
            throw new IllegalArgumentException("hosts is required");
        }

        List<Host> result = new ArrayList<Host>();
        String[] entries = hostsText.split(",");
        for (String rawEntry : entries) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }

            String host = entry;
            int port = defaultPort;
            int colon = entry.lastIndexOf(':');
            if (colon > 0 && colon < entry.length() - 1) {
                host = entry.substring(0, colon).trim();
                port = Integer.parseInt(entry.substring(colon + 1).trim());
            }
            result.add(new Host(host, port));
        }

        return result.toArray(new Host[result.size()]);
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
