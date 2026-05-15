# Mule Developer Guide for `aerospike-client-lib`

This guide explains how Mule developers should use the same Java JAR in local, DEV, SIT, and PROD.

This project is a plain Java JAR, not a Mule SDK connector. Mule applications use it through Mule Java Module or DataWeave static Java calls.

## 1. Dependency

Add this dependency in the Mule application `pom.xml`:

```xml
<dependency>
    <groupId>com.idfcfirstbank</groupId>
    <artifactId>aerospike-client-lib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Main class:

```text
com.idfcfirstbank.aerospike.api.AerospikeFunctions
```

Java target:

```text
Java 17, aligned with Mule 4.9 running on Java 17
```

Aerospike dependency:

```text
com.aerospike:aerospike-client-jdk8:10.0.0
```

The artifact name remains `aerospike-client-jdk8` because Aerospike uses that package for JDK 8 through JDK 20. Use `aerospike-client-jdk21` only when the runtime itself is Java 21 or later.

## 2. Same code for every environment

Use the same Java methods everywhere. Only the config values change.

Local:

```yaml
aerospike:
  hosts: localhost:3000
  namespace: test
  tlsEnabled: false
  authEnabled: false
  tlsName:
  username:
  password:
  maxConnectionsPerNode: 300
  maxCommandsInProcess: 0
  maxCommandsInQueue: 0
  writeTimeout: 1000
  readTimeout: 1000
  connectTimeout: 1000
  trustStorePath:
  trustStorePassword:
  keyStorePath:
  keyStorePassword:
```

DEV / SIT / PROD:

```yaml
aerospike:
  hosts: prod-host-1:4333,prod-host-2:4333
  namespace: prod_namespace
  tlsEnabled: true
  authEnabled: true
  tlsName: prod_tls_name
  username: ${AEROSPIKE_USERNAME}
  password: ${AEROSPIKE_PASSWORD}
  maxConnectionsPerNode: 300
  maxCommandsInProcess: 0
  maxCommandsInQueue: 0
  writeTimeout: 1000
  readTimeout: 1000
  connectTimeout: 1000
  trustStorePath: /opt/mule/certs/aerospike-truststore.jks
  trustStorePassword: ${AEROSPIKE_TRUSTSTORE_PASSWORD}
  keyStorePath:
  keyStorePassword:
```

For mutual TLS, also set:

```yaml
  keyStorePath: /opt/mule/certs/aerospike-keystore.jks
  keyStorePassword: ${AEROSPIKE_KEYSTORE_PASSWORD}
```

For PKI authentication, also set:

```yaml
  authMode: PKI
```

Confirm PKI user/certificate mapping with the Aerospike platform team before using it.

## 3. Mule Java Module call

Recommended method:

```text
getRecordWithConfig(java.util.Map, String, String)
```

Example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecordWithConfig(java.util.Map, String, String)">
    <java:args><![CDATA[#[{
        arg0: {
            hosts: p('aerospike.hosts'),
            namespace: p('aerospike.namespace'),
            tlsEnabled: p('aerospike.tlsEnabled') as Boolean,
            authEnabled: p('aerospike.authEnabled') as Boolean,
            tlsName: p('aerospike.tlsName') default null,
            username: p('aerospike.username') default null,
            password: p('aerospike.password') default null,
            maxConnectionsPerNode: p('aerospike.maxConnectionsPerNode') as Number,
            maxCommandsInProcess: p('aerospike.maxCommandsInProcess') as Number,
            maxCommandsInQueue: p('aerospike.maxCommandsInQueue') as Number,
            readTimeout: p('aerospike.readTimeout') as Number,
            writeTimeout: p('aerospike.writeTimeout') as Number,
            connectTimeout: p('aerospike.connectTimeout') as Number,
            trustStorePath: p('aerospike.trustStorePath') default null,
            trustStorePassword: p('aerospike.trustStorePassword') default null,
            keyStorePath: p('aerospike.keyStorePath') default null,
            keyStorePassword: p('aerospike.keyStorePassword') default null,
            authMode: p('aerospike.authMode') default null
        },
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id
    }]]]></java:args>
</java:invoke-static>
```

Other config-map methods:

```text
putRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
putRecordWithConfig(Map config, String setName, String key, Map bins, Number ttlSeconds)
putRecordWithConfig(Map config, String setName, String key, Map bins)
deleteRecordWithConfig(Map config, String setName, String key)
existsWithConfig(Map config, String setName, String key)
batchGetWithConfig(Map config, String setName, List keys)
```

For Mule, the no-TTL or `Number` TTL overload is often easier because DataWeave numeric values may not resolve as Java primitive `int`.

## 4. Local Docker Compose

Start local Aerospike:

```bash
docker compose up -d
```

Local Mule values:

```properties
aerospike.hosts=localhost:3000
aerospike.namespace=test
aerospike.tlsEnabled=false
aerospike.authEnabled=false
```

When Mule runs on your laptop, connect to `localhost:3000`. Do not use the Docker container IP.

## 5. EKS / PROD connectivity

If Aerospike is deployed in EKS, the `hosts` value must be a client-reachable seed endpoint.

Do not guess pod IPs. Confirm the endpoint with the Aerospike / EKS platform team.

Typical options:

1. Mule runtime inside same EKS cluster or same Kubernetes network:
   use the approved Kubernetes service DNS or Aerospike access address.

2. Mule runtime outside EKS but inside same VPC / connected network:
   use internal NLB, private DNS, PrivateLink, or a platform-approved internal endpoint.

3. Mule runtime outside the network:
   route through the approved enterprise network path before trying the Java library.

Confirm:

1. exact seed hostnames
2. client port: usually `3000` for non-TLS or `4333` for TLS
3. whether TLS is mandatory
4. whether username/password auth is enabled
5. whether mTLS or PKI auth is enabled
6. whether Mule can route to the endpoint
7. what certificate name must match `tlsName`

## 6. Security behavior

When `tlsEnabled=true`, the library:

- creates an Aerospike `TlsPolicy`
- creates hosts using `new Host(host, tlsName, port)`
- defaults host port to `4333` when no port is supplied
- loads `trustStorePath` when supplied
- loads `keyStorePath` when supplied

When `authEnabled=true`, the library:

- requires `username`
- requires `password`
- sets `ClientPolicy.user`
- sets `ClientPolicy.password`
- optionally sets `ClientPolicy.authMode`

Supported auth modes are Aerospike Java client values:

```text
INTERNAL
EXTERNAL
EXTERNAL_INSECURE
PKI
```

## 7. Tuning behavior

Applied by the library:

- `maxConnectionsPerNode`
- `readTimeout`
- `writeTimeout`
- `connectTimeout`

Accepted but not applied by the current synchronous implementation:

- `maxCommandsInProcess`
- `maxCommandsInQueue`

Those two fields are kept so Mule can pass the same enterprise config shape. They are typically meaningful for async/event-loop style clients, while this library currently uses synchronous Aerospike operations.

## 8. Secrets

Do not keep PROD credentials or store passwords in Git.

Use Mule secure properties, Runtime Manager properties, Kubernetes secrets, or approved environment variables:

```yaml
username: ${AEROSPIKE_USERNAME}
password: ${AEROSPIKE_PASSWORD}
trustStorePassword: ${AEROSPIKE_TRUSTSTORE_PASSWORD}
keyStorePassword: ${AEROSPIKE_KEYSTORE_PASSWORD}
```

## 9. Official references

- Mule Java Module invoke methods:
  [MuleSoft Java Module](https://docs.mulesoft.com/java-module/1.2/java-invoke-method)
- Mule property configuration:
  [Mule Configuring Properties](https://docs.mulesoft.com/mule-runtime/latest/configuring-properties)
- Aerospike Java client connection guide:
  [Aerospike Java Client Connecting](https://aerospike.com/docs/develop/client/java/connect/)
- Aerospike TLS overview:
  [Aerospike TLS Security](https://aerospike.com/docs/database/learn/security/tls)
- Aerospike TLS configuration:
  [Aerospike TLS Configuration](https://aerospike.com/docs/database/manage/network/tls/)
- Aerospike RBAC and access control:
  [Aerospike Access Control](https://aerospike.com/docs/database/manage/security/rbac/)
