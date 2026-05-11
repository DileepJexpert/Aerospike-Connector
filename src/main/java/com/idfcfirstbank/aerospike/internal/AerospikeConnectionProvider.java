package com.idfcfirstbank.aerospike.internal;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;
import com.idfcfirstbank.aerospike.internal.error.AerospikeError;
import com.idfcfirstbank.aerospike.internal.util.AerospikeValidation;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.DisplayName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Password;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates and validates Aerospike connections.
 *
 * hosts format examples:
 *   localhost
 *   localhost:3000
 *   host1:3000,host2:3000
 *
 * CachedConnectionProvider lets Mule reuse the same connection object for a given connector config.
 * This matches the Aerospike client recommendation to use one long-lived client per application/config.
 */
@Alias("basic")
public class AerospikeConnectionProvider implements CachedConnectionProvider<AerospikeConnection> {

    private static final Logger LOGGER = Logger.getLogger(AerospikeConnectionProvider.class.getName());

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
    @Optional(defaultValue = "300")
    @DisplayName("Max Connections Per Node")
    private int maxConnectionsPerNode;

    @Override
    public AerospikeConnection connect() throws ConnectionException {
        try {
            AerospikeValidation.requireNotBlank(hosts, "hosts");
            AerospikeValidation.requireNonNegative(connectionTimeoutMillis, "connectionTimeoutMillis");
            AerospikeValidation.requireNonNegative(maxConnectionsPerNode, "maxConnectionsPerNode");

            ClientPolicy policy = new ClientPolicy();
            policy.timeout = connectionTimeoutMillis;
            policy.user = emptyToNull(username);
            policy.password = emptyToNull(password);
            policy.maxConnsPerNode = maxConnectionsPerNode;

            Host[] parsedHosts = parseHosts(hosts, defaultPort);
            LOGGER.info("Connecting to Aerospike hosts=" + hosts);

            AerospikeClient client = new AerospikeClient(policy, parsedHosts);
            LOGGER.info("Aerospike connection established");
            return new AerospikeConnection(client);
        } catch (ModuleException ex) {
            throw new ConnectionException(ex.getMessage(), ex);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to connect to Aerospike", ex);
            throw new ConnectionException("Unable to connect to Aerospike: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void disconnect(AerospikeConnection connection) {
        if (connection != null) {
            LOGGER.info("Disconnecting Aerospike client");
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
        AerospikeValidation.requireNotBlank(hostsText, "hosts");
        if (defaultPort <= 0) {
            throw new ModuleException("defaultPort must be greater than zero", AerospikeError.VALIDATION_FAILED);
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

            AerospikeValidation.requireNotBlank(host, "host");
            if (port <= 0) {
                throw new ModuleException("port must be greater than zero", AerospikeError.VALIDATION_FAILED);
            }
            result.add(new Host(host, port));
        }

        if (result.isEmpty()) {
            throw new ModuleException("At least one Aerospike host is required", AerospikeError.VALIDATION_FAILED);
        }

        return result.toArray(new Host[result.size()]);
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
