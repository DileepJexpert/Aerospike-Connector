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
- Aerospike Java client dependency
- static facade for Mule and DataWeave: `AerospikeFunctions`
- internal client cache for reusing `AerospikeClient`
- basic operations:
  - `getRecord`
  - `putRecord`
  - `deleteRecord`
  - `exists`
  - `batchGet`

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
    method="getRecord(String, String, String, String)">
    <java:args><![CDATA[#[{
        arg0: "localhost:3000",
        arg1: "test",
        arg2: "customer",
        arg3: attributes.uriParams.id
    }]]]></java:args>
</java:invoke-static>
```

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
