# Aerospike Client Library

Plain Java library for calling Aerospike from Mule 4 via the Mule Java Module or DataWeave static method calls. The library manages client connections, type coercion, error mapping, and provides a consistent `Map<String,Object>` response that DataWeave can read directly.

---

## Aerospike concepts for newcomers

| Aerospike term | What it means in practice |
|---|---|
| **Namespace** | Roughly a database. Set in `aerospike.conf`. Usually one per environment (`test`, `prod`). |
| **Set** | Roughly a table. No schema — you define bins when you write a record. |
| **Bin** | Roughly a column. A bin has a name (≤ 14 chars) and a value (string, integer, float, list, map, …). |
| **Key** | The unique identifier for a record within a namespace + set. Always a string here. |
| **TTL** | Time-to-live in seconds. `0` = namespace default, `-1` = never expire, `-2` = keep current TTL. |
| **Generation** | A version counter that Aerospike increments on every write. Used for optimistic locking (CAS). |

---

## Quick start

### 1. Start a local Aerospike server

```bash
docker-compose up -d          # uses the included docker-compose.yml
```

Or run a single container:

```bash
docker run -d --name aerospike -p 3000-3003:3000-3003 aerospike/aerospike-server
```

### 2. Build and install the library

```bash
mvn clean install
```

### 3. Add the dependency to your Mule application's pom.xml

```xml
<dependency>
    <groupId>com.idfcfirstbank</groupId>
    <artifactId>aerospike-client-lib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## Recommended pattern: WithConfig

The `WithConfig` variants accept a config `Map` whose keys match the properties below. This is the best pattern for Mule because you can build the map from `p('...')` property references, keeping all environment-specific values in properties files.

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecordWithConfig(java.util.Map, String, String)">
    <java:args><![CDATA[#[{
        arg0: {
            hosts:     p('aerospike.hosts'),
            namespace: p('aerospike.namespace')
        },
        arg1: "customer",
        arg2: attributes.uriParams.id
    }]]]></java:args>
</java:invoke-static>
```

For TLS + auth (SIT / PROD):

```xml
{
    hosts:              p('aerospike.hosts'),
    namespace:          p('aerospike.namespace'),
    tlsEnabled:         p('aerospike.tlsEnabled') as Boolean,
    tlsName:            p('aerospike.tlsName'),
    trustStorePath:     p('aerospike.trustStorePath'),
    trustStorePassword: p('aerospike.trustStorePassword'),
    user:               p('aerospike.user'),
    password:           p('aerospike.password')
}
```

### Config map keys

| Key | Type | Default | Description |
|---|---|---|---|
| `hosts` | String | required | Comma-separated `host:port` entries, e.g. `as1:3000,as2:3000` |
| `namespace` | String | required | Aerospike namespace |
| `user` | String | — | Username (enables auth automatically) |
| `password` | String | — | Password |
| `authMode` | String | `INTERNAL` | `INTERNAL`, `EXTERNAL`, `EXTERNAL_INSECURE`, `PKI` |
| `tlsEnabled` | Boolean | `false` | Enable TLS |
| `tlsName` | String | — | TLS server name (SNI) |
| `trustStorePath` | String | — | Path to JKS/PKCS12 trust store |
| `trustStorePassword` | String | — | Trust store password |
| `keyStorePath` | String | — | Path to JKS/PKCS12 key store (mTLS) |
| `keyStorePassword` | String | — | Key store password |
| `sendKey` | Boolean | `true` | Store user key with record (enables `key` field in results) |
| `connectTimeout` | int | `0` | Connection timeout in ms (0 = no limit) |
| `readTimeout` | int | `0` | Read timeout in ms |
| `writeTimeout` | int | `0` | Write timeout in ms |
| `maxConnectionsPerNode` | int | `300` | Max concurrent connections per node |

---

## Operation reference

### Read operations

| Method | Description |
|---|---|
| `getRecordWithConfig(config, set, key)` | Read all bins |
| `getRecordFieldsWithConfig(config, set, key, fieldNames)` | Read specific bins only |
| `existsWithConfig(config, set, key)` | Check whether a record exists |
| `batchGetWithConfig(config, set, keys)` | Read multiple records by key |
| `batchGetFieldsWithConfig(config, set, keys, fieldNames)` | Read multiple records, projected |
| `findAllWithConfig(config, set)` | Scan all records in a set |
| `findAllFieldsWithConfig(config, set, fieldNames)` | Scan all records, projected |

### Write operations

| Method | Description |
|---|---|
| `putRecordWithConfig(config, set, key, bins)` | Insert or update (upsert) |
| `putRecordWithConfig(config, set, key, bins, ttlSeconds)` | Upsert with explicit TTL |
| `createRecordWithConfig(config, set, key, bins)` | Insert only — fails if key exists |
| `createRecordWithConfig(config, set, key, bins, ttlSeconds)` | Insert only with TTL |
| `replaceRecordWithConfig(config, set, key, bins, ttlSeconds)` | Replace all bins — fails if key absent |
| `updateRecordWithConfig(config, set, key, bins, ttlSeconds)` | Merge bins — fails if key absent |
| `putRecordIfGenerationWithConfig(config, set, key, bins, ttlSeconds, expectedGeneration)` | Optimistic locking (CAS) write |
| `incrementBinsWithConfig(config, set, key, deltas)` | Atomically add to integer/float bins |
| `incrementBinsWithConfig(config, set, key, deltas, ttlSeconds)` | Atomic increment with TTL reset |
| `touchRecordWithConfig(config, set, key, ttlSeconds)` | Reset TTL without rewriting bins |
| `deleteRecordWithConfig(config, set, key)` | Delete a record |

### Query operations

| Method | Description |
|---|---|
| `queryRecordsByFieldEqualsWithConfig(config, set, field, value, fieldNames)` | Secondary-index equality query |
| `queryRecordsByFieldRangeWithConfig(config, set, field, begin, end, fieldNames)` | Secondary-index range query |
| `queryWithConfig(config, set, criteria, fieldNames)` | Flexible expression-based query |

### Utility

| Method | Description |
|---|---|
| `pingWithConfig(config)` | Health check — returns `{connected, nodes}` |
| `closeAllClients()` | Shutdown hook — close all cached connections |

---

## Response format

Every single-record method returns `Map<String, Object>`:

```json
{
  "namespace": "test",
  "set": "customer",
  "key": "123",
  "operation": "get-record",
  "found": true,
  "bins": {
    "name": "Dileep",
    "age": 30
  }
}
```

List methods return `List<Map<String, Object>>` where each element has the same shape.

The `key` field is the original string key when `sendKey=true` (the default). Without `sendKey`, the key is the record digest in hex.

---

## Error handling

All errors throw `AerospikeOperationException` (a `RuntimeException`). In Mule, catch it with an `on-error-continue` or `on-error-propagate` block:

```xml
<error-handler>
    <on-error-continue
        type="JAVA:RUNTIME"
        when="#[error.cause.class == 'com.idfcfirstbank.aerospike.exception.AerospikeOperationException']">
        <set-variable variableName="errorType"
            value="#[error.cause.errorType as String]"/>
    </on-error-continue>
</error-handler>
```

`AerospikeErrorType` values:

| Error type | When it occurs |
|---|---|
| `TIMEOUT` | Operation exceeded the configured timeout |
| `KEY_NOT_FOUND` | Record does not exist (and the operation requires it) |
| `RECORD_ALREADY_EXISTS` | `createRecord` called on an existing key |
| `GENERATION_MISMATCH` | `putRecordIfGeneration` failed — record was modified concurrently |
| `CONNECTION_FAILED` | Cannot reach any cluster node |
| `AUTHENTICATION_FAILED` | Bad credentials or auth not enabled on server |
| `INVALID_NAMESPACE` | Namespace does not exist in the cluster |
| `WRITE_FAILURE` | Write rejected by server for another reason |
| `DELETE_FAILURE` | Delete rejected by server |
| `VALIDATION_FAILED` | Bad argument caught before the network call |
| `UNKNOWN` | Unexpected server error |

---

## Flexible query (expression filter)

`queryWithConfig` accepts a criteria map instead of a hard-coded field name. Build it in DataWeave:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="queryWithConfig(java.util.Map, String, java.util.Map, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: aerospikeConfig,
        arg1: "customer",
        arg2: {
            match: "AND",
            conditions: [
                { bin: "status",  op: "EQ",      value: "ACTIVE" },
                { bin: "balance", op: "GE",       value: 1000     }
            ],
            index: { bin: "status", op: "EQ", value: "ACTIVE" }
        },
        arg3: ["name", "balance", "status"]
    }]]]></java:args>
</java:invoke-static>
```

Supported operators: `EQ`, `NE`, `GT`, `GE`, `LT`, `LE`, `BETWEEN`, `IN`.

The optional `index` sub-map hints a secondary index to Aerospike so the server pre-filters by index before evaluating the expression (much faster on large sets).

---

## Optimistic locking example

Read the current generation, modify bins, then write only if nobody else changed the record in between:

```xml
<!-- Step 1: read current record -->
<java:invoke-static ... method="getRecordWithConfig(...)">
    ...
</java:invoke-static>
<set-variable variableName="generation" value="#[payload.generation]"/>

<!-- Step 2: conditional write -->
<java:invoke-static ... method="putRecordIfGenerationWithConfig(java.util.Map, String, String, java.util.Map, int, int)">
    <java:args><![CDATA[#[{
        arg0: aerospikeConfig,
        arg1: "customer",
        arg2: attributes.uriParams.id,
        arg3: { "status": "INACTIVE" },
        arg4: 0,
        arg5: vars.generation as Number
    }]]]></java:args>
</java:invoke-static>
```

If the record was modified between step 1 and step 2, `AerospikeErrorType.GENERATION_MISMATCH` is thrown.

---

## Atomic counter example

Increment a `pageViews` counter without a read-modify-write cycle:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="incrementBinsWithConfig(java.util.Map, String, String, java.util.Map)">
    <java:args><![CDATA[#[{
        arg0: aerospikeConfig,
        arg1: "sessions",
        arg2: vars.sessionId,
        arg3: { "pageViews": 1, "apiCalls": 1 }
    }]]]></java:args>
</java:invoke-static>
```

---

## Health check

Call `pingWithConfig` in a Mule health-check flow to verify cluster connectivity:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="pingWithConfig(java.util.Map)">
    <java:args><![CDATA[#[{ arg0: aerospikeConfig }]]]></java:args>
</java:invoke-static>
```

Response:

```json
{ "connected": true, "nodes": ["BB9040011AC4202", "BB9040011AC4203"] }
```

---

## DataWeave static call example

```dw
%dw 2.0
output application/json
---
java!com::idfcfirstbank::aerospike::api::AerospikeFunctions::getRecord(
    "localhost:3000",
    "test",
    "customer",
    attributes.uriParams.id
)
```

---

## Package structure

```
com.idfcfirstbank.aerospike
├── api           AerospikeFunctions — static Mule-friendly facade
├── config        AerospikeConfig — connection + policy settings
├── service       AerospikeClientProvider, AerospikeRecordService, AerospikeQuerySupport
├── exception     AerospikeOperationException, AerospikeErrorType, AerospikeExceptionMapper
├── model         AerospikeResponse — response builder
└── util          AerospikeValidation — shared argument checks
```

---

## TTL sentinel values

| Value | Meaning |
|---|---|
| `0` | Use the namespace `default-ttl` setting |
| `-1` | Never expire |
| `-2` | Keep the record's current TTL (touch-only semantics) |
| `> 0` | Expire after this many seconds |

Values below `-2` are rejected with `VALIDATION_FAILED`.
