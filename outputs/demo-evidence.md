# Wealthflow Gateway workflow demo

Executed locally on 7 November 2026 through the Spring Cloud Gateway at `http://localhost:8180`. The Gateway routed `/api/**` to the Spring chargeback API on port `8082`. The API used H2 and an embedded Kafka KRaft broker for this local demo. All inputs are synthetic.

## Calculation

- Chargeback: `368f4567-678a-41d9-912d-2d247198011a`
- Billed total: ₹100.00
- Exact shares: A ₹33.333, B ₹33.333, C ₹33.334
- Rounded down: A ₹33.33, B ₹33.33, C ₹33.33
- Residual: ₹0.01, assigned to C by largest fractional remainder
- Final allocation: A ₹33.33 + B ₹33.33 + C ₹33.34 = **₹100.00**
- Reconciliation variance: ₹0.00

## Kafka event published and consumed

Topic: `soa.s08.t24.events`

```json
{
  "eventId": "1cc10fe4-79fc-478d-a4c8-e9d2f47343a7",
  "eventType": "ChargebackFinalized",
  "chargebackId": "368f4567-678a-41d9-912d-2d247198011a",
  "totalBilledAmount": 100.00,
  "finalAllocatedAmount": 100.00,
  "rateVersion": "DEMO",
  "finalizedAt": "2026-11-06T18:44:40.151146Z"
}
```

The workflow consumer persisted this event as `FINALIZED`; the workflow query returned the same event ID, billed amount, and allocated amount.

## JWT authorization

- Signed `labUser` JWT: calculation allowed; attempt to finalize received **HTTP 403**.
- Signed `billingOfficer` JWT: finalization allowed; event published and consumed.

## Tests

- Maven: 6 tests passed, 0 failures/errors (domain, SpringBootTest + Kafka, DataJpaTest, and contract).
- Node: 3 tests passed, 0 failures.

The local demo uses a fixed development JWT key and embedded H2/Kafka. Do not use those settings for deployment.