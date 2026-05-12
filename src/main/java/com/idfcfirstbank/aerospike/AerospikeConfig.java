package com.idfcfirstbank.aerospike;

import java.util.Objects;

public final class AerospikeConfig {

    private final String hosts;
    private final String user;
    private final String password;

    public AerospikeConfig(String hosts) {
        this(hosts, null, null);
    }

    public AerospikeConfig(String hosts, String user, String password) {
        if (hosts == null || hosts.trim().isEmpty()) {
            throw new IllegalArgumentException("hosts must not be blank");
        }
        this.hosts = hosts.trim();
        this.user = trimToNull(user);
        this.password = trimToNull(password);
    }

    public String getHosts() {
        return hosts;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    String clientKey() {
        return hosts + "|" + nullToEmpty(user) + "|" + nullToEmpty(password);
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AerospikeConfig)) {
            return false;
        }
        AerospikeConfig that = (AerospikeConfig) o;
        return hosts.equals(that.hosts)
                && Objects.equals(user, that.user)
                && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hosts, user, password);
    }
}
