# Phase 0 Research: SocialMedia Platform

**Date**: 2026-07-18  
**Status**: Complete; no unresolved `NEEDS CLARIFICATION` items remain.

## 1. Platform and dependency management

- **Decision**: Use Java 21, Spring Boot 4.1.0 (or a later 4.1.x patch at implementation time), and the Spring Cloud 2025.1.2 Oakwood BOM. Let the Boot and Cloud BOMs select Spring Framework, Spring Security, Hibernate, Gateway, Eureka, Kafka, and Resilience4j integration versions.
- **Rationale**: Spring Cloud 2025.1.2 is the first Oakwood service release that explicitly supports Spring Boot 4.1.x. BOM management is the standard Spring approach and avoids an independently pinned, incompatible stack.
- **Alternatives considered**: Manually pinning each Spring module was rejected as version-management scaffolding. Spring Cloud 2025.0.x was rejected because it targets the Boot 3.5 generation.
- **Sources**: [Spring Boot 4.1 release](https://spring.io/blog/2026/06/10/spring-boot-4/), [Spring Cloud compatibility table](https://spring.io/projects/spring-cloud/).

### Lombok integration decision

- **Decision**: Use Spring Boot's managed Lombok version as a compile-time/provided Maven dependency in each module that uses it, register it as an annotation processor in the centralized compiler configuration, and exclude it from executable artifacts. Use `@RequiredArgsConstructor` for Spring constructor injection and only targeted accessors/constructors for persistence classes. Keep REST and Kafka boundary shapes as Java records.
- **Rationale**: Lombok satisfies the clarified preference while remaining build-time-only. Narrow annotations remove repetitive constructors/accessors without hiding contracts or letting mutable JPA state, lazy relationships, credentials, or payload text leak through generated `toString`, `equals`, or `hashCode` methods.
- **Alternatives considered**: Hand-written constructors/accessors were not selected because the clarification explicitly chooses Lombok. `@Data`, `@Value` for JPA entities, entity-wide `@ToString`/`@EqualsAndHashCode`, Lombok builders for every type, experimental Lombok features, delombok/source-generation stages, and shipping Lombok at runtime were rejected as unsafe or unnecessary.
- **Sources**: [Lombok Maven setup](https://projectlombok.org/setup/maven), [Lombok constructor annotations](https://projectlombok.org/features/constructor), and [Lombok `@Data` behavior](https://projectlombok.org/features/Data).

## 2. Imperative programming model

- **Decision**: Use annotated Spring MVC controllers, JPA, synchronous `RestClient`, `spring.threads.virtual.enabled=true`, and `spring.main.keep-alive=true`. Configure explicit connect and read timeouts; do not define custom executor pools initially.
- **Rationale**: One blocking, top-to-bottom control flow is easy to trace and is the constitution-required model. Spring Framework 7 deprecates `RestTemplate` in favor of `RestClient`.
- **Alternatives considered**: WebFlux, Reactor types, `WebClient`, OpenFeign, HTTP proxy interfaces, and custom executor tuning were rejected because the local portfolio workload does not establish a measured need.
- **Sources**: [Spring Boot virtual threads](https://docs.spring.io/spring-boot/reference/features/spring-application.html), [Spring Framework REST clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html).

## 3. Service discovery and edge routing

- **Decision**: Run one Eureka server. Register all services and the Gateway as Eureka clients. Define an explicit Gateway Server Web MVC route list using `lb://service-name`; do not enable automatic discovery-route exposure.
- **Rationale**: Eureka is stakeholder-selected. Explicit routes make the public surface reviewable and prevent internal endpoints from being exposed by naming convention.
- **Alternatives considered**: Consul, hard-coded URLs, peer Eureka nodes, Config Server, and automatic DiscoveryClient route generation were rejected.
- **Sources**: [Spring Cloud Netflix Eureka](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/index.html), [Gateway Server Web MVC](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html).

## 4. Authentication and authorization

- **Decision**: User Service hashes passwords with BCrypt and issues a 30-minute RSA-signed JWT containing only `sub`, `iss`, `iat`, `exp`, and `jti`. Gateway and each domain HTTP service use Spring Security resource-server support to validate the public key, issuer, and expiry. Controllers derive the acting user from `sub`. The private key is mounted only into User Service; the public key is mounted where validation occurs.
- **Rationale**: Asymmetric signing makes the trust boundary clear: User can mint credentials, while other services can only verify them. Revalidation in each service keeps authorization correct if a service is reached inside the Compose network.
- **Alternatives considered**: Spring Authorization Server/OIDC, refresh tokens, shared HMAC secrets, sessions, custom JWT filters, and client-supplied user IDs were rejected.
- **SIMPLIFY**: Internal read-only endpoints are reachable only on the private Compose network and do not add service credentials, mTLS, or a policy engine. That is an explicit local-demo boundary, not a production security claim.

## 5. Persistence and schema ownership

- **Decision**: Use one local PostgreSQL container with five databases and five exclusive roles: `user_db`, `post_db`, `follow_db`, `timeline_db`, and `notification_db`. Each stateful service has one datasource, one Flyway `V1` migration, and `ddl-auto=validate`; no service can connect with another role.
- **Rationale**: This meets the selected local isolation constraint while keeping each database independently movable. One SQL migration per service is the smallest reproducible schema mechanism.
- **Alternatives considered**: Shared tables, cross-database foreign keys, a shared superuser at runtime, schema-per-service under one credential, multiple PostgreSQL containers, and custom migration wrappers were rejected.
- **Risk retained**: Five databases and migration histories are repetitive. They are retained because exclusive service ownership is a constitutional requirement; environment profiles and migration abstractions are cut.

## 6. Identifiers and pagination

- **Decision**: Generate IDs in the owning application with `uuid-creator` 6.1.1 using RFC 9562 UUIDv7. Encode cursors as Base64URL JSON `{ "v": 1, "sortTime": "...", "id": "..." }`; reject malformed or unsupported cursors with `400`. Each collection implements its tiny codec locally rather than importing a shared pagination library.
- **Rationale**: UUIDv7 and `(sortTime,id)` are selected constraints. The cursor is opaque to clients but easy to explain and inspect in an interview.
- **Alternatives considered**: UUIDv4, database sequences, offset pagination, signed/encrypted cursors, and a shared cursor framework were rejected. Cursor signing adds no required security property because the server validates every decoded value.
- **Source**: [uuid-creator 6.1.1 in Maven Central](https://central.sonatype.com/artifact/com.github.f4b6a3/uuid-creator/6.1.1).

## 7. Synchronous service contracts

- **Decision**: Use JSON over versioned `/api/v1` public routes and `/internal/v1` private-network routes. Use concrete `RestClient` wrapper classes only where a service calls another service. Contract records are local to each consumer; no DTO/entity JAR is shared.
- **Rationale**: Concrete clients protect an actual service boundary without creating interface-per-class ceremony. Versioned OpenAPI documents make dependencies explicit.
- **Alternatives considered**: Shared domain models, generated client libraries, GraphQL, gRPC, generic CRUD APIs, MapStruct, and a reusable HTTP-client framework were rejected.

## 8. Kafka topic and event design

- **Decision**: Use two producer-domain topics, `post-events.v1` and `follow-events.v1`, each with three partitions, replication factor one, and seven-day retention in the local environment. `post-events.v1` is keyed by `postId`; `follow-events.v1` is keyed by `followerId:followedId`. Events use a small JSON envelope: `eventId`, `eventType`, `schemaVersion`, `aggregateId`, `occurredAt`, `correlationId`, and `payload`.
- **Rationale**: Two explicit topics preserve per-post and per-follow-pair ordering and describe who owns each fact without inventing a generic event bus or Schema Registry.
- **Alternatives considered**: One topic per event type, one global topic, consumer-specific command topics, Kafka Streams, Spring Cloud Stream, Avro/Schema Registry, and a second broker were rejected.

### Event inventory

| Event | Producer | Consumers | Purpose |
|---|---|---|---|
| `post.published.v1` | Post | Timeline and Notification (`timeline-service-v1`, `notification-service-v1`) | Fan out a post; when parent fields are present, also create the reply notification |
| `post.deleted.v1` | Post | Timeline (`timeline-service-v1`) | Remove stale timeline references |
| `follow.created.v1` | Follow | Notification (`notification-service-v1`) | Create a follow notification |
| `follow.removed.v1` | Follow | Timeline (`timeline-service-v1`) | Remove that author's entries for the former follower |

## 9. Transactional outbox

- **Decision**: Post and Follow write a domain row and outbox row in the same local database transaction. One concrete `OutboxPublisher` per producer polls up to 100 due rows every second, publishes with `KafkaTemplate`, and marks the row `PUBLISHED` only after broker acknowledgement. A send failure updates `attemptCount`, `lastError`, and `nextAttemptAt` with exponential delays capped at 60 seconds; after ten attempts it marks the row `FAILED`. Published rows are removed after seven days; failed rows remain 30 days for manual inspection/requeue.
- **Rationale**: This closes the database-commit/message-publication gap using the mechanism selected in the spec. If broker acknowledgement succeeds but the status update fails, duplicate publication is allowed and consumers remain idempotent.
- **Alternatives considered**: Dual writes, distributed transactions, Debezium/CDC, a shared outbox starter, unbounded retries, and producer-side exactly-once claims were rejected.
- **Risk retained**: The baseline publisher assumes one running instance of each producer service. Leasing and `SKIP LOCKED` multi-instance coordination are marked **SIMPLIFY** because horizontal scaling is out of scope.

## 10. Consumer retries, dead letters, and replay

- **Decision**: Each service uses one record listener and consumer group across both source topics. A `DefaultErrorHandler` with `FixedBackOff(1000ms, 2)` performs the initial attempt plus two blocking retries, then publishes the original record to a consumer-specific DLT: `post-events.v1.timeline-dlt`, `follow-events.v1.timeline-dlt`, `post-events.v1.notification-dlt`, or `follow-events.v1.notification-dlt`. DLT retention is 30 days. No non-blocking retry topics are created. Timeline inserts are protected by unique `(ownerUserId,postId)` and cleanup is idempotent. Notification records and `processed_event` are committed together, with a unique `(consumerName,eventId)`.
- **Rationale**: Blocking retry preserves topic partition order. Consumer-specific DLTs prevent one consumer's failure from being confused with another's, while database uniqueness makes re-delivery and offset-reset replay safe.
- **Alternatives considered**: Exactly-once marketing claims, Kafka/database distributed transactions, retry-topic graphs, infinite retry, and a generic idempotency framework were rejected.
- **Replay**: Pause the target consumer, reset its consumer-group offsets within the seven-day topic retention window (or republish a DLT record), and resume it. Visible rows remain duplicate-free. Outbox `FAILED` rows may be manually reset to `PENDING` after correcting the cause.

## 11. Timeline fan-out and hydration

- **Decision**: On `post.published.v1`, Timeline pages through Follow's internal follower query using `eligibleAt=publishedAt`, then batch-inserts one reference per eligible follower. New follows are not backfilled. `follow.removed.v1` deletes existing `(ownerUserId,authorId)` entries. Timeline reads keyset-page owned references, make exactly one bulk Post call for all IDs, restore timeline order in memory, and omit missing/deleted posts.
- **Rationale**: This demonstrates fan-out-on-write directly: write work grows with follower count while reads use precomputed references. The `eligibleAt` predicate prevents a delayed publication event from including a user who followed after publication.
- **Alternatives considered**: Fan-out-on-read, storing full post copies in Timeline, one Post lookup per entry, historical backfill, Redis feeds, and a celebrity hybrid were rejected.
- **Known limitation**: One Kafka record performs work proportional to follower count and can be slow for celebrities. The 1,000-follower scenario demonstrates this; it does not solve it.

## 12. Resilience scope

- **Decision**: All REST calls have two-second connect/read bounds. Add one non-reactive Spring Cloud CircuitBreaker/Resilience4j breaker around Timeline's bulk Post lookup, with no retry and a `503` response when unavailable. Kafka listener retry handles the asynchronous Follow lookup separately. User profile composition fails as `503` if either count owner is unavailable rather than returning misleading partial counts.
- **Rationale**: Timeline cannot produce a valid hydrated page without Post, making it the clearest manually demonstrable breaker. Retrying reads at the HTTP layer adds latency without improving the local demonstration.
- **Alternatives considered**: Breakers on every client, fallbacks with stale data, retries, bulkheads, annotations/AOP, and a generic resilience policy were rejected.

## 13. Gateway rate limiting

- **Decision**: Use Gateway Server Web MVC's built-in Bucket4j rate-limit filter with a local Caffeine-backed bucket, keyed by authenticated subject or client address for public auth routes. Keep one documented local-demo policy; return `429` when exhausted.
- **Rationale**: Rate limiting is the Gateway's assigned teaching concept. The official MVC filter avoids a custom algorithm and avoids adding Redis solely for one local instance.
- **Alternatives considered**: Redis, a hand-written token bucket, distributed rate-limit claims, per-endpoint policy matrices, and feature-flagged policies were rejected.
- **Source**: [Gateway MVC RateLimiter filter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/filters/ratelimiter.html).

## 14. Observability and local runtime

- **Decision**: A small servlet filter in each HTTP service accepts or generates `X-Correlation-Id`, puts it in MDC, returns it, and propagates it through `RestClient` and event envelopes. Use structured console logs. Docker Compose runs five services, Gateway, Eureka, one PostgreSQL, one single-node KRaft Kafka broker, and a one-shot topic initializer.
- **Rationale**: Correlation IDs make the end-to-end REST/Kafka path demonstrable without a telemetry stack. KRaft avoids ZooKeeper.
- **Alternatives considered**: ELK, Zipkin, OpenTelemetry collectors, service mesh, Kubernetes, Helm, multiple Compose profiles/environments, and log-shipping infrastructure were rejected.

## 15. Verification strategy

- **Decision**: Provide `http/socialmedia.http`, the phase-1 quickstart, Actuator health endpoints, correlation-log checks, a 200-post cursor scenario, a 1,000-follower fan-out scenario, duplicate-event replay, one dependency failure, and independent service restart checks. Compilation, packaging, image builds, startup, and health checks remain engineering checks.
- **Rationale**: This satisfies the constitution's manual-verification-only rule and proves the system-design tradeoffs visibly.
- **Alternatives considered**: Unit, integration, contract, component, end-to-end, and load-test code; test dependencies; coverage tools; and CI test stages were all rejected.

## Enterprise-scaffolding cut list

The following items are marked **SIMPLIFY** and excluded from the baseline: shared starter libraries, shared DTO/entity modules, generic CRUD bases, single-implementation service interfaces, MapStruct, a generic exception hierarchy, Config Server, feature flags, environment profile matrices, Redis, Schema Registry, CDC, service mesh, mTLS/service accounts, Eureka HA, distributed tracing stacks, Kubernetes/Helm, autoscaling, CQRS/event sourcing, and automated tests.
