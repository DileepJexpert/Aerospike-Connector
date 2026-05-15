# Aerospike Client Library

Plain Java Maven library for calling Aerospike from Mule through Mule Java Module or DataWeave static method calls.

## Package structure

This repo stays as one Java JAR, but the code is organized in a cleaner package layout:

- `com.idfcfirstbank.aerospike.api`
  static Mule-friendly facade methods
- `com.idfcfirstbank.aerospike.config`
  connection configuration objects
- `com.idfcfirstbank.aerospike.service`
  client lifecycle and record operations
- `com.idfcfirstbank.aerospike.exception`
  error types and exception mapping
- `com.idfcfirstbank.aerospike.model`
  response helpers
- `com.idfcfirstbank.aerospike.util`
  shared validation helpers

## What this project contains

- normal Maven JAR project
- `packaging` = `jar`
- `groupId` = `com.idfcfirstbank`
- `artifactId` = `aerospike-client-lib`
- Java 17 target, aligned with Mule 4.9 on Java 17
- Aerospike Java client dependency
- static facade for Mule and DataWeave: `AerospikeFunctions`
- internal client cache for reusing `AerospikeClient`
- config-map methods for local, DEV, SIT, and PROD environment properties
- TLS, mTLS, auth, timeout, and connection-limit properties
- basic operations:
  - `getRecord`
  - `getRecordFields`
  - `putRecord`
  - `deleteRecord`
  - `exists`
  - `batchGet`
  - `batchGetFields`
  - `queryRecordsByFieldEquals`
  - `queryRecordsByFieldRange`

In Aerospike terms, what Mule teams often call a "table" is a set, and what they call "columns" are bins.
The new projected read methods let Mule fetch only the bins it needs from a record.

## Build locally

```bash
mvn clean test
mvn clean install
```

## Use in a Mule application

Add this dependency to the Mule application's `pom.xml`:

```xml
<dependency>
    <groupId>com.idfcfirstbank</groupId>
    <artifactId>aerospike-client-lib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Mule Java Module example

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecordFieldsWithConfig(java.util.Map, String, String, java.util.List)">
    <java:args><![CDATA[#[{
        arg0: {
            hosts: p('aerospike.hosts'),
            namespace: p('aerospike.namespace'),
            tlsEnabled: p('aerospike.tlsEnabled') as Boolean,
            authEnabled: p('aerospike.authEnabled') as Boolean
        },
        arg1: p('aerospike.set.customer'),
        arg2: attributes.uriParams.id,
        arg3: ['name', 'city', 'status']
    }]]]></java:args>
</java:invoke-static>
```

## Query examples

Primary-key reads are the fastest path. For indexed reads on non-key bins, use:

- `queryRecordsByFieldEqualsWithConfig(Map config, String setName, String fieldName, Object fieldValue, List fieldNames)`
- `queryRecordsByFieldRangeWithConfig(Map config, String setName, String fieldName, Number rangeBegin, Number rangeEnd, List fieldNames)`

These query methods require a matching Aerospike secondary index on the filtered bin.

## DataWeave static method example

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

## Local Aerospike for testing

Use the included [docker-compose.yml](</C:/Users/dileepkm/OneDrive/ドキュメント/New project/docker-compose.yml>) or run:

```bash
docker run -d --name aerospike \
  -p 3000:3000 \
  -p 3001:3001 \
  -p 3002:3002 \
  -p 3003:3003 \
  aerospike/aerospike-server
```

## Local smoke test

The repo includes [LocalSmokeTest.java](</C:/Users/dileepkm/OneDrive/ドキュメント/New project/src/test/java/com/idfcfirstbank/aerospike/smoke/LocalSmokeTest.java>) for direct Aerospike verification.
