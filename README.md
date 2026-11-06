# Wealthflow

See [Guide.md](Guide.md) for the B1–B6 SOA requirements, producer/consumer ownership, JWT record-access rules, and CO1–CO4 evidence map. The guide marks the Spring Eureka registration and isolated JPA datasources as outstanding rather than describing the target diagram as a deployed topology.

Wealthflow keeps the compact three-module landing page and adds a Vercel deployment with serverless API routes, PostgreSQL persistence, HttpOnly JWT sessions, and a backend chargeback calculator using integer paise (`BigInt`). The `backend/workflow-worker.mjs` process consumes the real Kafka event and persists workflow state.

## Vercel deployment

1. Create a Vercel project from this repository root. `vercel.json` serves `outputs/` and maps `/workspace` to the authenticated workspace page.
2. Create PostgreSQL databases (Neon works well) and set `AUTH_DATABASE_URL`, `USAGE_DATABASE_URL`, `RATE_DATABASE_URL`, `CHARGEBACK_DATABASE_URL`, and `WORKFLOW_DATABASE_URL` in Vercel Project Settings → Environment Variables. One `DATABASE_URL` can be used for a local demonstration only.
3. Set a random `JWT_SECRET` of at least 32 characters. Never commit credentials.
4. Locally, run `npm install`, set the environment variables from `.env.example`, then run `npm run db:setup` and `npm run db:seed`.
5. Seed accounts are synthetic and configured only from environment variables. Use `npm run dev` for Vercel's local runtime. Deploy with `npx vercel --prod` after linking the Vercel project.

The browser receives an HttpOnly, Secure, SameSite=Lax JWT cookie; the JWT is not stored in localStorage. `labUser` records are owner-scoped, and rate changes and finalization require `billingOfficer`.

## Kafka and workflow status

Vercel Functions cannot host a Kafka broker or a persistent consumer. Configure `KAFKA_BROKERS` to a reachable managed Kafka cluster and run `npm run worker` as a separate always-on worker with `WORKFLOW_DATABASE_URL` and the same topic. The Chargeback API publishes `ChargebackFinalized` to `soa.s08.t24.events`; only the worker writes the consumed workflow state. Without Kafka configured, finalization returns `KAFKA_NOT_CONFIGURED` rather than simulating success.

For the full Spring Boot/Eureka/API Gateway deployment from the original requirements, deploy the long-running Java services and Kafka worker on a container platform; use Vercel for the frontend and serverless edge API only. This repository's Vercel implementation provides the working student-project path without claiming that Vercel runs Eureka or Spring services.

## API summary

- `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`
- `POST /api/usage`, `GET /api/usage`
- `POST /api/rates` (billing officer), `GET /api/rates`
- `POST /api/chargebacks/calculate`, `POST /api/chargebacks/:id/finalize`, `GET /api/chargebacks`, `GET /api/chargebacks/:id`
- `GET /api/workflows/:chargebackId`

Calculation input uses `{ "totalAmount":"100.00", "allocations":[{"id":"A","share":"33333"},{"id":"B","share":"33333"},{"id":"C","share":"33334"}] }`. Shares can be decimal weights. Amounts are parsed directly into integer paise; remainders sort descending, with recipient ID as a stable tie-breaker. `roundedAmount` is the floor and `finalAmount` includes the allocated residual.

## Checks

Use `npm run db:setup` and `npm run db:seed` before testing sign-in locally. For a production deployment, configure all five database URLs plus Kafka and keep the workflow worker running outside Vercel.

The Java/JUnit verification module is in `backend/`; it requires Java 21 and Maven. See [backend/README.md](backend/README.md) for the JUnit 5 + AssertJ, SpringBootTest, DataJpaTest, and contract test coverage. This focused module exercises the secured Spring API and embedded Kafka consumer; it is not a Spring Cloud Gateway/Eureka deployment.
