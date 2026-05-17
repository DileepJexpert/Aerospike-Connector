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

If Mule needs only selected "columns", use the projected-read method:

```text
getRecordFieldsWithConfig(java.util.Map, String, String, java.util.List)
```

In this library:

- Aerospike set = what many teams informally call a table
- Aerospike bin = what many teams informally call a column

So if a record has 5 bins and Mule only wants 4, pass those 4 bin names in the list.

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
getRecordFieldsWithConfig(Map config, String setName, String key, List fieldNames)
putRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
putRecordWithConfig(Map config, String setName, String key, Map bins, Number ttlSeconds)
putRecordWithConfig(Map config, String setName, String key, Map bins)
deleteRecordWithConfig(Map config, String setName, String key)
existsWithConfig(Map config, String setName, String key)
batchGetWithConfig(Map config, String setName, List keys)
batchGetFieldsWithConfig(Map config, String setName, List keys, List fieldNames)
queryRecordsByFieldEqualsWithConfig(Map config, String setName, String fieldName, Object fieldValue, List fieldNames)
queryRecordsByFieldRangeWithConfig(Map config, String setName, String fieldName, Number rangeBegin, Number rangeEnd, List fieldNames)
findAllWithConfig(Map config, String setName)
findAllFieldsWithConfig(Map config, String setName, List fieldNames)
queryWithConfig(Map config, String setName, Map criteria, List fieldNames)
createRecordWithConfig(Map config, String setName, String key, Map bins)
createRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
replaceRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
updateRecordWithConfig(Map config, String setName, String key, Map bins)
updateRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
putRecordIfGenerationWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds, int expectedGeneration)
incrementBinsWithConfig(Map config, String setName, String key, Map deltas)
incrementBinsWithConfig(Map config, String setName, String key, Map deltas, int ttlSeconds)
touchRecordWithConfig(Map config, String setName, String key, int ttlSeconds)
pingWithConfig(Map config)
```

For Mule, the no-TTL or `Number` TTL overload is often easier because DataWeave numeric values may not resolve as Java primitive `int`.

Projected read example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecordFieldsWithConfig(java.util.Map, String, String, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: {
            hosts: p('aerospike.hosts'),
            namespace: p('aerospike.namespace'),
            tlsEnabled: p('aerospike.tlsEnabled') as Boolean,
            authEnabled: p('aerospike.authEnabled') as Boolean,
            tlsName: p('aerospike.tlsName') default null,
            username: p('aerospike.username') default null,
            password: p('aerospike.password') default null
        },
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id,
        arg3: ['firstName', 'lastName', 'age', 'status']
    }]]]></java:args>
</java:invoke-static>
```

Batch projected read example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="batchGetFieldsWithConfig(java.util.Map, String, java.util.List, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: ['1001', '1002', '1003'],
        arg3: ['firstName', 'city']
    }]]]></java:args>
</java:invoke-static>
```

Query example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="queryRecordsByFieldEqualsWithConfig(java.util.Map, String, String, Object, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: 'city',
        arg3: 'Mumbai',
        arg4: ['firstName', 'lastName', 'city']
    }]]]></java:args>
</java:invoke-static>
```

Range query example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="queryRecordsByFieldRangeWithConfig(java.util.Map, String, String, Number, Number, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: 'age',
        arg3: 25,
        arg4: 40,
        arg5: ['firstName', 'age', 'city']
    }]]]></java:args>
</java:invoke-static>
```

Important:

1. `queryRecordsByFieldEqualsWithConfig` and `queryRecordsByFieldRangeWithConfig` need an Aerospike secondary index on the filtered bin.
2. Equality queries currently support non-blank `String` and integer `Number` values.
3. Range queries currently support integer `Number` values only.

Example AQL index creation for local testing:

```sql
CREATE INDEX idx_customer_city ON test.customer (city) STRING
CREATE INDEX idx_customer_age ON test.customer (age) NUMERIC
```

## 3A. New write and maintenance operations

These operations were added to give Mule teams full control over how records are written. Choose the right one for your use case.

### Which write method to use?

| You want to…                               | Method to call                  |
|--------------------------------------------|---------------------------------|
| Insert or overwrite (upsert)               | `putRecordWithConfig`           |
| Insert only — fail if already exists       | `createRecordWithConfig`        |
| Replace all bins — fail if not exists      | `replaceRecordWithConfig`       |
| Update some bins — fail if not exists      | `updateRecordWithConfig`        |
| Write only if nobody changed it since read | `putRecordIfGenerationWithConfig` |
| Add/subtract from a counter bin atomically | `incrementBinsWithConfig`       |
| Reset TTL without rewriting bins           | `touchRecordWithConfig`         |
| Check if the cluster is reachable          | `pingWithConfig`                |

---

### createRecord — insert only

Use this when the record must not exist yet (e.g. account registration).  
Throws `RECORD_ALREADY_EXISTS` if the key is already in Aerospike.

Method signature:
```text
createRecordWithConfig(Map config, String setName, String key, Map bins)
createRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
```

Arguments:
- `arg0` — config map (same shape as all other methods)
- `arg1` — set name (e.g. `"customer"`)
- `arg2` — record key (string, e.g. account ID)
- `arg3` — bins map (key = bin name, value = bin value)
- `arg4` — TTL in seconds (`0` = namespace default, `-1` = never expire)

Mule example:
```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="createRecordWithConfig(java.util.Map, String, String, java.util.Map)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id,
        arg3: payload
    }]]]></java:args>
</java:invoke-static>
```

Error handling tip: catch `RECORD_ALREADY_EXISTS` and return HTTP 409 Conflict.

---

### replaceRecord — replace all bins

Use this when you want to completely overwrite a record, removing any bins not present in your new payload.  
Throws `KEY_NOT_FOUND` if the record does not exist.

Method signature:
```text
replaceRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
```

Arguments:
- `arg0` — config map
- `arg1` — set name
- `arg2` — record key
- `arg3` — bins map — any bins NOT in this map are deleted from the record
- `arg4` — TTL in seconds

Mule example:
```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="replaceRecordWithConfig(java.util.Map, String, String, java.util.Map, int)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id,
        arg3: payload,
        arg4: 3600
    }]]]></java:args>
</java:invoke-static>
```

---

### updateRecord — merge bins (record must exist)

Use this when you want to update only the bins you supply and leave other bins untouched.  
Throws `KEY_NOT_FOUND` if the record does not exist.  
This is different from `putRecord` (upsert) — `updateRecord` will not create a new record.

Method signature:
```text
updateRecordWithConfig(Map config, String setName, String key, Map bins)
updateRecordWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds)
```

Arguments:
- `arg0` — config map
- `arg1` — set name
- `arg2` — record key
- `arg3` — bins to update; other bins on the record are untouched
- `arg4` — TTL in seconds (only the no-TTL overload uses namespace default)

Mule example — update only the `status` bin:
```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="updateRecordWithConfig(java.util.Map, String, String, java.util.Map)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id,
        arg3: { "status": payload.status }
    }]]]></java:args>
</java:invoke-static>
```

---

### putRecordIfGeneration — optimistic locking (CAS)

Use this when two Mule workers might update the same record at the same time and you must not overwrite each other's changes.

**How it works:**
1. Read the record with `getRecordWithConfig` — the response includes a `generation` number.
2. Modify the bins in your Mule flow.
3. Call `putRecordIfGenerationWithConfig` with the `generation` you read.
4. Aerospike checks if the record's current generation still matches. If someone else wrote the record between your read and write, the generations differ and Aerospike rejects the write with `GENERATION_MISMATCH`.

Method signature:
```text
putRecordIfGenerationWithConfig(Map config, String setName, String key, Map bins, int ttlSeconds, int expectedGeneration)
```

Arguments:
- `arg0` — config map
- `arg1` — set name
- `arg2` — record key
- `arg3` — bins map (full set of bins to write)
- `arg4` — TTL in seconds
- `arg5` — generation value read from the earlier `getRecord` response (`payload.generation`)

Mule example:
```xml
<!-- Step 1: read -->
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecordWithConfig(java.util.Map, String, String)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id
    }]]]></java:args>
</java:invoke-static>
<set-variable variableName="generation" value="#[payload.generation]" />
<set-variable variableName="existingRecord" value="#[payload]" />

<!-- Step 2: conditional write -->
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="putRecordIfGenerationWithConfig(java.util.Map, String, String, java.util.Map, int, int)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id,
        arg3: { "status": "INACTIVE", "updatedAt": now() as String },
        arg4: 0,
        arg5: vars.generation as Number
    }]]]></java:args>
</java:invoke-static>
```

Error handling tip: catch `GENERATION_MISMATCH` and retry from step 1, or return HTTP 409 Conflict.

---

### incrementBins — atomic counter

Use this to increment or decrement integer bins without a read-modify-write cycle.  
Aerospike performs this atomically on the server — no race conditions even if multiple Mule workers call this at the same time.

Common use cases: page view counters, API call counts, inventory adjustments.

Method signature:
```text
incrementBinsWithConfig(Map config, String setName, String key, Map deltas)
incrementBinsWithConfig(Map config, String setName, String key, Map deltas, int ttlSeconds)
```

Arguments:
- `arg0` — config map
- `arg1` — set name
- `arg2` — record key
- `arg3` — deltas map: bin name → amount to add (positive or negative integer). The bin is created starting from the delta value if it does not exist yet.
- `arg4` — TTL in seconds (optional overload)

Mule example — increment `pageViews` by 1 and `apiCalls` by 1:
```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="incrementBinsWithConfig(java.util.Map, String, String, java.util.Map)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: "sessions",
        arg2: vars.sessionId,
        arg3: { "pageViews": 1, "apiCalls": 1 }
    }]]]></java:args>
</java:invoke-static>
```

Mule example — decrement `stock` by the order quantity:
```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="incrementBinsWithConfig(java.util.Map, String, String, java.util.Map)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: "inventory",
        arg2: payload.productId,
        arg3: { "stock": -(payload.quantity as Number) }
    }]]]></java:args>
</java:invoke-static>
```

Response includes the updated bin values so you can check the new count in the same flow step.

---

### touchRecord — reset TTL only

Use this to extend a record's expiry without rewriting any data.  
Throws `KEY_NOT_FOUND` if the record does not exist.

Common use cases: session keep-alive, cache entry refresh.

Method signature:
```text
touchRecordWithConfig(Map config, String setName, String key, int ttlSeconds)
```

Arguments:
- `arg0` — config map
- `arg1` — set name
- `arg2` — record key
- `arg3` — new TTL in seconds (`0` = namespace default, `-1` = never expire)

Mule example — reset session TTL on each API call:
```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="touchRecordWithConfig(java.util.Map, String, String, int)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: "sessions",
        arg2: attributes.headers.sessionId,
        arg3: 1800
    }]]]></java:args>
</java:invoke-static>
```

---

### ping — health check

Use this in a Mule health-check flow or startup validation to confirm Aerospike is reachable.

Method signature:
```text
pingWithConfig(Map config)
```

Arguments:
- `arg0` — config map (only `hosts` and connection properties are needed; namespace is not required)

Response:
```json
{ "connected": true, "nodes": ["BB9040011AC4202", "BB9040011AC4203"] }
```

Mule example:
```xml
<flow name="health-check-flow">
    <http:listener config-ref="httpListenerConfig" path="/health/aerospike" allowedMethods="GET" />
    <flow-ref name="buildAerospikeConfig" />
    <java:invoke-static
        class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
        method="pingWithConfig(java.util.Map)">
        <java:args><![CDATA[#[{ arg0: vars.aerospikeConfig }]]]></java:args>
    </java:invoke-static>
</flow>
```

If `connected` is `false` or the call throws `CONNECTION_FAILED`, Aerospike is not reachable from this Mule instance.

---

Projected read example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecordFieldsWithConfig(java.util.Map, String, String, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: {
            hosts: p('aerospike.hosts'),
            namespace: p('aerospike.namespace'),
            tlsEnabled: p('aerospike.tlsEnabled') as Boolean,
            authEnabled: p('aerospike.authEnabled') as Boolean,
            tlsName: p('aerospike.tlsName') default null,
            username: p('aerospike.username') default null,
            password: p('aerospike.password') default null
        },
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id,
        arg3: ['firstName', 'lastName', 'age', 'status']
    }]]]></java:args>
</java:invoke-static>
```

Batch projected read example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="batchGetFieldsWithConfig(java.util.Map, String, java.util.List, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: ['1001', '1002', '1003'],
        arg3: ['firstName', 'city']
    }]]]></java:args>
</java:invoke-static>
```

Query example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="queryRecordsByFieldEqualsWithConfig(java.util.Map, String, String, Object, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: 'city',
        arg3: 'Mumbai',
        arg4: ['firstName', 'lastName', 'city']
    }]]]></java:args>
</java:invoke-static>
```

Range query example:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="queryRecordsByFieldRangeWithConfig(java.util.Map, String, String, Number, Number, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: vars.aerospikeConfig,
        arg1: p('aerospike.set.customer'),
        arg2: 'age',
        arg3: 25,
        arg4: 40,
        arg5: ['firstName', 'age', 'city']
    }]]]></java:args>
</java:invoke-static>
```

Important:

1. `queryRecordsByFieldEqualsWithConfig` and `queryRecordsByFieldRangeWithConfig` need an Aerospike secondary index on the filtered bin.
2. Equality queries currently support non-blank `String` and integer `Number` values.
3. Range queries currently support integer `Number` values only.

Example AQL index creation for local testing:

```sql
CREATE INDEX idx_customer_city ON test.customer (city) STRING
CREATE INDEX idx_customer_age ON test.customer (age) NUMERIC
```

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

## 4A. Quick Anypoint Studio test setup

Use these repo files:

- sample Mule flows: [full-feature-sample-flow.xml](</C:/Users/dileepkm/OneDrive/ドキュメント/New project/examples/full-feature-sample-flow.xml>)
- local properties: [local-aerospike.properties](</C:/Users/dileepkm/OneDrive/ドキュメント/New project/examples/local-aerospike.properties>)

Recommended Studio steps:

1. Create a new Mule 4.9 project.
2. Add Mule Java Module if it is not already present.
3. Add this library dependency in the Mule app `pom.xml`.
4. Copy `full-feature-sample-flow.xml` contents into a Mule XML config file.
5. Copy `local-aerospike.properties` into `src/main/resources`.
6. Start local Aerospike with Docker.
7. Run the Mule app on port `8081`.
8. Test each endpoint from Postman or curl.

Suggested test order:

1. `POST /customer/101?ttl=3600`
2. `GET /customer/101`
3. `GET /customer/101/fields?fields=name,city`
4. `GET /customer/101/exists`
5. `POST /customer/batch`
6. `POST /customer/batch/fields`
7. `GET /customer/query/city/Pune?fields=name,city`
8. `GET /customer/query/visits?from=1&to=20&fields=name,visits`
9. `DELETE /customer/101`

For query endpoints, create the required Aerospike secondary indexes first.

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
