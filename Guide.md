# Wealthflow SOA / Microservices Guide

> The original course Guide containing the B1–B6 wording was not present in the project files. This guide maps B1–B6 to the requirements supplied in the project request and records the implementation boundary accurately.

## B1 — Domain services and contracts

The bounded domains are **UsageMeter**, **RateCard**, and **Chargeback**. Each owns its REST contract and data: usage intake and reads; effective-dated rate versions; and high-precision charge calculation, reconciliation, and finalization. A consumer should depend on the published REST/event contract, never another service's tables.

The Vercel application has separate API route groups and supports distinct `USAGE_DATABASE_URL`, `RATE_DATABASE_URL`, `CHARGEBACK_DATABASE_URL`, and `WORKFLOW_DATABASE_URL` settings. Those routes run in the same Vercel project; they are not independently deployed Spring Boot services. The Java verification app is a single Spring Boot process.

## B2 — Kafka ownership and workflow

| Responsibility | Owner | Contract |
| --- | --- | --- |
| Produce | Chargeback domain, after officer-authorized finalization | `ChargebackFinalized` on `soa.s08.t24.events` |
| Consume | Separate Workflow worker / consumer group | Validate event type and owner, then upsert workflow state |
| Persist | Workflow domain only | `WORKFLOW_DATABASE_URL`, keyed by chargeback ID and unique event ID |

The event contains `eventId`, `eventType`, `chargebackId`, `ownerId`, `totalBilledAmount`, `finalAllocatedAmount`, `rateVersion`, `finalizedAt`, and allocation lines. The worker persists the owner with the workflow state so record access remains scoped after consumption. Duplicate delivery updates the same workflow row.

## B3 — JWT roles and record access

JWTs are signed with `JWT_SECRET` and identify the authenticated user. The API accepts the roles `labUser` and `billingOfficer`.

- `labUser` can submit usage, calculate charges, and read only their own usage, chargeback, and workflow records.
- `billingOfficer` can review usage, manage rate versions, and finalize chargebacks.
- Workflow lookups verify the chargeback owner and the persisted workflow owner before returning a record. Unauthenticated requests receive 401; authenticated users outside the owner/role boundary receive 403.

Never put signing secrets in source control or return them to the browser. Production browser sessions use HttpOnly cookies.

## B4 — Eureka registration and Gateway routing

The intended service topology is:

```text
Client → API Gateway → Eureka-discovered UsageMeter / RateCard / Chargeback
Chargeback → Kafka topic soa.s08.t24.events → Workflow consumer
```

The current Gateway module has a `/api/**` route to a configured `CHARGEBACK_SERVICE_URL`. It is a static destination, not a Eureka-discovered route. The current Spring projects do not include a Eureka server or Eureka clients, so registration and discovery remain an implementation gap for the Spring deployment. The landing-page architecture illustration describes the target topology; it is not evidence of a running registry.

## B5 — Separate JPA stores and ownership

Each independently deployed domain service must use its own datasource and schema. The Vercel deployment supports separate database URLs per domain; configure distinct databases in the deployment environment. The Java verification module currently uses one H2 datasource for both chargeback and workflow entities and must not be treated as proof of separate JPA stores. Do not share tables or repositories across domain boundaries in the production service topology.

## B6 — Verification and delivery evidence

The Java module contains:

- JUnit 5 + AssertJ domain tests for deterministic residual-paisa allocation and exact total reconciliation.
- `@SpringBootTest` workflow integration coverage for authenticated calculation, role-denied finalization, Kafka publication/consumption, and persisted workflow state.
- `@DataJpaTest` checks for chargeback persistence and unique event IDs.
- Contract tests for `ChargebackFinalized` fields and the calculate REST response.

These tests validate the single-process Spring verification module, including a denied cross-owner workflow read. Add service-to-service tests for Eureka registration, routed Gateway calls, and distinct JPA datasources before describing the full Spring microservice topology as passing.

## CO mapping

| Course outcome | Wealthflow evidence |
| --- | --- |
| **CO1 — Service contracts + Kafka workflow** | Domain REST contracts; Chargeback owns `ChargebackFinalized`; a separate worker consumes and persists workflow state. Contract tests check event and REST fields. |
| **CO2 — JWT roles + authorization** | HS256 JWT authentication; `labUser` and `billingOfficer` policy; owner-scoped record access and role-restricted rate/finalize operations. |
| **CO3 — Eureka + Gateway + per-service JPA** | Gateway route configuration and separate database URL settings exist. Eureka registration/discovery and isolated Spring JPA datasources are still required in the Spring deployment. |
| **CO4 — JUnit + integration + contract tests** | JUnit 5 + AssertJ domain tests, Spring Boot Kafka workflow integration test, Data JPA constraint test, and REST/event contract test. |

## Run the Java verification suite

From the repository root on Windows PowerShell:

```powershell
$env:JAVA_HOME = (Get-ChildItem work/tools -Directory | Where-Object Name -Like 'jdk-*' | Select-Object -First 1).FullName
& 'work/tools/apache-maven-3.9.9/bin/mvn.cmd' -f backend/pom.xml test
```

For API environment setup and the local Kafka workflow, see [README.md](README.md) and [backend/README.md](backend/README.md).
