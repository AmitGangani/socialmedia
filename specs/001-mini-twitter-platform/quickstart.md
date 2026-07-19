# Quickstart and Manual Validation: SocialMedia

This guide describes the runnable evidence expected after implementation. It is not an automated
test plan. Contract details live in [openapi.yaml](./contracts/openapi.yaml), event details in
[events.asyncapi.yaml](./contracts/events.asyncapi.yaml), and owned fields in
[data-model.md](./data-model.md).

## Prerequisites

- Java 21 and Maven 3.9+ for local packaging.
- Docker Engine with Compose v2.
- `openssl` for local RSA keys.
- An HTTP client that runs the version-controlled `http/socialmedia.http` collection.
- Enough local resources for seven Java processes, PostgreSQL, Kafka, and the demo dataset.

## One-time local setup

From the repository root:

```bash
cp .env.example .env
mkdir -p .local/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local/keys/private.pem
openssl rsa -pubout -in .local/keys/private.pem -out .local/keys/public.pem
```

Set the five database-role passwords and non-secret local settings in `.env`. Do not commit
`.env` or `.local/keys`. Only User Service receives the private key; Gateway and domain services
receive the public key.

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

Open `http/socialmedia.http` and set its local environment to `gateway=http://localhost:8080`.
The collection captures:

- Alice and Bob user IDs.
- Alice and Bob JWTs.
- Follow relationship ID.
- Original post ID, reply ID, and page cursors.
- A caller-supplied `X-Correlation-Id` for the primary publish flow.

JWTs and passwords are client variables only and must not appear in service logs.

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

## Architecture review checklist

- [ ] Account exists only in `user_db`; Post/PostLike only in `post_db`; FollowRelationship only
  in `follow_db`; TimelineEntry only in `timeline_db`; Notification/ProcessedEvent only in
  `notification_db`.
- [ ] Each runtime datasource credential can connect only to its owned database.
- [ ] No service imports another service's entity, repository, or DTO class.
- [ ] Gateway exposes no `/internal/v1` route and owns no domain data.
- [ ] Post/Follow domain writes and their outbox rows share one local transaction.
- [ ] Kafka delivery is described as at-least-once; replays preserve event ID.
- [ ] Timeline stores references only and makes one bulk Post call per non-empty page.
- [ ] Correlation ID crosses REST and Kafka without secrets.
- [ ] No generic CRUD base, mapper framework, single-use service interface, Config Server,
  feature flag, Redis, Schema Registry, CDC, tracing stack, or orchestration platform exists.
- [ ] No automated test source, test-only dependency, runner, coverage tool, generated test
  report, or CI application-test stage exists.

## Stop the environment

```bash
docker compose down
```

Do not add `--volumes` during ordinary validation; keeping data makes restart ownership visible.
Removing volumes is a separate destructive reset action and should be intentional.
