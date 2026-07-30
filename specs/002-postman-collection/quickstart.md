# Quickstart: Postman API Collection

This guide validates the **Postman collection packaging** feature. It is not an automated
test plan. Platform API shapes live in
[platform OpenAPI](../001-mini-twitter-platform/contracts/openapi.yaml). Scenario parity lives
in [scenario-inventory.md](./contracts/scenario-inventory.md). Artifact fields live in
[data-model.md](./data-model.md).

## Prerequisites

- Docker Compose stack for SocialMedia running (Gateway on `http://localhost:8080`).
- Postman desktop or web (free tier is enough), or a client that imports Collection v2.1.
- Repository files:
  - `postman/SocialMedia.postman_collection.json`
  - `postman/Local.postman_environment.json`
- Optional Runner data files under `postman/data/` if load/rate-limit iterations are used.

Platform one-time setup (keys, `.env`, `docker compose up`) follows the platform guide under
`specs/001-mini-twitter-platform/quickstart.md`. Manual API steps use **this** Postman surface
under `postman/` (the legacy `http/` request-file path is removed).

## Import

1. Open Postman → **Import**.
2. Import `postman/SocialMedia.postman_collection.json`.
3. Import `postman/Local.postman_environment.json`.
4. Select environment **Local** in the environment dropdown.
5. Confirm `gateway` is `http://localhost:8080` (change only if your host mapping differs).

### Required variables (partial import recovery)

If the environment file is missing, create variables:

| Variable | Typical value |
|----------|----------------|
| `gateway` | `http://localhost:8080` |
| `aliceEmail` / `aliceUsername` / `alicePassword` | demo Alice credentials |
| `bobEmail` / `bobUsername` / `bobPassword` | demo Bob credentials |
| Capture slots | `aliceId`, `bobId`, `aliceJwt`, `bobJwt`, post IDs, cursors (leave empty until run) |

## First-time Happy Path (under 15 minutes)

Open folder **`00 Happy Path`** and send requests **top to bottom**:

1. **Register Alice** → expect success; `aliceId` captured.
2. **Register Bob** → expect success; `bobId` captured.
3. **Login Alice** → expect success; `aliceJwt` captured (no manual paste).
4. **Login Bob by email** → expect success; `bobJwt` captured.
5. **Alice follows Bob** → expect success; relationship id captured if scripted.
6. **Bob publishes interaction parent** → expect success; parent post id captured.
7. **Alice home timeline - first page** → poll/send until parent appears (within ~10s is normal).
8. **Alice replies to Bob's post** → expect success; reply id captured.
9. **Poll Bob's notifications** → poll/send until follow and reply notifications appear.

**Human verification**: Read each request description; confirm status and business meaning in
the response panel. Do **not** rely on green/red Postman test results (capture-only scripts).

### Evidence checklist

- [x] Collection + Local environment committed under `postman/`
- [x] No hand-edited JWT or post ID required in later Happy Path bodies (vars + capture scripts)
- [x] Correlation IDs present on primary publish/reply requests
- [x] Scripts contain capture/header helpers only (no `pm.test` assertions)
- [x] Smoke run against local Gateway (2026-07-24): login-first path exercised via scripted HTTP
      equivalent of Happy Path requests (register or login → follow → publish → timeline → reply →
      notifications); see notes below

### Recorded smoke evidence (2026-07-24)

Gateway was healthy on `localhost:8080`. Automated Postman UI was not driven; the same request
shapes as `00 Happy Path` were exercised with curl-equivalent scripting (capture vars between
steps). Expectation: first-time register returns 201 when identities are free; login-first re-run
returns 200 for Alice/Bob logins and proceeds without pasting JWTs when using Postman capture
scripts. Operators should re-confirm once in Postman UI for SC-004 folder discoverability.

## Login-first re-run (under 10 minutes)

When Alice/Bob already exist (retained Compose volumes):

1. Skip register (or expect conflict if re-run).
2. Run **Login Alice** and **Login Bob by email** to refresh JWTs.
3. Continue from follow/publish steps as needed (new posts will create new IDs).

Only rotate `aliceEmail`/`bobEmail` (and usernames) when you **intentionally** re-demo
registration against existing data.

## Domain parity sampling (Story 2)

After Happy Path, spot-check one success and one failure in each:

| Folder | Example failure |
|--------|-----------------|
| `01 Auth & Users` | Reject wrong credentials |
| `02 Posts` | Reject empty post / foreign delete |
| `03 Follows & Timeline` | Reject self-follow / malformed cursor |
| `04 Engagement` | Reject like on deleted parent |
| `05 Notifications` | Cross-user notification denial |

Full list: [scenario-inventory.md](./contracts/scenario-inventory.md).

## Optional: Gateway rate limits (`06 Gateway Limits`)

1. Wait ≥60s with no auth/write traffic from this client if buckets may be empty.
2. Register/login dedicated rate-limit account (or login if exists).
3. Collection Runner on **Auth bucket exhaust probe** with `postman/data/auth-exhaust-11.csv`
   (delay 0); expect some `429` with `X-RateLimit-Remaining: 0`.
4. Immediately try **Auth exhausted register rejected**; confirm profile does not appear.
5. Wait ≥60s; confirm **Auth bucket after refill** reaches domain again (`401` not `429`).
6. Repeat pattern for write bucket with `postman/data/write-exhaust-61.csv` and `rateLimitJwt` +
   `bobId`.

This folder is **not** part of the everyday Happy Path timing budget.

## Optional: Load demos (`07 Optional Load`)

Use Collection Runner + data files only when demonstrating cursor traversal at scale or
1,000-follower fan-out:

| Request | Data file | Runner delay hint |
|---------|-----------|-------------------|
| Seed timeline post | `postman/data/timeline-seed-200.csv` | ~1050 ms |
| Register / login / follow fan-out | `postman/data/fanout-followers-1000.csv` | ~6100 ms on Gateway auth |

For fan-out follows, run **Login fan-out follower** then **Fan-out follow Bob** per data row so
`fanoutJwt` stays valid. Expect long runtime. Skip for ordinary demos.

## Ops-only notes (`08 Ops Notes`)

- **Timeline while Post is down**: `docker compose stop post-service`, send home timeline as
  Alice, observe documented failure/`503` behavior, then `docker compose start post-service`.
- **Internal bulk lookup**: Not on Gateway; set `postService` and use private network only.
- **Kafka DLT / replay** (doc-only):

```bash
# Unknown event type → notification DLT (example; adjust ids as needed)
docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server kafka:9092 --topic post-events.v1 \
  --property parse.key=true --property key.separator='|' <<'EOF'
019f0000-0000-7000-8000-000000000001|{"eventId":"019f0000-0000-7000-8000-000000000002","eventType":"post.unknown.v1","schemaVersion":1,"aggregateId":"019f0000-0000-7000-8000-000000000001","occurredAt":"2026-07-23T00:00:00Z","producer":"post-service","correlationId":"scenario8-notification-dlt","payload":{}}
EOF

docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic post-events.v1.notification-dlt \
  --from-beginning --max-messages 1 --property print.key=true --property print.headers=true
```

For duplicate-safe replay, republish an exact prior `follow.created.v1` or reply-bearing
`post.published.v1` key/value twice without changing `eventId` or payload fields.

## Migration completion checks

- [x] Collection + Local environment committed under `postman/`
- [x] Platform operator docs no longer require `http/socialmedia.http`
- [x] Repository has **no** `http/` directory (after implement delete step)
- [x] Scripts contain capture/header helpers only (no `pm.test` assertions)
- [x] Scenario inventory rows accounted for (request / runner / doc-only / ops-manual)

## What success looks like

A new contributor imports two JSON files, selects **Local**, runs **`00 Happy Path`**, and
completes the product story without JetBrains HTTP Client or manual token copy-paste.
