# Spring Boot verification module

This module contains executable Java domain, persistence, API, and Kafka-consumer tests alongside the Vercel app. It is a focused verification backend, not the production Eureka/API Gateway microservice topology.

Run from this directory with Java 21 and Maven installed. In the current workspace, the downloaded local toolchain is under `../work/tools`:

```sh
export JAVA_HOME="../work/tools/jdk-21.0.13+9"
"../work/tools/apache-maven-3.9.9/bin/mvn" test
```

On Windows PowerShell from the repository root:

```powershell
$env:JAVA_HOME = (Get-ChildItem work/tools -Directory | Where-Object Name -Like 'jdk-*' | Select-Object -First 1).FullName
& 'work/tools/apache-maven-3.9.9/bin/mvn.cmd' -f backend/pom.xml test
```

Coverage:

- `AllocationEngineTest` — JUnit 5 + AssertJ tests for the one-paisa case, exact reconciliation, multiple residual paise, and stable tie-breaking.
- `ChargebackWorkflowSpringBootTest` — `@SpringBootTest` + MockMvc + embedded Kafka. It rejects unauthenticated calculation, authenticates an officer, calculates, finalizes, publishes `ChargebackFinalized`, and waits for the consumer to persist workflow state.
- `ChargebackJpaTest` — `@DataJpaTest` verifies persistence and a unique event ID constraint.
- `ChargebackContractTest` — checks the REST response and event schema fields expected by a consumer.

Integration-test demo credentials are in `SecurityConfig`; do not use them outside tests. The main Vercel deployment uses the independent Vercel API implementation and HttpOnly JWT sessions documented in the root README.


## Live local Gateway demo

For a synthetic full-stack demonstration, package the core and Gateway, start the core with WEALTHFLOW_EMBEDDED_KAFKA=true on port 8082, then run the Gateway on port 8180 with CHARGEBACK_SERVICE_URL=http://localhost:8082. The embedded broker mode is demo-only. The verified run is documented in ../outputs/demo-evidence.md.
