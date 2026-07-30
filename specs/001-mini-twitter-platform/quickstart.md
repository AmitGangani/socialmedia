# Quickstart and Manual Validation: SocialMedia

This guide describes the runnable evidence expected after implementation. It is not an automated
test plan. Contract details live in [openapi.yaml](./contracts/openapi.yaml), event details in
[events.asyncapi.yaml](./contracts/events.asyncapi.yaml), and owned fields in
[data-model.md](./data-model.md).

## Prerequisites

- Java 21 and Maven 3.9+ for local packaging.
- Docker Engine with Compose v2.
- `openssl` for local RSA keys.
- Postman (or a compatible client) that can import the version-controlled collection under
  `postman/` (see also [Postman feature quickstart](../002-postman-collection/quickstart.md)).
- Enough local resources for seven Java processes, PostgreSQL, Kafka, and the demo dataset.

## One-time local setup

From the repository root:

```bash
cp .env.example .env
mkdir -p secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt-private.pem
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem
chmod 640 secrets/jwt-private.pem
chmod 644 secrets/jwt-public.pem
```

Set the five database-role passwords, set `JWT_KEYS_GID` to the output of `id -g`, and set other
non-secret local values in `.env`. Do not commit `.env` or `secrets`. Only User Service receives
the private key; Gateway and domain services receive the public key. User Service stays non-root
and receives the host key's group as a supplemental group so `0640` remains sufficient.

## Build and start

Package compilation is an engineering check, not an application test suite:

```bash
./mvnw -DskipTests package
docker compose build
docker compose up -d
docker compose ps
```

Expected:

- Maven annotation processing succeeds for Lombok-annotated classes, and packaged service
  artifacts do not contain Lombok as a runtime library.
- PostgreSQL and Kafka become healthy before dependent services start.
- Eureka is available on `http://localhost:8761`.
- Gateway is available on `http://localhost:8080`.
- Eureka shows `USER-SERVICE`, `POST-SERVICE`, `FOLLOW-SERVICE`, `TIMELINE-SERVICE`,
  `NOTIFICATION-SERVICE`, and `API-GATEWAY` registered.
- `docker compose ps` shows each deployable independently running.

Use Actuator health through documented local operational ports or `docker compose exec`; domain
ports are not public client entry points.

## Request collection variables

Import `postman/SocialMedia.postman_collection.json` and `postman/Local.postman_environment.json`
into Postman, select environment **Local**, and confirm `gateway=http://localhost:8080`.
Capture-only scripts populate:

- Alice and Bob user IDs.
- Alice and Bob JWTs.
- Follow relationship ID.
- Original post ID, reply ID, and page cursors.
- A caller-supplied `X-Correlation-Id` for the primary publish flow.

JWTs and passwords are client variables only and must not appear in service logs. Scripts do not
assert pass/fail; verify responses manually against each request description.

## Complete primary-flow walkthrough

Run folder **`00 Happy Path`** in `postman/SocialMedia.postman_collection.json` top to bottom:

1. `Register Alice`, `Register Bob`, `Login Alice`, and `Login Bob by email`.
2. `Alice follows Bob`.
3. `Bob publishes interaction parent`.
4. Poll `Alice home timeline - first page` for at most 10 seconds, until it contains the captured
   `interactionParentId`.
5. Run `Alice replies to Bob's post`.
6. Poll `Poll Bob's notifications` for at most 10 seconds, until FOLLOW has
   `subjectId=followRelationshipId` and REPLY has `subjectId=replyPostId`.

**Login-first re-run** (retained Compose volumes): skip register (or accept conflict), run both
logins to refresh JWTs, then continue. Rotate Alice/Bob emails/usernames in the Local environment
only when deliberately re-demonstrating registration. The collection captures all IDs and JWTs
needed by later requests; no client-supplied actor or recipient ID is accepted by protected
operations.

### Recorded Phase 9 primary-flow evidence (2026-07-23)

The rebuilt Compose environment was exercised through Gateway with fresh isolated accounts and
the version-controlled request shapes above. No JWT or password was printed or recorded.

| Observation | Recorded result |
|---|---|
| Registration and login | Alice/Bob returned `201/201`; both logins returned `200` |
| Follow command | `201`; relationship `019f9024-ed8d-75b1-8dd4-7a942b9b5e79` |
| Original publication | `201`; post `019f9024-eeb9-7997-ac9e-22b2b115867f`; response retained `phase9-primary-publish` |
| Timeline consistency | Alice's page contained the post after `2,518 ms`, within the 10-second window |
| Reply command | `201`; reply `019f9024-f935-72a6-bcc6-c49354b9743a` |
| Notification consistency | Bob's page contained the matching FOLLOW and REPLY after `1,486 ms`, within the 10-second window |
| Correlation trace | Gateway, both producer outboxes, Timeline, and Notification retained the `phase9-primary-*` correlation values |
| Ownership inputs | Acting users came from Alice/Bob JWT subjects; notification retrieval used only Bob's JWT subject |

## Scenario 1: Account access and profile composition

1. Run `Register Alice`, `Register Bob`, then both login requests.
2. Run `Alice - own account` with Alice's JWT.
3. Run `Bob - public profile` without a JWT.
4. Repeat registration with Alice's username in different case, then with her email in different
   case.
5. Run login once with a wrong password.

Expected:

- Registrations return distinct UUIDv7 user IDs and never return a password hash.
- Own account includes private email; public profile does not.
- Public profile returns zero follower/following/post counts from the owning services.
- Case-insensitive duplicates return `409` without a partial account.
- Wrong login returns the same `401` shape regardless of which credential was wrong.
- Every response contains `X-Correlation-Id`.

### Recorded Phase 3 evidence (2026-07-19)

The packaged jars were run against an isolated PostgreSQL 18 cluster with Eureka and Gateway.
Docker was not used for this run because the host denied access to its Docker socket. The same
version-controlled requests and public routes were used.

| Observation | Recorded result |
|---|---|
| Register Alice and Bob | `201`; both IDs were UUIDv7 |
| Login by normalized username and email | `200`; Bearer JWTs issued |
| JWT policy | Claims were exactly `sub`, `iss`, `iat`, `exp`, `jti`; lifetime was 1,800 seconds |
| Alice self view | `200`; email present; password/hash fields absent |
| Bob public profile via mixed-case username | `200`; follower/following/post counts were `0/0/0`; email and hash absent |
| Mixed-case duplicate username and email | `409` for each; rejected requests created no account rows |
| Wrong password and unknown login | Identical `401` status, content type, and response-field shape |
| Unknown registration field | `400` |
| Canonical registration boundaries | 11-byte password `400`; 12-byte password with 80-code-point display name `201`; 73-byte password `400` |
| Password persistence | All three committed rows contained BCrypt hashes and no submitted plaintext |
| Correlation response | Every observed response returned the supplied `X-Correlation-Id` |
| Post count dependency stopped | Public profile returned `503` in 1.09 seconds; no partial counts were returned |

## Scenario 2: Follow, publish, and fan-out-on-write

1. Run `Alice follows Bob` twice.
2. Read Alice's and Bob's public profiles.
3. Run `Bob publishes original` with a recognizable correlation ID.
4. Note the command response time; it must not wait for Timeline visibility.
5. Poll `Alice home timeline` for at most 10 seconds.
6. Search container logs for the correlation ID.

Expected:

- First follow creates one edge; the repeat returns the same current outcome and creates no
  second notification event.
- Profile counts reflect the exact committed Follow row.
- Bob's post response returns immediately with exact text and no edit route.
- Alice sees the post within the consistency window; users who follow Bob afterward do not see
  this older post.
- Correlation logs trace Gateway -> Post -> outbox publisher -> Kafka -> Timeline -> Follow and
  later Timeline -> Post.
- No password, JWT, or email appears in those logs.

## Scenario 3: One bulk hydration call and stable cursors

1. Use the collection's demonstration runner to create at least 200 eligible posts for accounts
   Alice follows. This runner sends documented HTTP requests and contains no assertions or test
   framework.
2. Request Alice's first home page with `size=20`; save `nextCursor`.
3. Publish one new eligible post.
4. Request the next page using the saved cursor and continue through the original dataset.
5. Inspect Timeline/Post correlation logs for one non-empty page.

Expected:

- The original 200 items appear exactly once in deterministic newest-first order; the newly
  inserted post does not shift the saved keyset boundary into a duplicate or omission.
- Each non-empty page logs exactly one `POST /internal/v1/posts/bulk` call and zero per-entry
  Post calls.
- A malformed cursor or `size=101` returns `400`.
- An empty timeline returns `200` with an empty `items` array.

### Recorded Phase 5 fan-out and cursor evidence (2026-07-19)

The isolated Compose environment ran PostgreSQL 17.5, Kafka 4.1.1, Eureka, Gateway, User,
Post, Follow, and Timeline. Two hundred cursor-seed posts were submitted through Post's HTTP
contract inside the private Compose network; no test source, assertion framework, or direct Post
table insert was used.

| Observation | Recorded result |
|---|---|
| First and repeated follow | `201` then `200`; both responses returned the same relationship ID |
| Follow transition storage | One active edge; exactly three published outbox rows across create/remove/re-create (`2` created, `1` removed) |
| Self-follow | `400`; no edge or outbox row was added |
| Exact public-profile count | Bob's follower count was `1` after the committed edge |
| Publish command latency | `201` in 18 ms; the corrected fan-out was visible in Alice's home timeline within 1.5 seconds |
| Fan-out insert | Correlated Timeline log recorded one eligible follower and one duplicate-safe insert |
| Bulk hydration | Each observed non-empty home request emitted one bulk-call log and returned one ordered page; the empty page emitted no bulk call |
| Cursor traversal | Eleven pages traversed the 201-item saved dataset; all 201 IDs were unique |
| Insert after first page | The newer post returned `201` and did not appear beyond the saved cursor boundary |
| Cursor/page validation | Malformed cursor and `size=101` each returned `400` |
| Reference storage | Alice owned 202 rows with 202 distinct post IDs after the newer post was materialized |
| Deleted/missing omission | Deleting a materialized post returned `204`; the reference disappeared from Alice's page inside the 10-second window |
| Empty owner page | A new user with no references received `200` with zero items and no Post hydration call |

## Scenario 4: Likes, replies, and parent deletion

1. Run `Alice likes Bob post` twice.
2. Run `Alice replies to Bob post`.
3. Poll Bob's notifications for at most 10 seconds.
4. Delete the parent as Bob, then fetch the reply.
5. Try a new like and reply against the deleted parent.

Expected:

- Two identical like requests produce one `PostLike` and a count increase of one.
- The reply is a normal immutable post with a direct parent reference.
- Bob receives one REPLY notification; Alice does not receive a self-notification for her own
  content.
- After deletion, the parent is unavailable, the reply remains visible with
  `parent.available=false`, and new likes/replies return `404`.
- Deleted content disappears from direct, profile-post, and timeline views within 10 seconds.

### Recorded Phase 4 standalone post evidence (2026-07-19)

The packaged Post Service was run against an isolated PostgreSQL 18 database. Kafka consumption,
likes, replies, and Timeline propagation belong to later phases and are not claimed by this
standalone evidence.

| Observation | Recorded result |
|---|---|
| One-code-point original | `201`; returned ID was UUIDv7, exact text was preserved, and author matched JWT `sub` |
| 280-code-point original | `201` for 280 astral Unicode code points; exact submitted text was returned |
| Direct visible lookup | `200`; immutable fields were present and no edit field or edit route existed |
| Stable profile-post keyset | Two `size=1` pages returned distinct IDs and the first page supplied an opaque cursor |
| Non-author delete | `403`; a following direct lookup remained `200` |
| Author delete and repeat | `204` then `204`; only the real transition created a deletion event |
| Deleted visibility | Direct lookup returned `404`; profile list omitted the ID; bulk lookup returned only the remaining visible ID |
| Atomic outbox state | Two committed posts produced two publication rows; one real delete produced one deletion row; all three retained the supplied correlation IDs |
| Broker unavailable | All three rows remained `PENDING`; attempts advanced to `2`, next-attempt times backed off, and stored errors were exactly the sanitized `Kafka publish failed: TimeoutException` |
| Packaging | Post Service package succeeded with no application tests; the executable jar contained no Lombok runtime entry |

### Recorded Phase 7 like/reply evidence (2026-07-23)

The version-controlled Scenario 4 request shapes were executed through Gateway against the
Compose environment. Alice followed Bob so the parent was eligible for Alice's Timeline, and
Bob followed Alice so Alice's reply was independently eligible for Bob's Timeline. Notification
visibility remains a Phase 8 concern and is not claimed by this evidence.

| Observation | Recorded result |
|---|---|
| Idempotent like | First and repeated `PUT .../likes/me` returned `200` with `likeCount=1`; `post_like` contained one row and one distinct liker; a separate liked reply exposed `likeCount=1` in direct and Timeline views |
| Reply contract | Creation returned `201` with a UUIDv7 reply ID, `reply=true`, the direct parent ID, `parent.available=true`, `likeCount=0`, and JWT-derived ownership |
| Atomic reply event | The reply's `post.published.v1` outbox row reached `PUBLISHED` and retained both `parentPostId` and the parent's author snapshot |
| Timeline eligibility | The parent appeared for Alice on the third one-second poll and the reply appeared for Bob on the second; each observed non-empty page logged one bulk Post hydration call |
| Parent deletion | Bob's delete returned `204`; direct reply retrieval remained `200` with the same parent ID and `parent.available=false` |
| Deleted-parent rejection | A new like and a new reply against the deleted parent each returned `404`; no rejected reply/outbox row was created |
| Timeline cleanup/survival | The parent reference was absent on Alice's first post-delete poll, while Bob still had exactly one reply reference hydrated with the unavailable-parent label |
| Persistence evidence | The reply row retained its parent and parent-author IDs; Timeline storage contained one reply row and zero rows for the deleted parent |
| Packaging | Clean Post and Timeline packages and both updated container images built successfully with application tests skipped and no test source present |

## Scenario 4A: Notification timing measurement

1. Prepare Alice and Bob plus ten visible Bob posts. Ensure Alice is not following Bob before
   each follow attempt by running the documented unfollow request first.
2. Perform ten successful `Alice follows Bob` attempts. For each attempt, record the follow
   response time, poll Bob's notifications until the matching event appears or 10 seconds elapse,
   record the visibility time, then unfollow before the next attempt.
3. Perform ten successful replies by Alice, one against each prepared Bob post. For each attempt,
   record the reply response time, poll Bob's notifications until the matching reply appears or
   10 seconds elapse, and record the visibility time.
4. Preserve a 20-row evidence table in the demonstration notes with attempt type, source ID,
   action response time, notification visibility time, elapsed milliseconds, and pass/fail.

Expected:

- All 20 follow/reply actions complete from their owning service without waiting for notification
  visibility.
- At least 19 of the 20 matching notifications become visible to Bob within 10 seconds.
- Every successful attempt creates exactly one visible notification, and none is visible to Alice.
- Any attempt that exceeds 10 seconds remains recorded as a failure; it is not silently retried or
  replaced in the evidence set.

### Recorded Phase 8 notification evidence (2026-07-23)

The Notification image was rebuilt and the Compose environment was started against its retained
PostgreSQL and Kafka volumes. Two unique accounts isolated this run from earlier demonstration
data. Each action used the public Gateway contract, and each matching recipient page was polled
without blocking the Follow or Post command.

| Attempt | Type | Source ID | Action response (ms) | Visible after response (ms) | Start-to-visible (ms) | Result |
|---:|---|---|---:|---:|---:|---|
| 1 | FOLLOW | `019f8ea5-2fdb-70aa-a16a-bf1472412e89` | 3,019 | 3,811 | 6,869 | PASS |
| 2 | FOLLOW | `019f8ea5-4142-7051-bb40-c8dc70b9b97d` | 95 | 736 | 865 | PASS |
| 3 | FOLLOW | `019f8ea5-44c2-7471-ad97-30ab152175dc` | 57 | 962 | 1,041 | PASS |
| 4 | FOLLOW | `019f8ea5-48fd-7cf3-b1cd-87b8029d9e8c` | 46 | 928 | 992 | PASS |
| 5 | FOLLOW | `019f8ea5-4d0b-7cd0-a02c-bdf38619a9d5` | 55 | 971 | 1,048 | PASS |
| 6 | FOLLOW | `019f8ea5-5189-774b-80b1-49f0c20d5519` | 81 | 905 | 1,017 | PASS |
| 7 | FOLLOW | `019f8ea5-5615-7203-bebf-4d29c414ea1c` | 118 | 753 | 915 | PASS |
| 8 | FOLLOW | `019f8ea5-5a13-7bfa-b943-77add81c83bc` | 112 | 771 | 925 | PASS |
| 9 | FOLLOW | `019f8ea5-5dde-7a5c-983c-e33cd12fd1d1` | 83 | 1,044 | 1,158 | PASS |
| 10 | FOLLOW | `019f8ea5-626d-7b73-9361-b122d62ef612` | 61 | 919 | 998 | PASS |
| 11 | REPLY | `019f8ea5-6686-76e2-9cb1-3b48f4961d5c` | 58 | 635 | 711 | PASS |
| 12 | REPLY | `019f8ea5-6935-74d9-96b3-a95b2d97c16e` | 27 | 907 | 951 | PASS |
| 13 | REPLY | `019f8ea5-6cf4-7e5a-a559-12507e9b7d62` | 33 | 1,221 | 1,270 | PASS |
| 14 | REPLY | `019f8ea5-71ed-7ea9-98d3-fafc77bb1e72` | 31 | 907 | 953 | PASS |
| 15 | REPLY | `019f8ea5-75b2-7101-8241-859eee9919d0` | 30 | 998 | 1,045 | PASS |
| 16 | REPLY | `019f8ea5-79ee-7fad-9d66-f0998380b9c0` | 65 | 745 | 842 | PASS |
| 17 | REPLY | `019f8ea5-7d3b-7041-9818-cef4c1484969` | 45 | 1,047 | 1,131 | PASS |
| 18 | REPLY | `019f8ea5-81b1-7e48-9750-6cf7829468e8` | 48 | 1,080 | 1,160 | PASS |
| 19 | REPLY | `019f8ea5-8643-7719-8cf5-d7c2cd9cecb3` | 44 | 666 | 738 | PASS |
| 20 | REPLY | `019f8ea5-8925-74e1-a010-4a09d09edad5` | 31 | 1,201 | 1,255 | PASS |

| Observation | Recorded result |
|---|---|
| Timing target | `20/20` matching notifications were visible within 10 seconds; the first follow included cold service-discovery/load-balancer resolution and still completed in 6.869 seconds |
| Trigger independence | Every follow and reply returned before its matching notification became visible |
| Recipient ownership | The recipient owned exactly 20 rows from the measured actions; the actor's JWT returned zero rows even when a recipient user ID was supplied as an extra query parameter and header |
| Keyset retrieval | Two `size=7` pages returned 7 rows each with zero ID overlap; malformed cursor and `size=101` returned `400`, and a missing JWT returned `401` |
| Self-reply | The valid self-reply event created one `processed_event` row, zero notification rows, and did not change the recipient's visible count |
| Duplicate replay | The first follow's unchanged source key/envelope was republished twice; its visible-notification count and processed-event count remained `1/1` |
| DLT handling | An unknown event produced exactly three listener failures (initial delivery plus two one-second retries) and then appeared unchanged in `post-events.v1.notification-dlt` |
| DLT metadata | The DLT record retained original topic, partition, and offset headers; exception message, cause, and stack-trace headers remained excluded |
| Correlation | Notification logs retained the source correlation ID for created, no-op, duplicate, and failed/DLT paths without logging event payload text |

## Scenario 5: Unfollow and re-follow boundary

1. Run `Alice unfollows Bob` twice.
2. Poll Alice's home timeline for at most 10 seconds.
3. Re-follow Bob and publish a new Bob post.
4. Verify the old pre-unfollow posts remain absent and the new post appears.
5. Replay the old `follow.removed.v1` record after the new post is visible.

Expected:

- Repeated unfollow is a no-op and creates no extra outbox record.
- Cleanup removes entries at or before `unfollowedAt`.
- Re-follow does not backfill history.
- Replaying the older removal does not delete the post published after re-follow because cleanup
  is time-bounded.

### Recorded Phase 5 unfollow/re-follow evidence (2026-07-19)

| Observation | Recorded result |
|---|---|
| First and repeated unfollow | `204` then `204`; only the first transition emitted `follow.removed.v1` |
| Cleanup visibility | The pre-unfollow post disappeared from the home timeline within 0.5 seconds |
| Re-follow | `201`; the old post remained absent, proving no historical backfill |
| New publication | A post published after re-follow became visible normally |
| Old removal replay | Event `019f79af-0fff-7668-bef5-76ce9907ee00` was republished unchanged |
| Time-bound safety | Alice had 202 entries before and after replay; all were newer than the old removal time |

## Scenario 6: Authorization and validation failures

Run each collection request and inspect the resource afterward:

- Alice attempts to delete Bob's post.
- A create request adds a client-supplied `authorId` or another unknown field.
- Alice attempts self-follow.
- Alice attempts to read Bob's notifications by adding a user ID parameter/header.
- Submit empty, whitespace-only, 281-code-point, and valid 1/280-code-point posts.
- Submit missing, expired, tampered, and malformed JWTs.

Expected:

- Non-author delete is `403` and the post remains.
- Unknown/impersonation fields are `400`; ownership always comes from JWT `sub`.
- Self-follow is `400` with no edge or event.
- Notification retrieval has no target-user input and returns only the JWT subject's rows.
- Post boundaries match the contract exactly.
- Invalid credentials are `401` and no protected state changes.

### Recorded Phase 4 post-validation evidence (2026-07-19)

| Request | Recorded result |
|---|---|
| Whitespace-only text | `400 application/problem+json` |
| 281 astral Unicode code points | `400 application/problem+json` |
| Client-supplied `authorId` | `400 application/problem+json`; no Post or outbox row was created |
| Malformed cursor | `400 application/problem+json` |
| `size=101` | `400 application/problem+json` |
| Missing JWT on publish | `401`; no Post or outbox row was created |
| PATCH edit attempt | `405 application/problem+json` |
| Correlation response | Every observed response returned the supplied `X-Correlation-Id` |

### Recorded Phase 9 authorization and validation evidence (2026-07-23)

Fresh isolated Alice/Bob accounts were exercised through Gateway against the full Compose stack.
No JWT or password was recorded in service logs.

| Request | Recorded result |
|---|---|
| Empty post text | `400 application/problem+json` |
| Whitespace-only text | `400 application/problem+json` |
| 1-code-point post | `201` |
| 280 astral Unicode code points | `201`; exact submitted text returned |
| 281 astral Unicode code points | `400 application/problem+json` |
| Client-supplied `authorId` | `400`; ownership remains JWT `sub` only |
| Alice delete of Bob's post | `403`; the post remained directly readable (`200`) |
| Self-follow | `400`; no edge was created |
| Missing JWT on publish | `401` |
| Malformed JWT (`not.a.jwt`) | `401` |
| Tampered JWT payload (valid shape, invalid signature) | `401` |
| Zeroed JWT signature | `401` |
| Expired RSA JWT (`exp` in the past) | `401` |
| Malformed post/timeline/notification cursors | `400` each |
| `size=101` on post list, timeline, and notifications | `400` each |
| Alice notification page with `userId` / `X-Target-User-Id` for Bob | `200` with only Alice-owned rows (empty page; no Bob rows leaked) |
| PATCH edit attempt | `405` |
| Correlation echo | Login and subsequent responses returned the supplied `X-Correlation-Id` |

## Scenario 7: Timeline dependency failure and circuit breaker

```bash
docker compose stop post-service
```

1. Request a non-empty home timeline repeatedly while Post is stopped.
2. Observe bounded failures and the named breaker state/log transition.
3. Restart Post and wait for registration/health.

```bash
docker compose start post-service
```

Expected:

- Timeline returns `503`; it never returns reference-only or partially hydrated content.
- Calls finish within the documented timeout; there is no HTTP retry storm.
- Unrelated account/follow/notification capabilities remain running.
- After the breaker recovery interval and a successful probe, the same page returns normally.

### Recorded Phase 5 dependency evidence (2026-07-19)

With a non-empty home timeline, Post Service was stopped independently. The request returned
`503` in 3.03 seconds and the failure response had no `items` field, so no reference-only or
partial page escaped. Post was restarted without restarting Timeline, Follow, User, Gateway,
Kafka, or PostgreSQL; the same request recovered to `200` with 20 hydrated items.

Timeline's bounded consumer failure path was also observed during implementation diagnosis:
the initial attempt plus two one-second retries published the unchanged source record to
`post-events.v1.timeline-dlt`. Its DLT headers retained event/correlation identity and original
topic, partition, offset, timestamp, and consumer group while exception messages, causes, and
stack traces were excluded. After correcting the JDBC timestamp binding, normal fan-out resumed.

### Recorded Phase 9 dependency and circuit-breaker evidence (2026-07-23)

Alice's home timeline already contained three hydrated posts. Post Service was stopped alone.

| Observation | Recorded result |
|---|---|
| Non-empty home while Post stopped | Five consecutive requests returned `503` |
| First bounded failure latency | `3,056 ms` (within the two-second RestClient bound plus discovery overhead) |
| Subsequent failures | About `1.0–2.1 s`; no partial `items` field appeared on any failure body |
| Partial-page leakage | Failure bodies exposed only `timestamp`/`status`/`error`/`path`; no reference-only content |
| Unrelated capabilities | Alice self-view remained `200`; follow command remained `200` while Post was down |
| Recovery | `docker compose start post-service` restored the same home page to `200` with `3` hydrated items after Post was healthy and the bulk circuit recovered (about 74 s wall clock from restart) |
| Other containers | Timeline, Follow, User, Notification, Gateway, Kafka, and PostgreSQL were not restarted |

## Scenario 8: Kafka DLT and idempotent replay

1. Stop Follow Service, then publish as Bob while Alice follows him. Post creation must still
   succeed because fan-out is asynchronous.
2. Observe Timeline's initial attempt plus two one-second retries and the record in
   `post-events.v1.timeline-dlt`.
3. Restart Follow Service.
4. Consume the DLT record with Kafka's console consumer, preserving its key and unchanged JSON
   envelope.
5. Republish that key/value to `post-events.v1`; do not generate a new `eventId`.
6. Republish it once more.

Expected:

- The failed record contains original topic/partition/offset metadata and sanitized error data.
- First replay creates the missing Timeline entry within 10 seconds.
- Second replay creates no duplicate visible entry.
- Notification's consumer group may see the same replay and records/no-ops it without a duplicate.
- The original correlation ID and event ID remain traceable end to end.

No replay REST endpoint, DLT UI, or generic replay service is expected.

### Recorded Phase 9 DLT and duplicate-replay evidence (2026-07-23)

Follow Service was stopped while Alice still followed Bob. Bob published through Gateway with
correlation `phase9-dlt-publish`. Publish still returned immediately (`201` in 179 ms) because
fan-out is asynchronous.

| Observation | Recorded result |
|---|---|
| Timeline retries | Exactly three listener failures (initial delivery plus two one-second retries) logged `ResourceAccessException` against Follow |
| Timeline DLT record | Unchanged `post.published.v1` envelope for event `019f904a-4791-71af-988b-4944ecbd4a26` / post `019f904a-4778-7768-aa3b-666526f9a85c` appeared on `post-events.v1.timeline-dlt` |
| DLT headers | Retained `eventId`, `eventType`, `correlationId=phase9-dlt-publish`, `kafka_dlt-original-topic=post-events.v1`, original partition/offset/timestamp, and `kafka_dlt-original-consumer-group=timeline-service-v1` |
| Sanitized failure metadata | Headers included exception FQCN only; exception message, cause text, and stack-trace headers were absent from the inspected DLT record |
| Follow restart + replay | After Follow was healthy and re-registered, the unchanged key/value was republished to `post-events.v1` |
| First replay | Alice's home contained the post and `timeline_entry` count for that `post_id` became `1` within the 10-second window |
| Second identical replay | `timeline_entry` count remained `1` (no duplicate visible entry) |
| Notification side-effect of replay | Notification recorded `processed_event` for the same `eventId` once; no follower notification row is created for plain `post.published.v1` |
| Notification unknown-type DLT | A deliberate `post.unknown.v1` with correlation `phase9-notification-dlt` produced three failures then an unchanged record on `post-events.v1.notification-dlt` (and Timeline's matching DLT) without payload-text logging |

## Architecture walkthrough: ownership and the Timeline tradeoff

The public edge is deliberately small: Gateway validates JWTs, applies the local rate limit,
creates or accepts `X-Correlation-Id`, and routes explicit `/api/v1` paths. It owns no domain
state and never routes `/internal/v1`. Eureka supplies logical service locations; it is not a
data store or an additional API boundary.

| Responsibility | Owned state | Synchronous contracts | Asynchronous role |
|---|---|---|---|
| User | `Account` in `user_db` | Auth, self/profile, internal existence; composes exact counts | None |
| Post | `Post`, `PostLike`, Post outbox in `post_db` | Post commands/queries, internal count and bulk hydration | Produces post facts after local commit |
| Follow | `FollowRelationship`, Follow outbox in `follow_db` | Follow commands, internal counts and eligible-follower pages | Produces follow facts after local commit |
| Timeline | Reference-only `TimelineEntry` in `timeline_db` | Home keyset page; one Post bulk call per non-empty page | Consumes post/follow facts idempotently |
| Notification | `Notification` and `ProcessedEvent` in `notification_db` | JWT-subject notification page | Consumes relevant post/follow facts exactly once visibly |

The primary flow is a REST/Kafka hybrid. A publish is an immediate REST command to Post; the
post and its outbox row commit atomically, so the caller does not wait for fan-out. The outbox
publisher emits the unchanged correlation ID in the JSON envelope and Kafka headers. Timeline
consumes the fact, uses Follow's private paged REST contract to enumerate eligible followers,
and stores only `(ownerUserId, postId, authorId, publishedAt)` references. A later home read is a
keyset query followed by one bounded Post bulk request, which keeps Post as the content owner.

| Choice | Publish/read cost | Data ownership and failure behavior |
|---|---|---|
| Implemented fan-out-on-write | Publication work grows with follower count; home reads are cheap and stable | Timeline owns duplicate-safe references only; failed Follow lookup retries then reaches its DLT |
| Fan-out-on-read alternative | Publication is cheap; every home read repeatedly merges followed authors' posts | Requires more read-time graph/content work and makes bounded stable paging harder at this scope |
| Celebrity hybrid (not implemented) | Would selectively avoid very large write amplification | Needs thresholds, job coordination, mixed read paths, and scaling policy not justified by this local baseline |

Failure remains owner-specific. If Post is unavailable during hydration, Timeline returns a
bounded `503` instead of leaking references or a partial page. Kafka delivery is at-least-once;
Timeline's `(ownerUserId,postId)` uniqueness makes replay harmless. Restarting one responsibility
does not redeploy the others, and its data survives in the PostgreSQL volume under its exclusive
database role. The 1,000-follower scenario demonstrates linear write amplification; it does not
claim to solve celebrity-scale fan-out, high availability, or horizontal publisher coordination.

## Scenario 9: High-follower tradeoff

1. Use the HTTP collection's data-runner mode to create 1,000 follower accounts and make each
   follow one author. This is data setup through public contracts, not an automated test suite.
2. Record Timeline entry count for the author's next post, then publish once.
3. Poll until fan-out completes and inspect the event's correlation logs.
4. Read several follower timelines.

Expected:

- One publication produces exactly one Timeline entry per eligible current follower and no
  duplicates when the event is replayed.
- Logs show follower pages and batch inserts, making work growth with follower count visible.
- Timeline reads remain a keyset query plus one bulk Post call.
- Documentation explicitly labels slow publication/fan-out for celebrities as unsolved; there is
  no job coordinator, distributed lock, or hybrid celebrity path.

### Recorded Phase 6 amplification and replay evidence (2026-07-20)

The Compose environment used the request-only data runner shape: 1,000 accounts were registered
and authenticated through User's public HTTP contract, each JWT subject followed the author
through Gateway, and the measured publication also entered through Gateway. No direct table seed,
assertion handler, test source, or load-test framework was used.

| Observation | Recorded result |
|---|---|
| Eligible follower setup | `1,000` active Follow rows with `1,000` distinct follower IDs |
| Measured post/event | Post `019f8046-84e0-72dd-8dd7-5c2939af41b2`; event `019f8046-8517-70cb-91ed-e7f8c09ded6f`; correlation `scenario9-high-follower-publish` |
| Fan-out completion | First follower read contained the post about 3 seconds after publication began |
| Write amplification | Ten eligible-follower pages of 100 each logged `inserted=100`; storage contained exactly `1,000` rows for `1,000` distinct owners |
| Read cost | A non-empty follower page logged one bulk Post hydration call for its one returned reference |
| Duplicate replay | The unchanged key/envelope/event ID was republished once; ten pages logged `inserted=0` and storage remained exactly `1,000` rows |
| Correlation trace | `phase6-correlation-trace` appeared at Gateway routing, Post outbox publication, and all ten Timeline consumer pages; envelopes and Kafka headers carry the same value |
| Celebrity limitation | Linear follower enumeration and inserts are visible; no coordinator, distributed lock, or hybrid read path is claimed |

## Scenario 12: Gateway rate-limit exhaustion and one-minute refill

Local per-instance Gateway buckets (FR-042 / SC-012 / US6 acceptance scenario 7):

| Bucket | Capacity | Key | Routes |
|---|---|---|---|
| Authentication | 10 / minute | Client address | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` |
| Protected writes | 60 / minute | Authenticated JWT `sub` | `POST` / `PUT` / `PATCH` / `DELETE` public routes |

Use folder **`06 Gateway Limits`** in `postman/SocialMedia.postman_collection.json` (dedicated
rate-limit account, Runner data files `postman/data/auth-exhaust-11.csv` and
`write-exhaust-61.csv`, rejected register/post probes, and post-refill requests). Bucket4j refills
greedily (~1 auth token / 6 s, ~1 write token / s), so exhaustion must run as an uninterrupted
burst; a slow serial client may never empty the write bucket.

1. Wait at least one minute with no auth or protected-write traffic from the demo client/account.
2. Register and log in the dedicated rate-limit subject; capture its JWT.
3. Burst more than 10 wrong-password logins against the auth bucket; immediately attempt a unique
   registration while exhausted.
4. Confirm the unique username has no public profile and User Service logs do not show the rejected
   correlation ID.
5. Wait at least 60 seconds; repeat a wrong-password login (expect domain `401`, not `429`) and a
   correct login (expect `200`).
6. Burst more than 60 authenticated protected writes (idempotent `PUT /api/v1/follows/{bobId}` is
   enough); immediately attempt a unique post while exhausted.
7. Confirm the rejected marker text is absent from the author's posts and Post Service logs do not
   show the rejected correlation ID.
8. Wait at least 60 seconds; publish the refill marker post (expect `201`).

Expected:

- Exhausted matching requests return `429` with `application/problem+json`,
  `X-RateLimit-Remaining: 0`, and the request correlation ID.
- Rejected requests never reach the owning domain service and create no domain state.
- After the one-minute refill window, matching auth and write requests succeed again.

### Recorded Phase 10 rate-limit evidence (2026-07-24)

Executed through Gateway (`localhost:8080`) against the full Compose stack with a dedicated
subject `RateLimit1784867510` (`019f9264-e0ee-7886-b9bb-92fafbc13047`) and follow target
`019f9264-df4a-7e80-bbd9-bed2cf4d8d90`. Exhaustion used multi-connection bursts so greedy refill
could not keep the write bucket full.

| Observation | Recorded result |
|---|---|
| Write burst | 70 parallel `PUT /api/v1/follows/{bobId}` in `1.034 s` → `60` × `200`, `10` × `429` |
| Write sample `429` | `application/problem+json` body `{"type":"about:blank","title":"Too Many Requests","status":429,"correlationId":"scenario12-write-exhaust-21"}` with `X-RateLimit-Remaining: 0` |
| Write rejected unique post | `POST /api/v1/posts` with text `RATE-LIMIT-REJECTED-MARKER-must-not-exist-v2` → `429`, `X-RateLimit-Remaining: 0` |
| Write no domain change | Author post page had `item_count=2` and `marker_present=False` for the rejected v2 marker; prior non-exhausted markers only |
| Write before domain routing | `post-service` logs had no `scenario12-write-rejected-post-v2`; `follow-service` logs had no `scenario12-write-exhaust-21` |
| Write after refill (~65 s) | `POST` marker `RATE-LIMIT-REFILL-MARKER-must-exist-v2` → `201`, `X-RateLimit-Remaining: 59`, post id `019f9269-f820-7884-b0df-bd72c62d2d2c` |
| Auth burst | 15 parallel wrong-password logins in `0.603 s` → `10` × `401`, `5` × `429` |
| Auth sample `429` | `{"type":"about:blank","title":"Too Many Requests","status":429,"correlationId":"scenario12-auth-exhaust-04"}` with remaining `0` |
| Auth rejected unique register | `POST /api/v1/auth/register` for `RateLimitRejected1784867846` → `429`, `X-RateLimit-Remaining: 0` |
| Auth no domain change | `GET /api/v1/profiles/RateLimitRejected1784867846` → `404`; `user-service` logs had no `scenario12-auth-rejected-register` |
| Auth after refill (~65 s) | Wrong-password login → `401` with `X-RateLimit-Remaining: 9` (domain reached); correct login for the demo subject → `200` with access token and `X-RateLimit-Remaining: 8` |

## Scenario 10: Independent restart

For each domain service in turn:

```bash
docker compose restart <service-name>
docker compose ps
```

Expected:

- Unrelated containers remain running and are not rebuilt/redeployed.
- Eureka removes/re-registers the restarted instance.
- Its owned capability becomes usable within 60 seconds.
- Owned data remains because it lives in that service's exclusive database.

### Recorded Phase 6 independent-restart evidence (2026-07-20)

Each domain container was gracefully stopped and started alone so Eureka removal and subsequent
registration could be observed separately. `healthy_registered` is measured from the individual
service start; Eureka's removal view can lag shutdown by its local response-cache interval.

| Service | Eureka removal observed | Healthy and registered | Unrelated services | Container/data identity |
|---|---:|---:|---|---|
| User | 15 s | 30 s | stayed running | same container and PostgreSQL volume |
| Post | 29 s | 60 s | stayed running | same container and PostgreSQL volume |
| Follow | 29 s | 60 s | stayed running | same container and PostgreSQL volume |
| Timeline | 28 s | 60 s | stayed running | same container and PostgreSQL volume |
| Notification | 29 s | 60 s | stayed running | same container and PostgreSQL volume |

After all five restarts, Eureka again listed `USER-SERVICE`, `POST-SERVICE`, `FOLLOW-SERVICE`,
`TIMELINE-SERVICE`, `NOTIFICATION-SERVICE`, and `API-GATEWAY`. The author's composed profile still
reported `1,000` followers and two posts, the measured post remained directly visible, the first
follower's hydrated Timeline still contained it, Notification health was `UP`, and the measured
post still had `1,000` distinct Timeline owners.

## Architecture review checklist

- [x] Account exists only in `user_db`; Post/PostLike only in `post_db`; FollowRelationship only
  in `follow_db`; TimelineEntry only in `timeline_db`; Notification/ProcessedEvent only in
  `notification_db`.
- [x] Each runtime datasource credential can connect only to its owned database.
- [x] No service imports another service's entity, repository, or DTO class.
- [x] Gateway exposes no `/internal/v1` route and owns no domain data.
- [x] Post/Follow domain writes and their outbox rows share one local transaction.
- [x] Kafka delivery is described as at-least-once; replays preserve event ID.
- [x] Timeline stores references only and makes one bulk Post call per non-empty page.
- [x] Correlation ID crosses REST and Kafka without secrets.
- [x] No generic CRUD base, mapper framework, single-use service interface, Config Server,
  feature flag, Redis, Schema Registry, CDC, tracing stack, or orchestration platform exists.
- [x] No automated test source, test-only dependency, runner, coverage tool, generated test
  report, or CI application-test stage exists.

### Recorded Phase 6 architecture review evidence (2026-07-20)

- All five runtime roles connected to their owned database and were denied when attempting a
  foreign database connection.
- A source scan found zero cross-service Java imports, zero Gateway internal routes, and zero
  copied content/profile/like fields in Timeline's migration.
- Post and Follow application services place domain mutation and outbox persistence under the
  same local `@Transactional` method.
- The tracked tree contains zero `src/test` files, test dependencies, generic service/mapper
  interfaces, generic CRUD bases, or forbidden infrastructure dependencies.
- The live Compose view exposed only Gateway and Eureka, kept domain/infrastructure containers on
  `socialmedia-private`, and retained PostgreSQL/Kafka named volumes across individual restarts.

### Recorded Phase 9 production-source and constitution review (2026-07-23)

A full production-source scan and live environment check were repeated after the complete
baseline (US1–US6 plus polish packaging) was present.

| Gate | Result | Evidence |
|---|---|---|
| Boundary / ownership | PASS | Zero cross-service Java package imports; each `application.yml` points at one owned JDBC database; Timeline migration stores only reference columns |
| Database roles | PASS | Live roles `user_service`, `post_service`, `follow_service`, `timeline_service`, and `notification_service` connect to their owned databases and receive `FATAL: permission denied` / no `CONNECT` on foreign databases |
| Contracts | PASS | Phase 9 primary-flow, Scenario 6 validation, Scenario 7 hydration failure, Scenario 8 DLT/replay, and Phase 10 Scenario 12 rate-limit `429`/`X-RateLimit-Remaining` evidence match the versioned OpenAPI/AsyncAPI contracts |
| Stack / simplicity | PASS | No MapStruct/ModelMapper, no reactive `Mono`/`Flux` application code, no `@Data` on entities, no single-use service interfaces, no generic CRUD bases |
| Resilience | PASS | One bulk Post breaker only; stopped-Post home pages return bounded `503` without partial items; Kafka uses initial delivery plus two retries then consumer-specific DLTs |
| Observability | PASS | Correlation IDs crossed Gateway, publish, Timeline fan-out/failure/replay, and Notification unknown-type failure without logging JWTs, passwords, emails, or event payload text |
| Security | PASS | Foreign delete `403`, impersonation fields `400`, invalid/expired/tampered JWTs `401`, notification pages ignore client-supplied target user inputs |
| Delivery | PASS | Host-published ports remain Gateway `8080` and Eureka `8761` only; domain services stay on the private Compose network |
| Verification policy | PASS | Zero `src/test` trees, zero `*Test.java`, zero test-scoped Maven dependencies, no CI application-test stage |
| Forbidden scaffolding | PASS | No Config Server, Schema Registry, Redis, tracing stack, Kubernetes, feature flags, or CDC dependencies in production sources |

Final manual acceptance: the packageable Compose environment demonstrates registration, login,
follow, publish, timeline hydration, reply/notification consistency, authorization/validation
boundaries, dependency isolation, DLT inspection, unchanged-event replay, Gateway rate-limit
exhaustion and one-minute refill (`429` before domain routing), and independent service ownership
without automated application tests.

## Stop the environment

```bash
docker compose down
```

Do not add `--volumes` during ordinary validation; keeping data makes restart ownership visible.
Removing volumes is a separate destructive reset action and should be intentional.
