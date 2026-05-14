# Mule Developer Guide for `aerospike-client-lib`

This guide explains how a Mule developer should use this library in local, DEV, SIT, and PROD environments.

This project is a plain Java JAR, not a Mule SDK connector. Mule applications use it through the Mule Java Module or through DataWeave static Java calls.

## 1. What Mule developers need to know

- Artifact:
  `com.idfcfirstbank:aerospike-client-lib:1.0.0-SNAPSHOT`
- Java target:
  Java 17, aligned with Mule 4.9 running on Java 17
- Main Mule entrypoint class:
  `com.idfcfirstbank.aerospike.api.AerospikeFunctions`
- Current supported security modes:
  - no authentication
  - username/password authentication
- Current unsupported security modes in code:
  - TLS / SSL
  - mTLS / PKI authentication
  - truststore / keystore driven client TLS

So for plain TCP Aerospike or username/password Aerospike, the library is ready.

If your Aerospike cluster uses TLS, you must enhance the code before using it in SIT or PROD. The required code changes are documented in section 9.

The project uses `aerospike-client-jdk8` even on Java 17. Aerospike's Java client documentation says `aerospike-client-jdk8` is the package for runtimes from JDK 8 up to before JDK 21. The `aerospike-client-jdk21` package is for JDK 21 or later.

## 2. How Mule should include the library

Add this dependency in the Mule application `pom.xml`:

```xml
<dependency>
    <groupId>com.idfcfirstbank</groupId>
    <artifactId>aerospike-client-lib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

After adding the dependency:

1. run Maven update in the Mule app
2. confirm Mule Java Module is available in the project
3. call the static methods from `AerospikeFunctions`

## 3. Recommended property model for Mule environments

Use Mule property files or secure property files instead of hardcoding Aerospike values in XML.

Suggested Mule properties:

```properties
aerospike.hosts=localhost:3000
aerospike.namespace=test
aerospike.set.customer=customer
aerospike.username=
aerospike.password=
```

Recommended files:

- `config-local.properties`
- `config-dev.properties`
- `config-sit.properties`
- `config-prod.properties`

For secured environments, keep password in secure properties or secret manager, not in plain text.

## 4. Local Docker Compose usage

This repo already includes `docker-compose.yml`.

Start local Aerospike:

```bash
docker compose up -d
```

Local values for Mule:

```properties
aerospike.hosts=localhost:3000
aerospike.namespace=test
aerospike.set.customer=customer
```

Example Mule Java Module call:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecord(String, String, String, String)">
    <java:args><![CDATA[#[{
        arg0: p('aerospike.hosts'),
        arg1: p('aerospike.namespace'),
        arg2: p('aerospike.set.customer'),
        arg3: attributes.uriParams.id
    }]]]></java:args>
</java:invoke-static>
```

For local Docker Compose, the important point is:

- from your laptop or local Mule runtime, use `localhost:3000`
- do not use Docker container IP directly

## 5. DEV / SIT / PROD usage without TLS

If Aerospike is reachable over normal TCP and does not require TLS, only the host list changes by environment.

Example:

```properties
# DEV
aerospike.hosts=dev-aerospike-01.company.net:3000,dev-aerospike-02.company.net:3000

# SIT
aerospike.hosts=sit-aerospike-01.company.net:3000,sit-aerospike-02.company.net:3000

# PROD
aerospike.hosts=prod-aerospike-01.company.net:3000,prod-aerospike-02.company.net:3000
```

If authentication is enabled:

```xml
<java:invoke-static
    class="com.idfcfirstbank.aerospike.api.AerospikeFunctions"
    method="getRecordWithAuth(String, String, String, String, String, String)">
    <java:args><![CDATA[#[{
        arg0: p('aerospike.hosts'),
        arg1: p('aerospike.username'),
        arg2: p('aerospike.password'),
        arg3: p('aerospike.namespace'),
        arg4: p('aerospike.set.customer'),
        arg5: attributes.uriParams.id
    }]]]></java:args>
</java:invoke-static>
```

Recommended runtime behavior:

- keep namespace in properties
- keep set names in properties when business-specific
- keep username/password in secure config
- pass record key and bins dynamically from Mule flow

## 6. If Aerospike is deployed in EKS

This is the most important deployment point for your case.

The Mule application must use Aerospike seed addresses that are reachable from the Mule runtime network. Do not use pod IPs unless Mule itself runs inside the same Kubernetes network and those pod IPs are routable from Mule.

Typical options:

1. Mule runtime is outside EKS but inside the same VPC or connected network
   Use an internal load balancer, internal DNS, or host-internal access address exposed for Aerospike.

2. Mule runtime is outside EKS and must connect from another network
   Use an approved external or private endpoint exposed by the platform team. This usually means an internal NLB, private link pattern, VPN-connected endpoint, or another controlled ingress design.

3. Mule runtime runs inside the same EKS cluster or same Kubernetes network
   You may be able to use Kubernetes service DNS or the access addresses configured by the Aerospike Kubernetes setup.

Recommended values for EKS-based Aerospike:

```properties
aerospike.hosts=internal-aerospike-seed.company.vpc:3000
```

or:

```properties
aerospike.hosts=seed1.company.vpc:3000,seed2.company.vpc:3000
```

What to confirm with the EKS / Aerospike platform team:

1. Which hostname should client applications use
2. Whether access is by internal load balancer, node address mapping, or service DNS
3. Whether the client port is `3000` or `4333`
4. Whether TLS is mandatory
5. Whether authentication is enabled
6. Whether the Mule runtime can route to that endpoint

Aerospike Kubernetes Operator documentation describes client access patterns such as `hostInternal`, `hostExternal`, `configuredIP`, and load balancer based seed finder services. That is why the exact `aerospike.hosts` value must come from the cluster networking design, not guessed from pod names.

## 7. Recommended environment examples

### Local

```properties
aerospike.hosts=localhost:3000
aerospike.namespace=test
aerospike.set.customer=customer
```

### DEV

```properties
aerospike.hosts=dev-aerospike.company.vpc:3000
aerospike.namespace=dev_ns
aerospike.set.customer=customer
aerospike.username=${secure::aerospike.username}
aerospike.password=${secure::aerospike.password}
```

### SIT

```properties
aerospike.hosts=sit-aerospike.company.vpc:3000
aerospike.namespace=sit_ns
aerospike.set.customer=customer
aerospike.username=${secure::aerospike.username}
aerospike.password=${secure::aerospike.password}
```

### PROD

```properties
aerospike.hosts=prod-aerospike.company.vpc:3000
aerospike.namespace=prod_ns
aerospike.set.customer=customer
aerospike.username=${secure::aerospike.username}
aerospike.password=${secure::aerospike.password}
```

## 8. Current library behavior and limitations

Current code behavior:

- caches one `AerospikeClient` per host/auth configuration
- supports multiple seed hosts in comma-separated format
- supports static Java calls from Mule Java Module or DataWeave
- supports:
  - `getRecord`
  - `putRecord`
  - `deleteRecord`
  - `exists`
  - `batchGet`

Current limitations:

- no TLS configuration in API
- no truststore path / truststore password handling
- no keystore path / keystore password handling
- no PKI auth mode
- no explicit connection timeout property exposed to Mule
- no per-environment client tuning properties exposed yet

This means:

- local Docker and plain DEV access are straightforward
- secured SIT / PROD may require library enhancement first

## 9. If Aerospike uses SSL / TLS / mTLS, what must change

Official Aerospike Java client guidance says TLS connections require:

- TLS enabled in client policy
- the host TLS name
- trust material for server certificate validation
- client key material when mutual TLS is required

For Aerospike, TLS commonly uses port `4333` instead of non-TLS port `3000`.

This library does not yet expose that configuration. To support TLS, change these files:

### `src/main/java/com/idfcfirstbank/aerospike/config/AerospikeConfig.java`

Add fields such as:

- `tlsEnabled`
- `tlsName`
- `trustStorePath`
- `trustStorePassword`
- `keyStorePath`
- `keyStorePassword`
- `authMode`

### `src/main/java/com/idfcfirstbank/aerospike/service/AerospikeClientProvider.java`

Enhance client creation to:

1. create and attach `TlsPolicy`
2. create `Host` with TLS name when required
3. set `ClientPolicy.tlsPolicy`
4. set client auth mode if PKI / mTLS is required
5. load truststore and keystore from JVM or explicitly configured runtime values

### `src/main/java/com/idfcfirstbank/aerospike/api/AerospikeFunctions.java`

Add overloaded methods that accept TLS/security parameters, or move to a richer config object pattern if the argument list becomes too large.

Recommended direction:

- for non-TLS environments, keep the current simple methods
- for TLS environments, add new methods like `getRecordSecure(...)` or move Mule to create a config map/object and call an instance service

### Mule-side property additions for TLS

Example future property set:

```properties
aerospike.hosts=prod-aerospike.company.vpc:4333
aerospike.tls.enabled=true
aerospike.tls.name=prod-aerospike.company.vpc
aerospike.truststore.path=/opt/mule/certs/aerospike-truststore.jks
aerospike.truststore.password=${secure::aerospike.truststore.password}
aerospike.keystore.path=/opt/mule/certs/aerospike-keystore.jks
aerospike.keystore.password=${secure::aerospike.keystore.password}
```

If server uses one-way TLS only:

- truststore is needed
- client keystore may not be needed

If server uses mutual TLS:

- truststore is needed
- client keystore is needed
- PKI authentication requirements must be confirmed with Aerospike team

## 10. Security checklist for SIT / PROD

Before promoting Mule usage into secured environments, confirm all of these:

1. Is Aerospike security enabled
2. Is username/password required
3. Is TLS required
4. Is mTLS required
5. What is the client port: `3000` or `4333`
6. What DNS name should the client use
7. What certificate CN or SAN must match the Aerospike TLS name
8. Where will Mule runtime load truststore and keystore from
9. Are credentials stored in secure properties / secrets manager
10. Can the Mule runtime network route to the Aerospike endpoint

## 11. Suggested Mule team usage pattern

Recommended pattern for Mule developers:

1. keep one shared configuration per environment in properties
2. store secrets outside source code
3. use Java Module `invoke-static`
4. map Java exceptions in Mule error handling
5. start with local Docker validation
6. move next to DEV
7. only then finalize SIT / PROD security settings

## 12. Practical recommendation for your EKS deployment

Because your Aerospike is deployed in EKS, the next step should be:

1. get the exact client endpoint from the Aerospike / EKS platform team
2. confirm whether client traffic is plain TCP or TLS
3. confirm whether Mule runs in the same VPC / cluster / peered network
4. if plain TCP:
   use current library with the approved seed endpoint
5. if TLS:
   enhance this library first before SIT / PROD use

## 13. Official references

- Mule Java Module invoke methods:
  [MuleSoft Java Module](https://docs.mulesoft.com/java-module/1.2/java-invoke-method)
- Aerospike Java client connection guide:
  [Aerospike Java Client Connecting](https://aerospike.com/docs/develop/client/java/connect/)
- Aerospike TLS overview:
  [Aerospike TLS Security](https://aerospike.com/docs/database/learn/security/tls)
- Aerospike TLS configuration:
  [Aerospike TLS Configuration](https://aerospike.com/docs/database/manage/network/tls/)
- Aerospike RBAC and access control:
  [Aerospike Access Control](https://aerospike.com/docs/database/manage/security/rbac/)
- Aerospike Kubernetes Operator configuration reference:
  [Aerospike Kubernetes Config Reference](https://aerospike.com/docs/kubernetes/reference/config-reference/)
