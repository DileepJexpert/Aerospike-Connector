# Aerospike Mule Connector

This is a starter Mule 4 custom connector project for Aerospike, aligned with the company-style `json-logger` plugin pattern shared in the screenshots.

## What this project contains

- Mule SDK / Mule extension style Maven project
- `packaging` = `mule-extension`
- `groupId` = `com.idfcfirstbank`
- `artifactId` = `aerospike-connector`
- `mule-artifact.json` with `minMuleVersion` = `4.3.0`
- Aerospike Java client dependency
- Basic connector operations:
  - `get-record`
  - `put-record`
  - `delete-record`
  - `exists`

## Build locally

```bash
mvn clean install
```

This installs the connector into your local Maven repository:

```text
~/.m2/repository/com/idfcfirstbank/aerospike-connector/1.0.0-SNAPSHOT/
```

## Use in a Mule application

Add this dependency to the Mule application's `pom.xml`:

```xml
<dependency>
    <groupId>com.idfcfirstbank</groupId>
    <artifactId>aerospike-connector</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

Then in Anypoint Studio:

```text
Right click Mule project → Maven → Update Project
```

or:

```text
Right click Mule project → Mule → Update Project Dependencies
```

Restart Studio if the connector does not appear in the Mule Palette.

## Local Aerospike for testing

```bash
docker run -d --name aerospike \
  -p 3000:3000 \
  -p 3001:3001 \
  -p 3002:3002 \
  -p 3003:3003 \
  aerospike/aerospike-server
```

## Example connector configuration in Mule XML

```xml
<aerospike:config name="Aerospike_Config">
    <aerospike:basic hosts="localhost:3000" defaultPort="3000" />
</aerospike:config>
```

## Example operation usage

```xml
<aerospike:get-record
    config-ref="Aerospike_Config"
    namespace="test"
    setName="customer"
    key="#[attributes.uriParams.id]" />
```

## Notes before using in SIT/PROD

Confirm these values with the Mule team before release:

- Exact Mule runtime version
- Java version used by Mule runtime
- Approved Aerospike Java client version
- Whether `mule-modules-parent` should remain `1.6.0` or be upgraded
- Artifactory repository and credentials
- Aerospike host, port, namespace, auth, TLS, and firewall access

## Important

This is a starter POC project. Before production, add:

- TLS support if Aerospike requires TLS
- custom error mapping such as `AEROSPIKE:TIMEOUT`, `AEROSPIKE:CONNECTION`, etc.
- MUnit tests
- validation for payload/bin types
- logging and masking according to company standards
- batch operations if required
