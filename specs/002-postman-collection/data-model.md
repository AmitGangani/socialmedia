# Data Model: Postman API Collection Artifacts

**Feature**: `002-postman-collection` | **Date**: 2026-07-24

This is not a service persistence model. It describes the version-controlled verification
artifacts and their variables.

## Entities

### API Collection

| Field | Description |
|-------|-------------|
| `info.name` | `SocialMedia` (or equivalent stable display name) |
| `info.schema` | Postman Collection v2.1 schema URL |
| `info.description` | Import notes, required environment variables, first-time vs login-first re-run |
| `item[]` | Folders and requests (see Folder) |
| Scripts | Folder/request event scripts limited to capture and header helpers |

**Validation / rules**:

- No `pm.test` / assertion libraries in any script.
- Single platform-wide collection (not one per service).
- Hand-curated; not generated as primary from OpenAPI.

### Capture-script convention

Post-response (Tests tab) scripts may only:

```javascript
// Capture-only: no pm.test assertions
try {
  var json = pm.response.json();
  if (json.accessToken) pm.environment.set("aliceJwt", json.accessToken);
  if (json.id) pm.environment.set("aliceId", json.id);
} catch (e) { /* leave env unchanged */ }
```

Pre-request scripts may only set dynamic helpers (e.g. emoji text of length 280, correlation
suffixes, rate-limit reject usernames). Never call `pm.test` or `pm.expect`.

### Folder

| Field | Description |
|-------|-------------|
| `name` | Numbered prefix + title (e.g., `00 Happy Path`) |
| `description` | When to run; first-time vs re-run; expected business outcomes |
| `item[]` | Ordered child requests or nested groups |

**Canonical folders** (research §4):

1. `00 Happy Path`
2. `01 Auth & Users`
3. `02 Posts`
4. `03 Follows & Timeline`
5. `04 Engagement`
6. `05 Notifications`
7. `06 Gateway Limits` (optional everyday)
8. `07 Optional Load` (optional everyday)
9. `08 Ops Notes` (compose/internal/Kafka pointers)

### Request

| Field | Description |
|-------|-------------|
| `name` | Stable human-readable name (maps to scenario inventory) |
| `request.method` | HTTP method |
| `request.url` | `{{gateway}}/api/v1/...` (or internal host var for ops-only) |
| `request.header` | `Content-Type`, `Authorization: Bearer {{…Jwt}}`, `X-Correlation-Id` |
| `request.body` | JSON raw when needed |
| `description` | Purpose + expected outcome (success or denial) for human verification |
| Capture script | Optional post-response sets environment vars from JSON body |

**Validation / rules**:

- No hard-coded host; use `{{gateway}}`.
- No hard-coded JWT; use captured env vars after login.
- Descriptions carry expected outcomes (not automated asserts).

### Environment Template (`Local`)

| Variable | Kind | Example / notes |
|----------|------|-----------------|
| `gateway` | config | `http://localhost:8080` |
| `postService` | config (ops) | optional; internal bulk only |
| `aliceEmail` | config | `alice@example.test` |
| `aliceUsername` | config | `Alice` |
| `alicePassword` | config | local demo only |
| `bobEmail` | config | `bob@example.test` |
| `bobUsername` | config | `Bob` |
| `bobPassword` | config | local demo only |
| `rateLimitEmail` / `rateLimitUsername` / `rateLimitPassword` | config | dedicated SC-012 subject |
| `aliceId` / `bobId` | capture | from register/login |
| `aliceJwt` / `bobJwt` | capture | from login (`accessToken`) |
| `rateLimitJwt` / `rateLimitUserId` | capture | rate-limit scenario |
| `boundaryPostId`, `originalPostId`, `timelinePostId`, `interactionParentId`, `replyPostId`, … | capture | post IDs as scenarios need |
| `postCursor`, `timelineCursor`, `notificationCursor` | capture | pagination |
| `followRelationshipId` | capture | follow create |

**Validation / rules**:

- Commit only local demo secrets (not production).
- Empty string acceptable for capture slots before first run.
- Re-run path refreshes JWTs via login without changing stable config identities.

### Scenario

Logical grouping of one or more Requests used for a demo or check (happy path step, negative
case, rate-limit burst, etc.). Tracked in [contracts/scenario-inventory.md](./contracts/scenario-inventory.md).

### Captured Runtime Variable

Value written by a capture script and read by later requests. Lifecycle: empty → set on success
response → overwritten on re-login / new publish → never written to service logs by design of
the platform.

## Relationships

```text
Environment Template ──provides──► Request URL/headers/body variables
API Collection ──contains──► Folder ──contains──► Request
Request ──capture script──► Captured Runtime Variable ──consumed by──► later Request
Scenario ──maps to──► one or more named Requests (inventory)
```

## State transitions (demo session)

| State | Meaning |
|-------|---------|
| Fresh import | Config vars set; capture vars empty |
| After first-time Happy Path | IDs + JWTs + sample post/follow IDs populated |
| Login-first re-run | Config unchanged; JWTs refreshed; new post IDs may overwrite scenario captures |
| Token expired | Protected calls fail auth; re-run Login Alice/Bob |
| After optional load | Large numbers of posts/follows exist; not required for Happy Path |

## Non-entities (explicit)

- No new database tables, Kafka topics, or OpenAPI paths.
- No Postman Cloud workspace identity required in git.
