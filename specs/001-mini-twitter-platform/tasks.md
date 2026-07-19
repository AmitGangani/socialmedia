# Tasks: SocialMedia Platform

**Input**: Design documents from `/specs/001-mini-twitter-platform/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Verification**: Automated test tasks are prohibited. Verification uses compilation, packaging,
container startup and health, the version-controlled HTTP collection, correlation logs, manual
failure/replay scenarios, and architecture review checkpoints.

**Organization**: Tasks are grouped by user story in specification priority order. User Story 1
includes the minimal Post- and Follow-owned count query surfaces required to demonstrate exact
public-profile composition without stubs or shared data.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel after its phase prerequisites because it changes different files
- **[Story]**: Maps the task to its specification user story
- Every task names the exact file or files it changes or validates

## Phase 1: Setup (Shared Project and Runtime Skeleton)

**Purpose**: Create the Maven modules, compile-time Lombok policy, deployable skeletons, and local
infrastructure definitions used by every story.

- [X] T001 Create the Java 21 Maven aggregator with Spring Boot 4.1.x parent, Spring Cloud 2025.1.2 BOM, module list, compiler annotation-processor configuration, and Boot repackage exclusion for Lombok in `pom.xml`
- [X] T002 Add repository-wide Lombok guardrails that stop configuration bubbling and reject broad `@Data`/experimental usage in `lombok.config`
- [X] T003 Add the Maven wrapper pinned for Maven 3.9+ packaging in `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties`
- [X] T004 [P] Create service module descriptors with only service-specific dependencies and provided Lombok where consumed in `services/user-service/pom.xml`, `services/post-service/pom.xml`, `services/follow-service/pom.xml`, `services/timeline-service/pom.xml`, and `services/notification-service/pom.xml`
- [X] T005 [P] Create edge and registry module descriptors in `gateway/pom.xml` and `discovery-server/pom.xml`
- [X] T006 Add minimal Spring Boot entry points without shared application code in `gateway/src/main/java/com/example/socialmedia/gateway/GatewayApplication.java`, `discovery-server/src/main/java/com/example/socialmedia/discovery/DiscoveryServerApplication.java`, and each `services/*/src/main/java/com/example/socialmedia/*/*Application.java`
- [X] T007 [P] Add lightweight reproducible container builds in `gateway/Dockerfile`, `discovery-server/Dockerfile`, `services/user-service/Dockerfile`, `services/post-service/Dockerfile`, `services/follow-service/Dockerfile`, `services/timeline-service/Dockerfile`, and `services/notification-service/Dockerfile`
- [X] T008 [P] Define external configuration names, generated-key exclusions, and local-secret exclusions in `.env.example` and `.gitignore`
- [X] T009 [P] Create idempotent initialization for five databases and five exclusive runtime roles in `infra/postgres/init-databases.sh`
- [X] T010 [P] Create the two three-partition source topics and four consumer-specific 30-day DLTs in `infra/kafka/create-topics.sh`
- [X] T011 Assemble PostgreSQL, single-node KRaft Kafka, topic initialization, Eureka, Gateway, and five independently restartable services with health-based ordering and private domain ports in `docker-compose.yml`

**Checkpoint**: The repository has buildable module and runtime skeletons; no domain behavior is implemented yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish discovery, externalized configuration, security, correlation, logging, and
service-owned schemas before story behavior is added.

**Critical**: Complete this phase before implementing any user story.

- [X] T012 Configure a non-client single-node Eureka registry, dashboard, and health endpoint in `discovery-server/src/main/java/com/example/socialmedia/discovery/DiscoveryServerApplication.java` and `discovery-server/src/main/resources/application.yml`
- [X] T013 [P] Configure User Service discovery, virtual threads, datasource/Flyway validation, actuator health, RSA key locations, and two-second REST bounds in `services/user-service/src/main/resources/application.yml`
- [X] T014 [P] Configure Post Service discovery, virtual threads, datasource/Flyway validation, Kafka producer, scheduler, actuator health, and JWT public key in `services/post-service/src/main/resources/application.yml`
- [X] T015 [P] Configure Follow Service discovery, virtual threads, datasource/Flyway validation, Kafka producer, bounded User REST client, actuator health, and JWT public key in `services/follow-service/src/main/resources/application.yml`
- [X] T016 [P] Configure Timeline Service discovery, virtual threads, datasource/Flyway validation, Kafka consumer group, bounded internal REST clients, actuator health, and JWT public key in `services/timeline-service/src/main/resources/application.yml`
- [X] T017 [P] Configure Notification Service discovery, virtual threads, datasource/Flyway validation, Kafka consumer group, actuator health, and JWT public key in `services/notification-service/src/main/resources/application.yml`
- [X] T018 Configure Gateway Server Web MVC with explicit `lb://` public routes, no discovery locator, no `/internal/v1` exposure, Eureka registration, virtual threads, and actuator health in `gateway/src/main/resources/application.yml`
- [X] T019 [P] Implement Gateway JWT validation, public-route allowlisting, protected-route enforcement, and denial responses in `gateway/src/main/java/com/example/socialmedia/gateway/GatewaySecurityConfig.java`
- [X] T020 [P] Configure one local Bucket4j/Caffeine rate-limit policy for authentication and protected writes with `429` exhaustion behavior in `gateway/src/main/java/com/example/socialmedia/gateway/RateLimitConfig.java`
- [X] T021 [P] Implement edge acceptance/generation and response propagation of `X-Correlation-Id` in `gateway/src/main/java/com/example/socialmedia/gateway/CorrelationIdFilter.java`
- [X] T022 [P] Implement resource-server JWT validation and JWT-subject extraction for User Service in `services/user-service/src/main/java/com/example/socialmedia/user/config/SecurityConfig.java`
- [X] T023 [P] Implement resource-server JWT validation and JWT-subject extraction for Post Service in `services/post-service/src/main/java/com/example/socialmedia/post/config/SecurityConfig.java`
- [X] T024 [P] Implement resource-server JWT validation and JWT-subject extraction for Follow Service in `services/follow-service/src/main/java/com/example/socialmedia/follow/config/SecurityConfig.java`
- [X] T025 [P] Implement resource-server JWT validation and JWT-subject extraction for Timeline Service in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/config/SecurityConfig.java`
- [X] T026 [P] Implement resource-server JWT validation and JWT-subject extraction for Notification Service in `services/notification-service/src/main/java/com/example/socialmedia/notification/config/SecurityConfig.java`
- [X] T027 Add servlet correlation filters that populate MDC, return the header, and never log credentials or payload text in `services/user-service/src/main/java/com/example/socialmedia/user/config/CorrelationIdFilter.java`, `services/post-service/src/main/java/com/example/socialmedia/post/config/CorrelationIdFilter.java`, `services/follow-service/src/main/java/com/example/socialmedia/follow/config/CorrelationIdFilter.java`, `services/timeline-service/src/main/java/com/example/socialmedia/timeline/config/CorrelationIdFilter.java`, and `services/notification-service/src/main/java/com/example/socialmedia/notification/config/CorrelationIdFilter.java`
- [X] T028 [P] Configure structured console fields for service, operation, and correlation ID with secret-safe defaults in each deployable's `src/main/resources/logback-spring.xml`
- [X] T029 [P] Create the Account schema and case-insensitive uniqueness constraints in `services/user-service/src/main/resources/db/migration/V1__user_schema.sql`
- [X] T030 [P] Create Post, PostLike, and producer-owned outbox tables, checks, indexes, and retention-query indexes in `services/post-service/src/main/resources/db/migration/V1__post_schema.sql`
- [X] T031 [P] Create FollowRelationship and producer-owned outbox tables with graph/count/follower-page indexes in `services/follow-service/src/main/resources/db/migration/V1__follow_schema.sql`
- [X] T032 [P] Create reference-only TimelineEntry storage with idempotency, keyset, and bounded-cleanup indexes in `services/timeline-service/src/main/resources/db/migration/V1__timeline_schema.sql`
- [X] T033 [P] Create Notification and ProcessedEvent storage with source-event and recipient-keyset constraints in `services/notification-service/src/main/resources/db/migration/V1__notification_schema.sql`
- [X] T034 Compile all modules without application tests and resolve configuration or annotation-processing failures through `pom.xml` and the affected module `pom.xml` files

**Checkpoint**: Discovery, build, security, logging, schemas, and runtime configuration are ready; story work may begin.

---

## Phase 3: User Story 1 - Create and Access an Account (Priority: P1) - MVP

**Goal**: Register an account, authenticate it, retrieve private self data, and expose a public
profile with exact Post- and Follow-owned counts and no security data.

**Independent Verification**: Run Quickstart Scenario 1: register Alice and Bob, log both in,
retrieve Alice's own account and Bob's public profile, reject case-insensitive duplicates and a
wrong password, and confirm every response carries a correlation ID without exposing hashes.

### Implementation for User Story 1

- [X] T035 [P] [US1] Implement the Account JPA entity with UUIDv7 identity, normalized uniqueness fields, targeted Lombok accessors, and a protected no-arg constructor in `services/user-service/src/main/java/com/example/socialmedia/user/domain/Account.java`
- [X] T036 [US1] Add normalized email/username lookup and uniqueness operations in `services/user-service/src/main/java/com/example/socialmedia/user/persistence/AccountRepository.java`
- [X] T037 [P] [US1] Implement the minimal Post entity mapping and non-deleted author count query needed for profile composition in `services/post-service/src/main/java/com/example/socialmedia/post/domain/Post.java` and `services/post-service/src/main/java/com/example/socialmedia/post/persistence/PostRepository.java`
- [X] T038 [P] [US1] Implement the minimal FollowRelationship mapping and exact follower/following count queries needed for profile composition in `services/follow-service/src/main/java/com/example/socialmedia/follow/domain/FollowRelationship.java` and `services/follow-service/src/main/java/com/example/socialmedia/follow/persistence/FollowRepository.java`
- [X] T039 [US1] Implement transactional registration, normalization, BCrypt hashing, uniform credential failure, and private/public projections in `services/user-service/src/main/java/com/example/socialmedia/user/application/AccountService.java`
- [X] T040 [P] [US1] Implement 30-minute RSA JWT issuance with only `sub`, `iss`, `iat`, `exp`, and `jti` claims in `services/user-service/src/main/java/com/example/socialmedia/user/application/JwtIssuer.java`
- [X] T041 [US1] Implement Bean-validated register and login contracts using local Java records and strict unknown-field rejection in `services/user-service/src/main/java/com/example/socialmedia/user/api/AuthController.java`
- [X] T042 [P] [US1] Expose the versioned internal exact follow-count response without exposing graph rows in `services/follow-service/src/main/java/com/example/socialmedia/follow/api/FollowController.java`
- [X] T043 [P] [US1] Expose the versioned internal non-deleted post-count response without exposing Post entities in `services/post-service/src/main/java/com/example/socialmedia/post/api/PostController.java`
- [X] T044 [US1] Implement two bounded Eureka-resolved RestClient calls that fail public-profile composition as `503` rather than returning partial counts in `services/user-service/src/main/java/com/example/socialmedia/user/integration/ProfileCountClient.java` and `services/user-service/src/main/java/com/example/socialmedia/user/UserApplication.java`
- [X] T045 [US1] Implement authenticated `/api/v1/users/me`, public `/api/v1/profiles/{username}`, and private-network user-existence contracts with local records in `services/user-service/src/main/java/com/example/socialmedia/user/api/ProfileController.java`
- [X] T046 [US1] Add account registration, login, self, public profile, duplicate identity, and wrong-credential requests with environment-captured IDs/JWTs in `http/socialmedia.http`
- [X] T047 [US1] Execute and record the account/profile manual evidence and expected response shapes under Scenario 1 in `specs/001-mini-twitter-platform/quickstart.md`

**Checkpoint**: User Story 1 is independently functional and is the suggested MVP.

---

## Phase 4: User Story 2 - Publish and Remove Text Posts (Priority: P1)

**Goal**: Let an authenticated author publish immutable 1-280-code-point posts, retrieve and page
them, and soft-delete only their own posts while atomically recording publication/deletion facts.

**Independent Verification**: Publish valid 1- and 280-code-point posts, reject blank and
281-code-point text, retrieve/page posts, confirm no edit route exists, reject another author's
delete, delete as owner, and confirm direct/profile views omit the post.

### Implementation for User Story 2

- [X] T048 [P] [US2] Implement the Post-owned outbox entity and due/status repository operations with targeted Lombok and no entity-wide string/equality generation in `services/post-service/src/main/java/com/example/socialmedia/post/domain/OutboxEvent.java` and `services/post-service/src/main/java/com/example/socialmedia/post/persistence/OutboxRepository.java`
- [X] T049 [P] [US2] Implement the local versioned Base64URL `(sortTime,id)` cursor codec with strict malformed/version rejection in `services/post-service/src/main/java/com/example/socialmedia/post/application/CursorCodec.java`
- [X] T050 [US2] Extend Post persistence with visible lookup, author-only soft deletion, deterministic author keysets, and bulk visible lookup methods in `services/post-service/src/main/java/com/example/socialmedia/post/persistence/PostRepository.java`
- [X] T051 [US2] Implement transactional original-post creation and deletion with UUIDv7 IDs, Unicode code-point validation, JWT-derived ownership, and same-transaction outbox records in `services/post-service/src/main/java/com/example/socialmedia/post/application/PostService.java`
- [X] T052 [US2] Extend public create/get/delete/profile-post and private bulk/count contracts with local Java records, page-size bounds, strict request fields, and Problem Details in `services/post-service/src/main/java/com/example/socialmedia/post/api/PostController.java`
- [X] T053 [US2] Implement the application-managed outbox poll/publish/ack flow with one-second polling, batches of 100, capped exponential retry, ten-attempt failure, sanitized errors, and retention cleanup in `services/post-service/src/main/java/com/example/socialmedia/post/integration/OutboxPublisher.java`
- [X] T054 [US2] Configure Post's Kafka JSON producer, scheduled publisher, UUIDv7 generation, and correlation propagation without a shared event library in `services/post-service/src/main/java/com/example/socialmedia/post/PostApplication.java`
- [X] T055 [US2] Add post boundary, listing, authorization, deletion, and no-edit requests to `http/socialmedia.http`
- [X] T056 [US2] Execute and record the standalone publish/retrieve/page/delete and validation evidence under Scenarios 4 and 6 in `specs/001-mini-twitter-platform/quickstart.md`
- [X] T057 [US2] Package Post Service without application tests and confirm the executable artifact excludes Lombok through `services/post-service/pom.xml`

**Checkpoint**: User Stories 1 and 2 work, and Post-owned events are durable even before consumers exist.

---

## Phase 5: User Story 3 - Follow People and Read a Home Timeline (Priority: P1)

**Goal**: Create unique directional follows, fan new post references out to eligible followers,
read a stable keyset-paginated hydrated timeline, and remove bounded history after unfollow.

**Independent Verification**: Follow twice, publish as the followed author, observe Timeline
visibility within 10 seconds, traverse 200 entries without gaps/duplicates using one bulk Post
lookup per page, unfollow twice, re-follow without backfill, and prove an old removal replay does
not remove a post published after re-follow.

### Implementation for User Story 3

- [X] T058 [P] [US3] Implement Follow's producer-owned outbox entity and repository with the documented lifecycle and no shared outbox module in `services/follow-service/src/main/java/com/example/socialmedia/follow/domain/OutboxEvent.java` and `services/follow-service/src/main/java/com/example/socialmedia/follow/persistence/OutboxRepository.java`
- [X] T059 [P] [US3] Implement Follow's local `(followedAt,followerId)` opaque follower-page cursor codec in `services/follow-service/src/main/java/com/example/socialmedia/follow/application/CursorCodec.java`
- [X] T060 [P] [US3] Implement a two-second Eureka-resolved User existence lookup with correlation propagation in `services/follow-service/src/main/java/com/example/socialmedia/follow/integration/UserClient.java`
- [X] T061 [US3] Extend Follow persistence with unique-edge lookup, idempotent delete, counts, and set-based eligible-follower keysets in `services/follow-service/src/main/java/com/example/socialmedia/follow/persistence/FollowRepository.java`
- [X] T062 [US3] Implement non-self follow, idempotent unfollow/re-follow, target validation, and same-transaction created/removed outbox facts in `services/follow-service/src/main/java/com/example/socialmedia/follow/application/FollowService.java`
- [X] T063 [US3] Extend public follow/unfollow and private eligible-follower page contracts with JWT-derived actor identity in `services/follow-service/src/main/java/com/example/socialmedia/follow/api/FollowController.java`
- [X] T064 [US3] Implement Follow's scheduled outbox publication, bounded retry/failure, retention cleanup, unchanged event identity, and correlation headers in `services/follow-service/src/main/java/com/example/socialmedia/follow/integration/OutboxPublisher.java` and `services/follow-service/src/main/java/com/example/socialmedia/follow/FollowApplication.java`
- [X] T065 [P] [US3] Implement the reference-only TimelineEntry model with UUIDv7 identity and targeted Lombok persistence annotations in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/domain/TimelineEntry.java`
- [X] T066 [US3] Implement JdbcClient batch `ON CONFLICT DO NOTHING`, post deletion, time-bounded author cleanup, and owner keyset reads in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/persistence/TimelineRepository.java`
- [X] T067 [P] [US3] Implement Timeline's local opaque `(publishedAt,postId)` cursor codec with default 20 and maximum 100 page rules in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/application/CursorCodec.java`
- [X] T068 [P] [US3] Implement paged eligible-follower retrieval with correlation propagation and no copied graph state in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/integration/FollowClient.java`
- [X] T069 [P] [US3] Implement one bulk Post request per non-empty page with a single Resilience4j circuit breaker, no retry/fallback, order restoration support, and bounded `503` failure in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/integration/PostClient.java`
- [X] T070 [P] [US3] Configure initial delivery plus two one-second Kafka retries and Timeline-specific DLT publication with sanitized failure metadata in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/config/KafkaFailureConfig.java`
- [X] T071 [US3] Consume all four known events with direct switching, fan out published posts through eligible follower pages, idempotently delete posts, time-bound unfollow cleanup, no-op follow creation, and reject unknown types to DLT in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/integration/TimelineEventConsumer.java`
- [X] T072 [US3] Implement home-page reference reads, exactly one bulk hydration call, order preservation, deleted/missing omission, and empty-page handling in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/application/TimelineService.java`
- [X] T073 [US3] Expose JWT-subject-owned `/api/v1/timeline/home` with opaque cursors, page bounds, and `503` dependency behavior in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/api/TimelineController.java`
- [X] T074 [US3] Configure Timeline's JSON event deserialization, consumer groups, UUIDv7 generation, RestClient interceptors, and named circuit breaker in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/TimelineApplication.java`
- [X] T075 [US3] Add follow/unfollow, home timeline, malformed cursor, page-size, re-follow, and bulk-hydration requests to `http/socialmedia.http`
- [X] T076 [US3] Execute and record fan-out, stable cursor traversal, one-bulk-call, unfollow, re-follow, and dependency-failure evidence under Scenarios 2, 3, 5, and 7 in `specs/001-mini-twitter-platform/quickstart.md`
- [X] T077 [US3] Package Follow and Timeline modules without application tests and resolve only production-build failures through `services/follow-service/pom.xml` and `services/timeline-service/pom.xml`

**Checkpoint**: The primary registration-follow-publish-timeline flow is functional across service boundaries.

---

## Phase 6: User Story 6 - Demonstrate Independent Services and Timeline Tradeoffs (Priority: P1)

**Goal**: Make service ownership, independent deployment, correlated REST/Kafka flow, and the
fan-out-on-write cost/read tradeoff directly demonstrable to a reviewer.

**Independent Verification**: Start the documented environment, trace one correlated publish to
Timeline, create at least 1,000 followers and observe one duplicate-safe reference per follower,
restart each responsibility independently, and complete the ownership/contract checklist with
zero cross-database or shared-model access.

### Implementation for User Story 6

- [ ] T078 [P] [US6] Add request-only data-runner sections for 200 timeline posts and 1,000 follower accounts without assertions or a test framework in `http/socialmedia.http`
- [ ] T079 [P] [US6] Add per-service health checks, persistent database storage, explicit private networking, and independent restart-safe service definitions in `docker-compose.yml`
- [ ] T080 [US6] Complete correlation propagation from Gateway through RestClient calls, Post/Follow outboxes, Kafka envelopes/headers, Timeline consumption, and bulk hydration in `gateway/src/main/java/com/example/socialmedia/gateway/CorrelationIdFilter.java`, `services/user-service/src/main/java/com/example/socialmedia/user/UserApplication.java`, `services/post-service/src/main/java/com/example/socialmedia/post/integration/OutboxPublisher.java`, `services/follow-service/src/main/java/com/example/socialmedia/follow/integration/OutboxPublisher.java`, and `services/timeline-service/src/main/java/com/example/socialmedia/timeline/TimelineApplication.java`
- [ ] T081 [P] [US6] Document the service boundary walkthrough, REST/Kafka hybrid, database ownership, fan-out-on-write/read comparison, failure behavior, and unsolved celebrity limitation in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T082 [US6] Execute and record the 1,000-follower amplification and duplicate replay evidence under Scenario 9 in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T083 [US6] Execute and record independent restart, Eureka re-registration, unrelated-service continuity, and 60-second recovery evidence under Scenario 10 in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T084 [US6] Complete the ownership, contract isolation, internal-route, reference-only Timeline, and forbidden-scaffolding review in `specs/001-mini-twitter-platform/quickstart.md`

**Checkpoint**: The architecture and Timeline tradeoff are reviewable without requiring a UI or orchestration platform.

---

## Phase 7: User Story 4 - Like and Reply to Posts (Priority: P2)

**Goal**: Add idempotent one-like-per-user behavior and immutable direct-parent replies that
survive later parent deletion and remain eligible for Timeline fan-out.

**Independent Verification**: Like one post twice and observe one row/count increment; create a
reply; delete the parent; retrieve the reply with unavailable-parent status; and reject new likes
or replies against the deleted parent.

### Implementation for User Story 4

- [ ] T085 [P] [US4] Implement PostLike with UUIDv7 identity, unique `(postId,userId)`, targeted Lombok persistence annotations, and count lookup in `services/post-service/src/main/java/com/example/socialmedia/post/domain/PostLike.java` and `services/post-service/src/main/java/com/example/socialmedia/post/persistence/PostLikeRepository.java`
- [ ] T086 [US4] Extend Post persistence for parent visibility, parent author snapshot, like insertion, and like counts without a denormalized counter in `services/post-service/src/main/java/com/example/socialmedia/post/persistence/PostRepository.java` and `services/post-service/src/main/java/com/example/socialmedia/post/persistence/PostLikeRepository.java`
- [ ] T087 [US4] Implement transactional idempotent like and immutable reply creation, deleted-parent rejection, parent survival semantics, and reply `post.published.v1` outbox payloads in `services/post-service/src/main/java/com/example/socialmedia/post/application/PostService.java`
- [ ] T088 [US4] Expose strict PUT-like and POST-reply contracts and parent-availability response fields in `services/post-service/src/main/java/com/example/socialmedia/post/api/PostController.java`
- [ ] T089 [US4] Preserve reply labels, parent references, timeline ordering, and one-call hydration in `services/timeline-service/src/main/java/com/example/socialmedia/timeline/integration/PostClient.java` and `services/timeline-service/src/main/java/com/example/socialmedia/timeline/application/TimelineService.java`
- [ ] T090 [US4] Add duplicate-like, reply, parent deletion, and deleted-parent rejection requests to `http/socialmedia.http`
- [ ] T091 [US4] Execute and record like idempotency, reply visibility, parent deletion, and Timeline eligibility evidence under Scenario 4 in `specs/001-mini-twitter-platform/quickstart.md`

**Checkpoint**: Likes and replies work without introducing another service, mutable post editing, or threaded ranking.

---

## Phase 8: User Story 5 - Receive Social Notifications (Priority: P2)

**Goal**: Materialize recipient-owned follow and reply notifications asynchronously and exactly
once visibly under at-least-once delivery and replay.

**Independent Verification**: Complete 10 successful follow attempts and 10 successful reply
attempts, confirm all commands return before notification visibility, record at least 19 of 20
matching notifications visible to the correct recipient within 10 seconds, replay source events,
and observe no duplicate visible rows.

### Implementation for User Story 5

- [ ] T092 [P] [US5] Implement Notification with FOLLOW/REPLY invariants, UUIDv7 identity, source-event uniqueness, recipient keyset ordering, and targeted Lombok persistence annotations in `services/notification-service/src/main/java/com/example/socialmedia/notification/domain/Notification.java`
- [ ] T093 [P] [US5] Implement the composite-key ProcessedEvent model for `notification-service-v1` event identities in `services/notification-service/src/main/java/com/example/socialmedia/notification/domain/ProcessedEvent.java`
- [ ] T094 [US5] Add notification keyset/source-event operations and processed-event duplicate detection in `services/notification-service/src/main/java/com/example/socialmedia/notification/persistence/NotificationRepository.java` and `services/notification-service/src/main/java/com/example/socialmedia/notification/persistence/ProcessedEventRepository.java`
- [ ] T095 [P] [US5] Implement Notification's local opaque `(eventTime,id)` cursor codec with malformed/version/page-size rejection in `services/notification-service/src/main/java/com/example/socialmedia/notification/application/CursorCodec.java`
- [ ] T096 [US5] Implement one-transaction notification plus processed-identity commits, self-notification no-ops, duplicate exits, and newest-first retrieval in `services/notification-service/src/main/java/com/example/socialmedia/notification/application/NotificationService.java`
- [ ] T097 [P] [US5] Configure initial delivery plus two one-second Kafka retries and Notification-specific DLT publication with sanitized failure metadata in `services/notification-service/src/main/java/com/example/socialmedia/notification/config/KafkaFailureConfig.java`
- [ ] T098 [US5] Consume follow-created and reply-bearing post-published facts, explicitly no-op irrelevant known events, and reject unknown types to DLT in `services/notification-service/src/main/java/com/example/socialmedia/notification/integration/NotificationEventConsumer.java`
- [ ] T099 [US5] Expose only JWT-subject-owned newest-first notification retrieval with no target-user input in `services/notification-service/src/main/java/com/example/socialmedia/notification/api/NotificationController.java`
- [ ] T100 [US5] Configure Notification event deserialization, consumer groups, UUIDv7 generation, and transaction boundaries in `services/notification-service/src/main/java/com/example/socialmedia/notification/NotificationApplication.java`
- [ ] T101 [US5] Add recipient notification retrieval, cross-user denial, consistency polling, DLT inspection, and unchanged-event replay requests/commands to `http/socialmedia.http`
- [ ] T102 [US5] Execute and record the 10-follow/10-reply timing table, recipient isolation, DLT handling, and duplicate replay evidence under Scenarios 4, 4A, 6, and 8 in `specs/001-mini-twitter-platform/quickstart.md`

**Checkpoint**: Follow and reply side effects remain asynchronous and duplicate-safe.

---

## Phase 9: Polish and Cross-Cutting Validation

**Purpose**: Validate the complete system against contracts, security constraints, operational
behavior, and the constitution without adding speculative infrastructure.

- [ ] T103 [P] Reconcile implemented request/response status codes, required fields, cursor rules, and internal-only operations with `specs/001-mini-twitter-platform/contracts/openapi.yaml`
- [ ] T104 [P] Reconcile implemented topic keys, envelopes, payloads, consumer groups, retry/DLT metadata, and replay identity rules with `specs/001-mini-twitter-platform/contracts/events.asyncapi.yaml`
- [ ] T105 Package every module without application tests, inspect executable contents to confirm Lombok is absent at runtime, and record any build corrections in `pom.xml` and affected module `pom.xml` files
- [ ] T106 Build and start all images, verify PostgreSQL/Kafka health ordering and Eureka registrations, and record environment corrections in `docker-compose.yml`
- [ ] T107 Execute the complete registration-login-follow-publish-timeline-reply-notification flow in `http/socialmedia.http` and reconcile the runnable steps in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T108 Execute authorization, strict-input, Unicode boundary, malformed cursor, and invalid-JWT scenarios and record evidence in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T109 Execute Timeline's stopped-Post timeout/circuit-breaker/recovery scenario and record bounded `503` plus no-partial-page evidence in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T110 Execute Kafka DLT inspection and unchanged-event duplicate replay for Timeline and Notification and record correlation/event identity evidence in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T111 Review production sources for shared models, cross-database access, generic repositories, single-use interfaces, mapper frameworks, reactive code, secret logging, and automated-test artifacts; record the result in `specs/001-mini-twitter-platform/quickstart.md`
- [ ] T112 Perform the final post-implementation constitution and service-ownership review and record all manual acceptance outcomes in `specs/001-mini-twitter-platform/quickstart.md`

**Checkpoint**: The complete baseline is packageable, locally reproducible, manually demonstrated, and constitution-compliant.

---

## Dependencies and Execution Order

### Phase Dependencies

- **Phase 1 - Setup**: No dependencies; begin here.
- **Phase 2 - Foundational**: Depends on Phase 1 and blocks all user-story work.
- **Phase 3 - US1**: Starts after Foundational and supplies real identities/JWTs for later manual flows.
- **Phase 4 - US2**: Starts after Foundational; manual authorization evidence uses US1 identities.
- **Phase 5 - US3**: Depends on US1 users and US2 post/outbox behavior.
- **Phase 6 - US6**: Core architecture demonstration depends on US1-US3; its request runners can be authored earlier.
- **Phase 7 - US4**: Depends on US1 and US2; Timeline reply visibility additionally uses US3.
- **Phase 8 - US5**: Depends on US3 follow events and US4 reply-bearing post events.
- **Phase 9 - Polish**: Depends on all selected stories.

### User Story Dependency Graph

```text
Setup -> Foundational
Foundational -> US1 -> US2 -> US3 -> US6
                     |      |
                     +----> US4
US3 + US4 ----------------> US5
US1 + US2 + US3 + US4 + US5 + US6 -> Polish
```

US2 implementation may begin alongside US1 after Foundational, but its protected manual checks
need US1 credentials. US6 remains a P1 story because it proves the project's primary architecture;
its complete cross-capability walkthrough is repeated in Polish after US4 and US5 are available.

### Within Each User Story

- Schema/model and repository work precedes service behavior.
- Service behavior precedes controller or consumer integration.
- Producer events precede consumers of those facts.
- HTTP collection updates precede manual execution and evidence recording.
- Compilation/packaging and manual verification complete the story checkpoint.

## Parallel Opportunities

- Setup infrastructure definitions T004-T010 can be developed concurrently after T001-T003.
- Foundational service configuration, security, migrations, and logging tasks marked `[P]` can be split by module.
- US1 Account modeling T035, Post count model/query T037, Follow count model/query T038, and JWT issuance T040 can proceed concurrently; T036 follows T035.
- US2 outbox modeling T048 and cursor work T049 can proceed concurrently before service assembly.
- US3 Follow-side and Timeline-side models/clients/configuration marked `[P]` can proceed concurrently before T071-T074 integrate them.
- US6 request runners, Compose operational hardening, and architecture documentation T078-T081 can proceed concurrently.
- US4 PostLike modeling T085 can begin while the earlier Post behavior is being reviewed.
- US5 models T092-T093, cursor T095, and DLT configuration T097 can proceed concurrently; repositories T094 follow the models before consumer/service assembly.
- Contract reconciliation T103-T104 can run concurrently before integrated validation.

## Parallel Execution Examples

### User Story 1

```text
T035 Account entity | T037 Post count model/query | T038 Follow count model/query | T040 JWT issuer
```

### User Story 2

```text
T048 Post outbox model/repository | T049 Post cursor codec
```

### User Story 3

```text
T058 Follow outbox | T059 Follow cursor | T060 User client | T065 Timeline model | T067 Timeline cursor | T068 Follow client | T069 Post client | T070 Kafka failure config
```

### User Story 6

```text
T078 HTTP data runners | T079 Compose operational definitions | T081 Architecture walkthrough
```

### User Story 4

```text
T085 PostLike model/repository can be implemented while reviewing existing Post cursor and outbox behavior before T086-T089 integration.
```

### User Story 5

```text
T092 Notification model | T093 ProcessedEvent model | T095 cursor codec | T097 Kafka failure config
```

## Implementation Strategy

### MVP First: User Story 1

1. Complete Setup and Foundational phases.
2. Complete T035-T047, including the real owner-provided zero-count endpoints.
3. Stop and execute Quickstart Scenario 1.
4. Demo registration, login, self data, and exact public profile composition as the MVP.

### Incremental Delivery

1. Add US2 to establish immutable Post writes and durable outbox facts.
2. Add US3 to complete the primary follow-to-precomputed-Timeline flow.
3. Add US6 evidence to make boundaries and fan-out tradeoffs reviewable.
4. Add US4 likes/replies without changing the service topology.
5. Add US5 asynchronous notifications and replay-safe consumption.
6. Run Polish validation across all stories and contracts.

### Parallel Team Strategy

After the Foundational checkpoint, one stream can complete US1 identity/profile work while another
starts US2 Post modeling. Once their contracts stabilize, separate streams can implement Follow
production and Timeline consumption, then converge for US3. US4 and the US6 demonstration surface
can proceed concurrently; US5 follows after both follow and reply facts exist.

## Notes

- `[P]` means different files and no dependency on another incomplete task in its parallel set.
- Lombok is compile-time only: use `@RequiredArgsConstructor` and targeted persistence accessors;
  keep boundary DTOs as records and do not add `@Data` or generated entity string/equality methods.
- Do not add test source, test-only dependencies, test runners, mocks, coverage, or CI test stages.
- Do not add shared DTO/entity modules, generic CRUD bases, mapper frameworks, Config Server,
  Redis, Schema Registry, CDC, tracing stacks, Kubernetes, or speculative extension points.
- Preserve real local data during ordinary restart validation; destructive volume resets require
  an explicit separate action.
