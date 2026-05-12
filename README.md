# Aerospike Client Library

Plain Java Maven library for calling Aerospike from Mule through Mule Java Module or DataWeave Java static method calls.

## What this project contains

- normal Maven JAR project
- `packaging` = `jar`
- `groupId` = `com.idfcfirstbank`
- `artifactId` = `aerospike-client-lib`
- Aerospike Java client dependency
- Mule-friendly static facade: `AerospikeFunctions`
- Java-friendly instance service: `AerospikeService`
- cached `AerospikeClient` instances per host/auth config
- Basic operations:
  - `getRecord`
  - `putRecord`
  - `deleteRecord`
  - `exists`
  - `batchGet`

This is not a Mule SDK connector and will not appear in the Mule Palette. Mule apps call it through Java Module or DataWeave Java integration.

## Build locally

```bash
mvn clean install
```

This installs the library into your local Maven repository:

```text
~/.m2/repository/com/idfcfirstbank/aerospike-client-lib/1.0.0-SNAPSHOT/
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
    class="com.idfcfirstbank.aerospike.AerospikeFunctions"
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
java!com::idfcfirstbank::aerospike::AerospikeFunctions::getRecord(
    "localhost:3000",
    "test",
    "customer",
    attributes.uriParams.id
)
```

## Java API example

```java
AerospikeConfig config = new AerospikeConfig("localhost:3000");
AerospikeService service = new AerospikeService(config);
Map<String, Object> response = service.getRecord("test", "customer", "123");
```

## Response shape

```json
{
  "success": true,
  "found": true,
  "key": "123",
  "bins": {
    "name": "Dileep"
  },
  "generation": 1,
  "expiration": 0
}
```

## Local Aerospike for testing

```bash
docker run -d --name aerospike \
  -p 3000:3000 \
  -p 3001:3001 \
  -p 3002:3002 \
  -p 3003:3003 \
  aerospike/aerospike-server
```

## Notes before SIT/PROD

Confirm these values with the Mule team before release:

- Exact Mule runtime version
- Java version used by Mule runtime
- Approved Aerospike Java client version
- Artifactory repository and credentials
- Aerospike host, port, namespace, auth, TLS, and firewall access

## Important

Before production, confirm:

- TLS support if Aerospike requires TLS
- Mule flow error mapping for `JAVA:INVOCATION`
- validation for payload/bin types
- logging and masking according to company standards
- whether this plain library should later be wrapped by a Mule SDK connector for Palette support
