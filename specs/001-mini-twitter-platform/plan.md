# Implementation Plan: SocialMedia Platform

**Branch**: `001-mini-twitter-platform` | **Date**: 2026-07-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-mini-twitter-platform/spec.md`

## Summary

Build six independently deployable Spring Boot responsibilities: User, Post, Follow,
Timeline, Notification, and API Gateway, plus a small Eureka registry. REST returns immediate
results; two Kafka domain streams carry only post and follow facts needed for precomputed
timelines and notifications. Post and Follow use application-managed transactional outboxes.
Every stateful service owns an exclusive PostgreSQL database and role.

The design is intentionally narrow. Each service uses responsibility-based packages, concrete
services and clients, local contract records, Lombok-assisted constructor injection, and direct
Controller -> Service -> Repository flow. There is no shared code library, generic CRUD layer,
mapper framework, reactive stack, Config Server,
Schema Registry, CDC platform, Redis, tracing stack, Kubernetes, feature flags, or automated
test source.

## Technical Context

**Language/Version**: Java 21 LTS

**Primary Dependencies**: Spring Boot 4.1.x (4.1.0 baseline); Spring Cloud 2025.1.2
Oakwood BOM; Spring MVC; Spring Data JPA; Spring Security resource server/JOSE; Spring
Kafka; Spring Cloud Gateway Server Web MVC; Spring Cloud Netflix Eureka; Spring Cloud
CircuitBreaker Resilience4j; Flyway; PostgreSQL driver; Lombok; `uuid-creator` 6.1.1. Lombok
is compile-time/provided only and uses the version managed by the Spring Boot parent; versions
below the Boot/Cloud BOMs are not independently pinned.

**Lombok Policy**: Modules that use Lombok declare it as a `provided` dependency and annotation
processor; the executable packaging excludes it. Use `@RequiredArgsConstructor` for constructor
injection and targeted `@Getter`/`@Setter` plus protected `@NoArgsConstructor` where JPA requires
them. Keep boundary DTOs as Java records. Do not use `@Data`, entity-wide generated
`@ToString`, or field-derived `@EqualsAndHashCode` on JPA entities; sensitive fields and lazy
relationships must never enter generated string/equality behavior.

**Concurrency Model**: Blocking MVC, JPA, and `RestClient` on Java 21 virtual threads;
`spring.main.keep-alive=true`; no Reactor application code or custom executor pools.

**Storage**: One local PostgreSQL process with `user_db`, `post_db`, `follow_db`,
`timeline_db`, and `notification_db`; each has an exclusive runtime role. No cross-database
queries or foreign keys.

**Service Discovery**: One Eureka server is the sole registry. Single-node operation is an
explicit local-demo limitation.

**Service Communication**: Versioned JSON REST under `/api/v1` and `/internal/v1`; Kafka
JSON events on `post-events.v1` and `follow-events.v1` with documented versioned envelopes.

**Manual Verification**: `http/socialmedia.http`, [quickstart.md](./quickstart.md), health
checks, correlation logs, cursor traversal, replay, failure, independent restart, and
high-follower demonstrations. Automated application tests and test-only tooling are forbidden.

**Target Platform**: Linux containers through one Docker Compose file; Maven multi-module
repository; only Gateway, Eureka dashboard, and required infrastructure ports are host-visible.

**Core Concept**: Explainable service ownership plus a focused REST/Kafka hybrid; the primary
system-design tradeoff is Timeline fan-out-on-write versus fan-out-on-read.

**Resilience Scope**: Two-second bounds on REST calls; one circuit breaker only on Timeline's
bulk Post lookup. It has no retry or fallback and returns `503` when a valid page cannot be
hydrated. Kafka has bounded listener retry and consumer-specific DLTs.

**Observability**: Structured SLF4J/Logback console logs; `X-Correlation-Id` accepted/generated
at the edge, returned to callers, stored in MDC, propagated across REST, persisted in outboxes,
and copied to Kafka envelopes/headers. JWTs, passwords, email addresses, and event payload text
are not logged.

**Security**: BCrypt; 30-minute RSA JWTs; Gateway and domain services validate signature,
issuer, and expiry; acting user always comes from JWT `sub`; Bean Validation at HTTP boundaries.
Internal endpoints remain off Gateway routes and accessible only on the private Compose network.

**Performance Goals**: Primary flow under three minutes; timeline fan-out, deletion cleanup,
and unfollow cleanup within 10 seconds in every documented local run; at least 19 of 20
documented notification attempts visible within 10 seconds; one bulk Post call per non-empty
timeline page; owned capability healthy within 60 seconds after restart.

**Constraints**: 1-280 Unicode code points per post; cursor pages default 20/max 100;
deterministic `(sortTime,id)` keysets; at-least-once events with duplicate-safe consumers;
no historical timeline backfill; no partial profile counts or partial timeline hydration.

**Scale/Scope**: Single-machine portfolio environment; manual traversal of 200 timeline
entries and fan-out to at least 1,000 followers. High availability, horizontal scaling,
multi-region operation, and the celebrity problem are not claimed as solved.

## Constitution Check

### Pre-design gate

| Gate | Result | Evidence |
|---|---|---|
| Boundary | PASS | Six mandated responsibilities, each with one owner; five exclusive databases; no shared entities or repositories |
| Contracts | PASS | All REST and Kafka boundaries are versioned; failure and 10-second consistency behavior are explicit |
| Stack | PASS | Java 21, Boot 4.1.x, Cloud 2025.1.2, BOM-managed compile-time Lombok, MVC virtual threads, Gateway MVC, and Eureka only |
| Teaching focus | PASS | Each deployable has one named concept below; Timeline owns the primary scaling tradeoff |
| Simplicity | PASS | Responsibility-based packages, concrete classes, local records, no generic frameworks or speculative extension points |
| Resilience | PASS | One breaker on Timeline -> Post; no HTTP retries; Kafka retry relies on idempotency |
| Observability | PASS | Correlation ID crosses REST, outbox, Kafka, and downstream REST without secrets |
| Security | PASS | BCrypt, RSA JWT validation, JWT-derived ownership, private internal routes, Bean Validation |
| Delivery | PASS | One lightweight image per deployable and one Compose environment; no Kubernetes |
| Verification | PASS | Manual request/log/health/review evidence only; no test source or runner |

No constitution exception is required.

## Project Structure

### Documentation for this feature

```text
specs/001-mini-twitter-platform/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi.yaml
│   └── events.asyncapi.yaml
└── tasks.md                    # created later by /speckit-tasks
```

### Planned source layout

```text
pom.xml                         # aggregator + Boot parent/Cloud BOM + compiler/Lombok policy
gateway/
├── pom.xml
├── Dockerfile
└── src/main/{java,resources}/
discovery-server/
├── pom.xml
├── Dockerfile
└── src/main/{java,resources}/
services/
├── user-service/
├── post-service/
├── follow-service/
├── timeline-service/
└── notification-service/       # each has pom.xml, Dockerfile, src/main/{java,resources}
infra/
├── postgres/init-databases.sh  # creates five databases/roles from external env values
└── kafka/create-topics.sh      # creates two source topics and four DLTs
http/
└── socialmedia.http           # version-controlled manual request collection
.env.example                    # names only; real local values stay uncommitted
docker-compose.yml
```

**Structure Decision**: One Maven module per deployable proves independent build/restart while
the root POM removes repeated BOM coordinates. It contains no shared application code. Every
service uses a `com.example.socialmedia.<service>` root with only the application entry point at
that root. Other classes use the necessary `api`, `application`, `domain`, `persistence`,
`integration`, and `config` packages; controller/client DTO records stay beside the boundary that
owns them. Lombok configuration is centralized in the root build,
but each consuming module opts in with a compile-time/provided dependency; Lombok is never a
shared runtime library or an inter-service contract dependency.

## Simplicity Risk Review Before Service Design

| Candidate complexity | Decision before inclusion |
|---|---|
| Six deployables | **Keep**: explicitly required and each maps to one interview concept; splitting further is **SIMPLIFY** |
| Eureka | **Keep**: stakeholder-selected sole registry; peer nodes, zones, auth integration, and Config Server are **SIMPLIFY** |
| Kafka + outbox | **Keep**: explicit atomic-publication requirement; Schema Registry, CDC, Kafka Streams, and generic event libraries are **SIMPLIFY** |
| Five Flyway histories | **Keep with risk**: repetitive, but independently owned schemas require reproducible versioning; only one initial migration per service |
| Security configuration in each HTTP service | **Keep with risk**: small duplication allows each owner to verify JWT identity; a shared security starter is **SIMPLIFY** |
| Gateway rate-limit state | **Keep local only**: official Bucket4j/Caffeine MVC filter demonstrates the assigned concept; Redis/distributed claims are **SIMPLIFY** |
| Resilience4j | **Keep once**: one visible Timeline -> Post breaker; blanket policies, fallbacks, retries, bulkheads, and AOP annotations are **SIMPLIFY** |
| DTOs and mapping | **Keep only boundary records**: controller/client records protect contracts; shared DTO jars and mapper frameworks are **SIMPLIFY** |
| Lombok | **Keep narrowly**: stakeholder-selected compile-time boilerplate reduction for constructors/accessors; `@Data`, entity-wide generated equality/string output, experimental annotations, and runtime packaging are **SIMPLIFY** |
| Exception handling | **SIMPLIFY**: use standard validation/Problem Detail plus direct `ResponseStatusException`; no hierarchy or shared advice framework |
| Configuration environments | **SIMPLIFY**: one externalized local configuration; no profile matrix, feature flags, secret manager, or remote config server |

## Service Design

The class lists below are ceilings for the baseline. Request/response/event records may be nested
in their controller, client, or consumer class when that keeps the package smaller. A Spring Data
repository interface is a required framework integration point, not an interface-per-class pattern.

Across all modules, Spring components use final collaborators with Lombok
`@RequiredArgsConstructor` for constructor injection. JPA entities use only the targeted Lombok
annotations required by their persistence shape, retain a protected no-argument constructor,
and do not generate `toString`, `equals`, or `hashCode` from mutable fields. Request, response,
cursor, and event payload types remain explicit Java records so their contracts are visible.

### 1. User Service

**Boundary**: Own accounts, password verification, JWT issuance, and public profile fields;
compose but never persist Follow- or Post-owned counts.

#### Minimal package/class structure

```text
com.example.socialmedia.user
├── UserApplication.java        # application entry point only
├── api/
│   ├── AuthController.java     # nested registration/login records
│   └── ProfileController.java  # public, me, internal-exists records
├── application/
│   ├── AccountService.java
│   └── JwtIssuer.java
├── domain/Account.java
├── persistence/AccountRepository.java # Spring Data interface
├── integration/ProfileCountClient.java # concrete Follow + Post calls
└── config/
    ├── SecurityConfig.java
    └── CorrelationIdFilter.java
```

#### Key endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | Create one account and return its public fields |
| POST | `/api/v1/auth/login` | Verify BCrypt password and issue expiring JWT |
| GET | `/api/v1/users/me` | Return the authenticated account without password/hash |
| GET | `/api/v1/profiles/{username}` | Compose public fields plus Follow/Post counts |
| GET | `/internal/v1/users/{userId}/exists` | Let Follow reject nonexistent targets |

#### Owned data fields

- `Account`: `id`, `email`, `normalizedEmail`, `username`, `normalizedUsername`,
  `displayName`, `bio`, `passwordHash`, `createdAt`.
- No profile table, persisted social/content counts, refresh-token table, credential history,
  or account-status state machine. Those have no baseline behavior and are marked **SIMPLIFY**.

#### One core HLD/LLD concept

Authentication lifecycle: case-insensitive identity constraints, BCrypt verification, and
minimal stateless RSA JWT issuance.

#### Complexity ledger

| Added complexity | One-line justification |
|---|---|
| `JwtIssuer` | Keeps credential construction and claim policy separate from account persistence |
| `ProfileCountClient` with two bounded calls | Public profile is explicitly required to compose counts from their owning services |
| Normalized email/username columns | Portable, readable case-insensitive uniqueness without PostgreSQL-specific `citext` behavior |
| Service-local JWT validation | Ownership still comes from a verified token if the service is reached inside the private network |
| Circuit breakers or cached counts | **SIMPLIFY**: return bounded `503`; stale/partial counts violate the profile contract |

### 2. Post Service

**Boundary**: Own immutable original posts, replies, soft deletion, likes, and post-owned event
publication.

#### Minimal package/class structure

```text
com.example.socialmedia.post
├── PostApplication.java
├── api/PostController.java       # public + internal routes and local records
├── application/
│   ├── PostService.java
│   └── CursorCodec.java
├── domain/
│   ├── Post.java
│   ├── PostLike.java
│   └── OutboxEvent.java
├── persistence/
│   ├── PostRepository.java     # explicit keyset/bulk queries
│   ├── PostLikeRepository.java # unique insert/query
│   └── OutboxRepository.java
├── integration/OutboxPublisher.java
└── config/
    ├── SecurityConfig.java
    └── CorrelationIdFilter.java
```

#### Key endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/posts` | Create immutable original post from JWT subject |
| POST | `/api/v1/posts/{parentPostId}/replies` | Create immutable reply after parent validation |
| GET | `/api/v1/posts/{postId}` | Return visible post and parent availability |
| DELETE | `/api/v1/posts/{postId}` | Author-only soft delete; emit deletion fact |
| PUT | `/api/v1/posts/{postId}/likes/me` | Idempotently create current user's like |
| GET | `/api/v1/users/{userId}/posts?cursor=&size=` | Visible authored posts by keyset |
| POST | `/internal/v1/posts/bulk` | Return visible post summaries for up to 100 IDs |
| GET | `/internal/v1/users/{userId}/post-count` | Count non-deleted authored posts |

#### Owned data fields

- `Post`: `id`, `authorId`, `text`, `publishedAt`, `parentPostId`, `parentAuthorId`,
  `deletedAt`.
- `PostLike`: `id`, `postId`, `userId`, `createdAt`; unique `(postId,userId)`.
- `OutboxEvent`: `eventId`, `aggregateId`, `eventType`, `schemaVersion`, `topic`,
  `messageKey`, `payload`, `occurredAt`, `correlationId`, `status`, `attemptCount`,
  `nextAttemptAt`, `publishedAt`, `lastError`, `createdAt`.
- Like count is an indexed count of `PostLike`; it is not a second mutable counter.

#### One core HLD/LLD concept

Immutable write model with database-enforced idempotent interactions and stable keyset reads.

#### Complexity ledger

| Added complexity | One-line justification |
|---|---|
| Separate `PostLike` row | Unique `(postId,userId)` is the source of truth for like-once behavior |
| Soft-delete timestamp | Replies must survive parent deletion and Timeline hydration must suppress deleted posts |
| Parent author snapshot | Reply notification can identify its recipient without a later cross-service lookup |
| Local cursor codec | Required deterministic keyset paging; deliberately not a shared pagination library |
| Three concrete outbox classes | Required atomic database/event handoff; duplicated in Follow instead of a custom starter |
| Separate Reply service/thread model | **SIMPLIFY**: nullable direct parent fields already satisfy reply behavior |
| Denormalized like counter | **SIMPLIFY**: unnecessary at the stated scale and creates consistency work |

### 3. Follow Service

**Boundary**: Own the current directional follower -> followed edge and answer set-based graph
queries without copying user records.

#### Minimal package/class structure

```text
com.example.socialmedia.follow
├── FollowApplication.java
├── api/FollowController.java       # public + internal routes and local records
├── application/
│   ├── FollowService.java
│   └── CursorCodec.java
├── domain/
│   ├── FollowRelationship.java
│   └── OutboxEvent.java
├── persistence/
│   ├── FollowRepository.java  # count + follower keyset queries
│   └── OutboxRepository.java
├── integration/
│   ├── UserClient.java        # concrete existence lookup
│   └── OutboxPublisher.java
└── config/
    ├── SecurityConfig.java
    └── CorrelationIdFilter.java
```

#### Key endpoints

| Method | Path | Purpose |
|---|---|---|
| PUT | `/api/v1/follows/{followedUserId}` | Idempotently create a non-self edge |
| DELETE | `/api/v1/follows/{followedUserId}` | Idempotently remove the edge |
| GET | `/internal/v1/users/{userId}/follow-counts` | Return follower and following counts together |
| GET | `/internal/v1/users/{userId}/followers?eligibleAt=&cursor=&size=` | Page current followers whose `followedAt <= eligibleAt` for Timeline fan-out |

#### Owned data fields

- `FollowRelationship`: `id`, `followerId`, `followedId`, `followedAt`; unique
  `(followerId,followedId)`.
- The active state is row presence. Unfollow deletes the row and writes an outbox event in the
  same transaction; re-follow creates a new relationship ID/time.
- `OutboxEvent`: same producer-owned fields as Post, duplicated locally by design.

#### One core HLD/LLD concept

Directional graph edges with set-based count/follower queries that avoid N+1 user lookups.

#### Complexity ledger

| Added complexity | One-line justification |
|---|---|
| One User existence call | A follow command must reject a nonexistent target immediately |
| Paged `eligibleAt` follower query | Bounds memory and prevents delayed post events from backfilling later followers |
| Local cursor codec | The high-follower demonstration must enumerate stable pages without offset drift |
| Local outbox | Follow notifications and unfollow cleanup must not be lost after domain commit |
| Graph database or copied user profiles | **SIMPLIFY**: one indexed edge table answers every current requirement |
| Persisted count table | **SIMPLIFY**: indexed exact counts avoid another consistency mechanism |

### 4. Timeline Service

**Boundary**: Own only precomputed user-to-post references and hydrate one page through one bulk
Post request; never copy Post display data or Follow graph state.

#### Minimal package/class structure

```text
com.example.socialmedia.timeline
├── TimelineApplication.java
├── api/TimelineController.java
├── application/
│   ├── TimelineService.java
│   └── CursorCodec.java
├── domain/TimelineEntry.java
├── persistence/TimelineRepository.java # concrete JdbcClient repository
├── integration/
│   ├── TimelineEventConsumer.java  # switch over four event types
│   ├── FollowClient.java
│   └── PostClient.java
└── config/
    ├── KafkaFailureConfig.java
    ├── SecurityConfig.java
    └── CorrelationIdFilter.java
```

#### Key endpoint and internal flow

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/timeline/home?cursor=&size=` | Keyset-page owned references, bulk hydrate, preserve order, omit missing/deleted posts |

- `post.published.v1`: page eligible current followers and batch `INSERT ... ON CONFLICT DO
  NOTHING` references in one local transaction.
- `post.deleted.v1`: idempotently delete all references for `postId`.
- `follow.removed.v1`: idempotently delete that owner/author's entries with
  `publishedAt <= unfollowedAt`; the time bound prevents a delayed old unfollow event from
  deleting posts created after a re-follow.
- `follow.created.v1`: explicit no-op because historical backfill is excluded.

#### Owned data fields

- `TimelineEntry`: `id`, `ownerUserId`, `postId`, `authorId`, `publishedAt`,
  `sourceEventId`, `createdAt`; unique `(ownerUserId,postId)`.
- Index `(ownerUserId,publishedAt DESC,postId DESC)` for reads and
  `(ownerUserId,authorId,publishedAt)` for cleanup.
- No text, username, reply label, like count, follow projection, or fan-out job state.

#### One core HLD/LLD concept

Fan-out-on-write: publication cost grows with follower count so home reads can use precomputed
references.

#### Complexity ledger

| Added complexity | One-line justification |
|---|---|
| Concrete `JdbcClient` repository | Set-based `ON CONFLICT` fan-out and keyset SQL are clearer than hiding them behind ORM behavior |
| Follow client in event handling | Follow remains the only graph owner and supplies eligible recipients in bounded pages |
| One Post bulk client per read page | Required to hydrate without copying content or making N downstream calls |
| One circuit breaker on Post bulk call | A failed dependency cannot produce a contract-valid page; bounded `503` is manually demonstrable |
| Kafka retry + consumer-specific DLT | Required bounded recovery for at-least-once fan-out and cleanup |
| Fan-out job table, distributed lock, follow projection, or celebrity hybrid | **SIMPLIFY**: horizontal fan-out scaling is explicitly not solved in this baseline |

### 5. Notification Service

**Boundary**: Materialize recipient-owned follow/reply notifications from Kafka and record event
identities so replay does not duplicate visible state.

#### Minimal package/class structure

```text
com.example.socialmedia.notification
├── NotificationApplication.java
├── api/NotificationController.java
├── application/
│   ├── NotificationService.java
│   └── CursorCodec.java
├── domain/
│   ├── Notification.java
│   └── ProcessedEvent.java
├── persistence/
│   ├── NotificationRepository.java
│   └── ProcessedEventRepository.java
├── integration/NotificationEventConsumer.java
└── config/
    ├── KafkaFailureConfig.java
    ├── SecurityConfig.java
    └── CorrelationIdFilter.java
```

#### Key endpoint and internal flow

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/notifications?cursor=&size=` | Return JWT subject's notifications newest first |

- `follow.created.v1`: create a FOLLOW notification unless actor equals recipient.
- `post.published.v1` with parent fields: create a REPLY notification unless actor equals parent
  author; an original post is an explicit no-op.
- In one database transaction, insert the notification (when applicable) and the processed event.
  Duplicate `eventId` exits without another visible row.

#### Owned data fields

- `Notification`: `id`, `recipientUserId`, `actorUserId`, `type`, `subjectId`,
  `parentPostId`, `eventTime`, `sourceEventId`, `availableAt`; unique `sourceEventId`.
- `ProcessedEvent`: `eventId`, `consumerName`, `eventType`, `processedAt`; unique
  `(consumerName,eventId)`.
- Persistence plus `availableAt` is the only delivery state. A one-value status enum and
  read/unread state are marked **SIMPLIFY**.

#### One core HLD/LLD concept

At-least-once asynchronous delivery with transactional idempotent consumption and visible
eventual consistency.

#### Complexity ledger

| Added complexity | One-line justification |
|---|---|
| `ProcessedEvent` | Explicitly required and makes duplicate/replay handling inspectable |
| Separate notification row | Kafka retention is not the user-facing query model |
| Cursor codec | Required deterministic newest-first retrieval without offset races |
| Kafka retry + consumer-specific DLT | Prevents a poison event from blocking notifications forever |
| Handler registry, templates, channels, read/unread, push/email | **SIMPLIFY**: a direct switch over two relevant facts is sufficient |

### 6. API Gateway

**Boundary**: Be the sole client edge for explicit routing, JWT validation, correlation, and one
small local rate-limit policy; own no domain state.

#### Minimal package/class structure

```text
com.example.socialmedia.gateway
├── GatewayApplication.java
├── GatewaySecurityConfig.java
├── RateLimitConfig.java        # official MVC Bucket4j/Caffeine beans
└── CorrelationIdFilter.java
resources/application.yml       # explicit lb:// routes; no discovery route locator
```

#### Key routes

| Gateway path | Target | Access |
|---|---|---|
| `/api/v1/auth/**`, `/api/v1/profiles/**` | User | Public |
| `/api/v1/users/me` | User | JWT |
| GET post/profile-post routes | Post | Public |
| Post writes/likes/replies | Post | JWT |
| `/api/v1/follows/**` | Follow | JWT |
| `/api/v1/timeline/**` | Timeline | JWT |
| `/api/v1/notifications/**` | Notification | JWT |

`/internal/**` is never routed. Login/registration and protected writes use a documented
per-instance token bucket; exhausted requests return `429`.

#### Owned data fields

None. Caffeine bucket entries are ephemeral process memory, not domain state.

#### One core HLD/LLD concept

Edge policy enforcement: authenticate once at the public boundary, route by logical service
name, and shed abusive bursts before domain work.

#### Complexity ledger

| Added complexity | One-line justification |
|---|---|
| Gateway Server Web MVC | Constitution-required servlet gateway and single public entry point |
| Local Bucket4j/Caffeine filter | Demonstrates rate limiting without adding Redis for one Gateway instance |
| Explicit YAML routes | Makes exposed paths reviewable and keeps internal contracts private |
| Distributed rate limits, WAF, CORS matrix, route DSL framework | **SIMPLIFY**: no multi-instance edge or browser client requirement exists |

### 7. Eureka Discovery Server

**Boundary**: Provide the one registry used by Gateway and synchronous service clients; own no
domain data.

#### Minimal package/class structure

```text
com.example.socialmedia.discovery
└── DiscoveryServerApplication.java
resources/application.yml
```

#### Key operational endpoints

- `/eureka/*`: service registration/discovery protocol.
- `/actuator/health`: startup/restart evidence.
- `/`: local dashboard for the reviewer only.

#### Owned data fields

None durable; registry state is ephemeral.

#### One core HLD/LLD concept

Logical-name service discovery independent of container addresses.

#### Complexity ledger

| Added complexity | One-line justification |
|---|---|
| One Eureka node | Explicit stakeholder choice and visible registry concept |
| Peers, zones, persistence, Consul, registry authentication | **SIMPLIFY**: local demo availability does not justify HA scaffolding |

## Contract and Interaction Design

### REST contracts changed

All routes and schemas are defined in [contracts/openapi.yaml](./contracts/openapi.yaml).

| Caller | Callee | Contract | Failure behavior |
|---|---|---|---|
| Client/Gateway | User | Auth, self, public profile | Validation/auth errors are `4xx`; profile is `503` if exact counts cannot be composed |
| User | Follow | Follow counts | Two-second timeout; profile returns `503`, never partial counts |
| User | Post | Post count | Two-second timeout; profile returns `503`, never partial counts |
| Follow | User | User existence | Two-second timeout; follow returns `503`, not a guessed outcome |
| Timeline consumer | Follow | Eligible follower pages | Failure throws to Kafka retry/DLT; no partial event acknowledgement |
| Timeline | Post | Bulk summaries | One call/page; breaker returns `503`; no partial page |

### Kafka contracts changed

The complete envelope/payload schemas are defined in
[contracts/events.asyncapi.yaml](./contracts/events.asyncapi.yaml).

| Topic | Key | Events | Consumers |
|---|---|---|---|
| `post-events.v1` | `postId` | `post.published.v1`, `post.deleted.v1` | `timeline-service-v1`, `notification-service-v1` |
| `follow-events.v1` | `followerId:followedId` | `follow.created.v1`, `follow.removed.v1` | `timeline-service-v1`, `notification-service-v1` |

Each topic has three local partitions, replication factor one, and seven-day retention. Each
consumer uses initial attempt + two one-second retries, then its own 30-day DLT. Unknown event
types go to DLT; known irrelevant types are explicit no-ops.

### Outbox and replay rules

1. Post/Follow commit domain and outbox rows together.
2. A single local publisher polls at most 100 due rows every second.
3. It marks `PUBLISHED` only after broker acknowledgement. A crash between acknowledgement and
   status update can duplicate a record by design.
4. Publish failures back off exponentially up to 60 seconds; attempt ten marks `FAILED`.
5. Published rows are deleted after seven days; failed rows are retained 30 days; pending rows
   are not age-purged.
6. Replay republishes the unchanged envelope and key to the source topic, preserving `eventId`
   and `correlationId`; no replay API or DLT UI is built.

## Primary End-to-End Flows

### Publish to home timeline

1. Gateway and Post validate JWT; Post takes `authorId` from `sub`.
2. Post commits `Post` plus `post.published.v1` outbox row.
3. Publisher sends the event keyed by `postId`.
4. Timeline asks Follow for current followers with `followedAt <= publishedAt` in stable pages.
5. Timeline batch-inserts duplicate-safe references.
6. Home read pages references, calls Post bulk once, restores order, and omits unavailable posts.

### Follow/unfollow and notification

1. Follow validates the target through User and creates/deletes the unique edge idempotently.
2. Only a real state transition writes `follow.created.v1` or `follow.removed.v1` to outbox.
3. Notification consumes create and commits notification + processed identity.
4. Timeline consumes remove and deletes only entries at or before `unfollowedAt`.

### Reply notification

1. Post validates the parent in its own database and snapshots `parentAuthorId`.
2. Reply and one `post.published.v1` event commit together; parent fields distinguish the reply.
3. Timeline treats the reply as an authored post; Notification uses the same fact to notify the
   parent author exactly once. No second reply-specific event is necessary.

## Post-design Constitution Re-check

| Gate | Result | Post-design evidence |
|---|---|---|
| Boundary/ownership | PASS | Data model has one database owner per entity and identifiers only across boundaries |
| Contracts | PASS | OpenAPI and AsyncAPI list every public/internal/event interaction and version |
| Stack | PASS | No reactive path, parallel registry, or independently pinned Spring component; Lombok is BOM-managed and compile-time only |
| Teaching/simplicity | PASS | Responsibility-based packages, per-service concept and complexity ledger above; Lombok is narrowly scoped and all generic/reusable scaffolding cut |
| Resilience | PASS | Exactly one circuit breaker and idempotency-aware Kafka retry; no HTTP retry |
| Observability/security | PASS | Correlation/JWT behavior appears in REST and event contracts; secrets excluded |
| Delivery/verification | PASS | Compose/manual quickstart only; no orchestration or automated-test artifact |

No gate violation or unresolved clarification remains.

## Complexity Tracking

Not applicable: the design has no constitution violation requiring an exception. Items that
start to resemble enterprise scaffolding are marked **SIMPLIFY** in the risk and per-service
ledgers and are not included in the baseline.
