# Feature Specification: SocialMedia Platform

**Feature Branch**: `001-mini-twitter-platform`

**Created**: 2026-07-18

**Status**: Ready for Implementation

**Input**: User description: "Build a lean, interview-ready SocialMedia platform with real service boundaries, account access, profiles, text posts, follows, a precomputed home timeline, asynchronous notifications, likes, and replies."

## Clarifications

### Session 2026-07-18

- Q: Which baseline scope should `/plan` target? → A: Keep the complete current scope, including likes and replies.
- Q: Which transport should carry timeline fan-out and notification events? → A: Apache Kafka for both event flows.
- Q: Which service-discovery mechanism should the baseline use? → A: Eureka as the sole service registry.
- Q: Should Notification Service own persisted state or remain stateless? → A: It owns a database for notifications and processed-event identities.
- Q: Which identifier and pagination convention should the baseline use? → A: Service-owned UUIDv7 identifiers with opaque keyset cursors based on `(sortTime, id)`.
- Q: Should the baseline explicitly demonstrate event-driven architecture, and at what scope? → A: Use a focused hybrid: REST for immediate operations and Kafka events for timeline fan-out and notifications.
- Q: Which service should compose public-profile fields with Follow-owned and Post-owned counts? → A: User Service synchronously composes the response through versioned Follow and Post REST queries.
- Q: How should event-producing services avoid losing Kafka events between domain commit and publication? → A: Use a transactional outbox with an application-managed scheduled publisher.
- Q: How should service-owned databases be isolated in the local portfolio environment? → A: Use one PostgreSQL instance with a separate database and exclusive role per stateful service.
- Q: Which demonstration surface should the baseline provide? → A: A version-controlled HTTP request collection and concise demonstration guide only.
- Q: How should Timeline turn a page of owned post references into displayable post data? → A: Perform one versioned bulk Post Service lookup per non-empty timeline page.
- Q: Should the Java implementation use Lombok to reduce boilerplate? → A: Yes. Use Lombok as a compile-time-only dependency for constructor injection and narrowly selected accessors, while keeping API/event DTOs as Java records and avoiding broad `@Data`, generated `toString`, or generated equality on JPA entities.
- Q: How should source files be organized within each domain service? → A: Use responsibility-based packages: `api`, `application`, `domain`, `persistence`, `integration`, and `config`, leaving only the service application entry point at the package root.
- Q: What should happen when users reply to their own posts? → A: Self-replies are allowed, but they MUST NOT create a notification.
- Q: Which consistency target should apply to asynchronous visibility? → A: Timeline fan-out, deletion cleanup, and unfollow cleanup MUST finish within 10 seconds in every documented local run; the 95% allowance applies only to notification visibility.
- Q: How should the 95% notification criterion be measured? → A: Run 20 documented attempts—10 follow and 10 reply—and require at least 19 notifications to become visible within 10 seconds.
- Q: How should first-time-reviewer success be verified? → A: Remove the percentage-based reviewer-success criterion because the repeatable demonstration criteria already cover documentation quality.
- Q: Which canonical project name should replace Mini Twitter? → A: Use `SocialMedia` as the display name and `socialmedia` as the technical slug and package namespace; the existing feature branch and specification directory retain their historical names.
- Q: How is the 280-character post limit counted? → A: Count Unicode code points; accept 1–280 code points.
- Q: Which registration validation limits apply? → A: Email ≤254 code points; username 3–30 ASCII letters, digits, or underscores; display name 1–80 Unicode code points; bio 0–160 Unicode code points; password 12–72 UTF-8 bytes.
- Q: Which measurable Gateway rate limits apply? → A: Per Gateway instance, allow 10 authentication attempts per minute per client address and 60 protected writes per minute per authenticated account; return `429` before domain routing when exhausted.

## User Scenarios & Verification *(mandatory)*

The scenarios in this section are manual acceptance descriptions for planning and portfolio demonstration. They do not require automated test code or an automated test suite.

### User Story 1 - Create and Access an Account (Priority: P1)

A visitor creates an account with an email address, unique username, and password, then signs in and receives a time-limited credential that can be used for protected actions. Anyone can view the resulting public profile without seeing private account information.

**Why this priority**: Identity is the prerequisite for every personalized action and establishes who owns posts, follows, likes, and notifications.

**Independent Verification**: Manually register a new account, sign in with the registered credentials, use the issued credential to retrieve the account's own profile, and retrieve that profile publicly by username.

**Acceptance Scenarios**:

1. **Given** an unused email address and username, **When** a visitor submits valid registration details, **Then** one account and public profile are created without exposing the password.
2. **Given** a registered account, **When** the user signs in with the correct password, **Then** the user receives a signed, expiring credential accepted for protected actions.
3. **Given** a registered account, **When** incorrect credentials are submitted, **Then** access is denied without revealing whether the email, username, or password was incorrect.
4. **Given** a public profile, **When** any visitor views it, **Then** the username, display name, bio, follower count, following count, and post count are visible, while email and security data are not.
5. **Given** an authenticated user submits a protected action containing another user's identifier, **When** the action is authorized, **Then** ownership is determined from the authenticated identity and the supplied identifier cannot impersonate another user.

---

### User Story 2 - Publish and Remove Text Posts (Priority: P1)

An authenticated user publishes a short text post. The post records its author and publication time, cannot be edited, and can only be deleted by its author.

**Why this priority**: Publishing is the core content-producing action and supplies the content consumed by profiles and timelines.

**Independent Verification**: Manually publish a valid post, retrieve it with its author and timestamp, verify no edit capability exists, delete it as the author, and confirm another user cannot delete it.

**Acceptance Scenarios**:

1. **Given** an authenticated user and text containing 1 to 280 Unicode code points, **When** the user publishes it, **Then** a post with the exact text, author, and publication time is created.
2. **Given** a published post, **When** its author deletes it, **Then** the post is no longer available in post, profile, or home-timeline views after the allowed consistency window.
3. **Given** a published post, **When** a different user attempts to delete it, **Then** deletion is denied and the post remains unchanged.
4. **Given** a published post, **When** a user looks for an edit action, **Then** no post-edit capability is available.

---

### User Story 3 - Follow People and Read a Home Timeline (Priority: P1)

An authenticated user follows another account and sees posts that account publishes afterward in a reverse-chronological, paginated home timeline. The user can unfollow the account to stop seeing its posts.

**Why this priority**: This is the primary end-to-end social flow and the central architecture demonstration for the portfolio project.

**Independent Verification**: Manually have one user follow a second user, publish posts as the second user, verify they appear in the first user's home timeline in stable reverse-chronological pages, then unfollow and verify the second user's posts cease appearing.

**Acceptance Scenarios**:

1. **Given** two distinct users with no follow relationship, **When** the first follows the second, **Then** one directional relationship is created and both public profile counts reflect it within 10 seconds.
2. **Given** a user follows an author, **When** that author publishes a post, **Then** a reference to the post is added to the follower's precomputed home timeline without requiring the follower to request the timeline.
3. **Given** followed authors have published multiple posts, **When** the user requests successive timeline pages, **Then** available posts appear newest first without duplicates or omissions caused by newly published posts.
4. **Given** a user unfollows an author, **When** the consistency window of 10 seconds has passed, **Then** that author's posts no longer appear in subsequent home-timeline requests and future posts are not distributed to that user.
5. **Given** an author already has posts, **When** a new follower follows the author, **Then** older posts are not backfilled and only posts published after the follow succeeds are eligible for the new follower's timeline.

---

### User Story 4 - Like and Reply to Posts (Priority: P2)

An authenticated user can like a post once and can reply with a new text post that references the original post. Readers can distinguish a reply from an original post.

**Why this priority**: These interactions make the social model demonstrable while reusing the existing post concept instead of adding unrelated feature breadth.

**Independent Verification**: Manually like a post twice and verify only one like is counted, then publish a valid reply and verify its parent reference and normal post attributes.

**Acceptance Scenarios**:

1. **Given** an existing post, **When** an authenticated user likes it, **Then** the post's like count increases by one and records that user's like.
2. **Given** the same user has already liked a post, **When** the user repeats the like action, **Then** the request has no additional effect and the like count does not increase again.
3. **Given** an existing post, **When** an authenticated user submits a valid reply, **Then** a new immutable post is created with a reference to its parent.
4. **Given** an existing reply, **When** it is displayed, **Then** readers can identify its author, text, publication time, and parent post.
5. **Given** a parent with existing replies, **When** the parent is deleted, **Then** the replies remain available and indicate that their parent is unavailable.

---

### User Story 5 - Receive Social Notifications (Priority: P2)

An authenticated user receives a notification when another user follows them or replies to one of their posts. The triggering follow or reply succeeds without waiting for notification delivery.

**Why this priority**: Notifications demonstrate decoupled side effects and eventual consistency without making a secondary feature block the primary action.

**Independent Verification**: Manually follow a user and reply to that user's post, verify both triggering actions complete before their notifications arrive, and then retrieve one notification of each type for the recipient.

**Acceptance Scenarios**:

1. **Given** one user follows another, **When** the follow succeeds, **Then** the followed user receives one notification identifying the follower and follow event, and that attempt contributes to the documented notification timing measurement.
2. **Given** a user replies to another user's post, **When** the reply succeeds, **Then** the original author receives one notification identifying the replier and reply, and that attempt contributes to the documented notification timing measurement.
3. **Given** notification delivery is delayed or temporarily unavailable, **When** a follow or reply is submitted, **Then** the triggering action still returns its own success or failure without waiting for notification creation.
4. **Given** the same follow or reply event is delivered more than once, **When** notifications are processed, **Then** the recipient sees only one notification for that event.
5. **Given** notifications for two different users, **When** either user retrieves notifications, **Then** only that user's notifications are returned newest first.

---

### User Story 6 - Demonstrate Independent Services and Timeline Tradeoffs (Priority: P1)

A portfolio reviewer can run the platform locally, identify the responsibility and owned data of each service, redeploy one service independently, trace an end-to-end user flow across service boundaries, and understand why timeline distribution favors fast reads while creating high write cost for accounts with many followers.

**Why this priority**: The project's purpose is to demonstrate explainable microservice and system-design decisions, not merely reproduce social features.

**Independent Verification**: Manually start the documented local environment, exercise the registration-to-timeline flow, stop and restart one domain service while the other processes remain running, and review the timeline tradeoff documentation and repeatable high-follower demonstration.

**Acceptance Scenarios**:

1. **Given** the complete local environment is running, **When** a user registers, signs in, follows an author, and the author publishes, **Then** the post reaches the follower's home timeline across independently deployable boundaries.
2. **Given** one domain service is stopped and restarted, **When** operators inspect the environment, **Then** unrelated services remain running and the restarted service resumes its owned capability without requiring their redeployment.
3. **Given** the architecture documentation, **When** a reviewer traces any primary flow, **Then** each data change has exactly one owning service and all cross-service interactions are explicitly identified.
4. **Given** the timeline demonstration and documentation, **When** follower count increases, **Then** the reviewer can observe and explain that publication work grows with follower count, while timeline reads use precomputed entries.
5. **Given** the documented celebrity scenario, **When** a reviewer assesses the baseline, **Then** it is explicitly presented as a known tradeoff rather than falsely claimed as solved.
6. **Given** one end-to-end operation, **When** a reviewer follows its correlation reference, **Then** the operation can be traced across every participating boundary without revealing a password, credential, or other secret.
7. **Given** an authentication or protected-write rate-limit bucket is exhausted, **When** another matching request reaches the Gateway, **Then** it returns `429` without routing to a domain service or changing domain state, and matching requests can succeed after the one-minute refill window.

### Edge Cases

- Registration rejects duplicate usernames or email addresses without creating a partial account; username and email uniqueness comparisons are case-insensitive.
- Registration rejects an invalid or greater-than-254-code-point email; a username outside 3–30 ASCII letters, digits, or underscores; a display name outside 1–80 Unicode code points; a bio longer than 160 Unicode code points; or a password outside 12–72 UTF-8 bytes. Password contents never appear in responses, logs, or public data.
- Expired, malformed, missing, or tampered credentials cannot perform protected actions.
- Empty posts, whitespace-only posts, and posts longer than 280 Unicode code points are rejected without changing post counts or timelines.
- A user cannot follow themselves; repeating an existing follow or removing a nonexistent follow has no additional effect on counts or notifications.
- A post and its reply may share the same timestamp resolution; ordering remains stable through a deterministic tie-breaker across pages.
- A reply cannot be created for a missing or deleted parent. If a parent is deleted later, existing replies remain available but indicate that their parent is unavailable.
- Replies authored by a followed account are eligible for the home timeline and are clearly labeled as replies.
- A deleted post cannot receive new likes or replies; stale timeline references never cause deleted content to be displayed.
- A user may reply to their own post; the reply follows normal post and timeline rules but creates no notification.
- Failed or duplicate timeline-distribution and notification events can be processed again without creating duplicate visible entries.
- The home timeline is empty, rather than erroneous, for a user who follows no one or whose followed accounts have not published since being followed.
- Temporary unavailability of a dependent service produces a bounded, understandable failure for the affected capability and does not expose internal details or secrets.
- Exceeding 10 authentication attempts per minute from one client address or 60 protected writes per minute from one authenticated account on a Gateway instance returns `429` before domain routing; rejected requests create no domain change, and the local bucket refills within one minute.

## Service Boundary & Scope *(mandatory)*

- **Owning Services**:
  - **User** owns accounts, credentials, public profile fields, and account access; it composes public-profile responses through versioned REST queries without taking ownership of Follow or Post counts.
  - **Post** owns original posts, replies, post deletion state, and likes.
  - **Follow** owns directional follow relationships.
  - **Timeline** owns each user's precomputed home-timeline entries and hydrates each non-empty page through one versioned bulk Post Service lookup without persisting Post-owned content.
  - **Notification** owns a dedicated database containing follow and reply notifications, their delivery state, and processed-event identities used for idempotent Kafka consumption.
  - **Gateway** provides the single client entry point and enforces access checks and local per-instance rate limits without owning domain data.
- **Boundary Rationale**: Each boundary isolates one interview-relevant responsibility and scaling concern: identity, write-heavy content, social graph relationships, feed distribution, asynchronous side effects, or edge routing and access control.
- **Package Organization Constraint**: Each domain service MUST place its application entry point at the service package root and organize all other source files into only the responsibility-based packages it needs: `api` for HTTP controllers and boundary records, `application` for concrete use-case services, `domain` for owned entities and domain rules, `persistence` for repositories, `integration` for REST clients and Kafka/outbox components, and `config` for security, correlation, and framework configuration. Empty packages, shared cross-service implementation packages, generic layers, and interfaces added only to satisfy the package layout are forbidden.
- **Affected Services**: Publishing affects Post and Timeline; following affects Follow, Timeline visibility, and Notification; replying affects Post, Timeline, and Notification. User Service composes profile display synchronously from User-owned fields, Follow-owned follower/following counts, and the Post-owned post count.
- **Owned Data**: Every domain record has one authoritative owner; other services retain only stable identifiers, derived counts, precomputed timeline entries, or event-processing state needed for their own responsibility.
- **Persistence Constraint**: The local environment uses one PostgreSQL instance with a separate database and exclusive credentials for each stateful service. Cross-database access is forbidden, and each service must remain movable to an independent instance without application-contract changes.
- **Cross-Service Outcomes**: Account access, post creation, follow changes, likes, and replies return an immediate outcome. A composed profile reflects each owning service's committed counts at query time. Timeline fan-out, post-deletion cleanup, and unfollow cleanup MUST become visible within 10 seconds in every documented local run. Notification creation retains its separate target of becoming visible within 10 seconds for at least 95% of documented attempts.
- **Baseline Scope Decision**: Likes and replies remain baseline capabilities alongside all six deployable responsibilities; neither is deferred to a stretch goal.
- **Messaging Constraint**: Apache Kafka carries timeline-distribution and notification events; synchronous commands and queries that require an immediate result remain REST-based. Each event-producing service atomically commits an outbox record with its domain change, and an application-managed scheduled publisher relays pending records to Kafka. The plan must define topics, keys, consumer groups, outbox retry and retention rules, at-least-once delivery, bounded retries, dead-letter handling, replay behavior, correlation metadata, and idempotent consumption without introducing a second broker or CDC platform.
- **Discovery Constraint**: Eureka is the sole service registry; Consul and parallel discovery mechanisms are excluded from the baseline.
- **Identity and Pagination Constraint**: Each owning service generates UUIDv7 identifiers for externally referenced domain records and events. Reverse-chronological collection APIs use opaque keyset cursors encoding `(sortTime, id)`; offset pagination is excluded.
- **Out of Scope**: Direct messages; media; search; hashtags; trending topics; reposts; password recovery; email verification; enterprise identity federation; browser, mobile, or native clients; moderation or administration; multi-region operation; automatic horizontal scaling; orchestration beyond a single local environment; automated unit, integration, contract, component, or end-to-end test code; and any claim that high-follower timeline fan-out is solved.

## Requirements *(mandatory)*

### Functional Requirements

#### Accounts and Profiles

- **FR-001**: The system MUST allow a visitor to register exactly one account using a unique valid email of at most 254 Unicode code points; a unique username of 3–30 ASCII letters, digits, or underscores; a display name of 1–80 Unicode code points; an optional bio of at most 160 Unicode code points; and a password of 12–72 UTF-8 bytes. *(Covered by Story 1.)*
- **FR-002**: The system MUST keep passwords and private account data out of public profiles, responses, operational records, and diagnostic output. *(Covered by Story 1 and edge cases.)*
- **FR-003**: The system MUST authenticate registered users and issue a signed, expiring credential that identifies the account for subsequent protected actions. *(Covered by Story 1.)*
- **FR-004**: The system MUST reject missing, expired, malformed, or invalid credentials for protected actions. *(Covered by Story 1 and edge cases.)*
- **FR-005**: User Service MUST expose a public profile containing username, display name, bio, follower count, following count, and post count, but not email or security data; it MUST obtain the counts through versioned synchronous REST queries to Follow and Post without persisting them as User-owned state. *(Covered by Story 1.)*
- **FR-006**: The system MUST ensure that all authorization decisions use the authenticated identity rather than an identity supplied in action data. *(Covered by Stories 1 and 2.)*

#### Posts and Interactions

- **FR-007**: An authenticated user MUST be able to create a text post containing 1 to 280 Unicode code points. *(Covered by Story 2.)*
- **FR-008**: Every post MUST expose a stable identifier, authenticated author, exact text, publication time, reply status, like count, and deletion availability. *(Covered by Stories 2 and 4.)*
- **FR-009**: A published post MUST be immutable; the system MUST provide deletion, but no editing capability. *(Covered by Story 2.)*
- **FR-010**: Only a post's author MUST be able to delete that post. *(Covered by Story 2.)*
- **FR-011**: Deleted posts MUST cease to appear in direct, profile, and timeline views within 10 seconds and MUST reject new likes and replies. *(Covered by Story 2 and edge cases.)*
- **FR-012**: An authenticated user MUST be able to like an existing post at most once, with repeated requests producing no additional like. *(Covered by Story 4.)*
- **FR-013**: An authenticated user MUST be able to create a reply that satisfies all normal post rules and references one existing parent post. *(Covered by Story 4.)*
- **FR-014**: Deleting a parent MUST NOT delete its replies; each remaining reply MUST indicate that its parent is unavailable. *(Covered by Story 4 and edge cases.)*

#### Following and Timeline

- **FR-015**: An authenticated user MUST be able to follow and unfollow any other existing user, but MUST NOT be able to follow themselves. *(Covered by Story 3.)*
- **FR-016**: Each follow relationship MUST be directional and unique for the follower-followed pair. *(Covered by Story 3.)*
- **FR-017**: Repeated follow and unfollow actions MUST be safe and MUST NOT corrupt relationship or profile counts. *(Covered by Story 3 and edge cases.)*
- **FR-018**: The system MUST update follower and following counts to match successful relationship changes within 10 seconds. *(Covered by Story 3.)*
- **FR-019**: Each authenticated user MUST have a home timeline containing eligible posts published by accounts they followed when those posts were created. *(Covered by Story 3.)*
- **FR-020**: The Timeline capability MUST implement fan-out-on-write by adding a new post reference to each current follower's precomputed timeline as a consequence of publication, independently of timeline reads, with the reference visible within 10 seconds in every documented local run. *(Covered by Stories 3 and 6.)*
- **FR-021**: The home timeline MUST return entries in deterministic reverse-chronological order using an opaque keyset cursor over `(publicationTime, postId)`, where `postId` is the UUIDv7 tie-breaker; the default page size is 20 and the maximum requested page size is 100. *(Covered by Story 3.)*
- **FR-022**: Timeline pagination MUST avoid duplicate or skipped entries when new posts are published between page requests. *(Covered by Story 3 and edge cases.)*
- **FR-023**: A new follow MUST apply only to posts published after that follow succeeds; historical backfill is excluded from the baseline. *(Covered by Story 3.)*
- **FR-024**: An unfollowed account's existing entries MUST cease appearing within 10 seconds, and no future post from that account may be distributed while the relationship is absent. *(Covered by Story 3.)*
- **FR-025**: Original posts and replies by followed authors MUST be eligible for the timeline, with replies visibly distinguished. *(Covered by Stories 3 and 4.)*

#### Notifications

- **FR-026**: A successful follow MUST cause exactly one notification for the followed user, identifying the follower and event time. *(Covered by Story 5.)*
- **FR-027**: A successful reply to another user's post MUST cause exactly one notification for the parent post's author, identifying the replier, reply, parent post, and event time; a reply to the authenticated user's own post MUST NOT create a notification. *(Covered by Story 5 and edge cases.)*
- **FR-028**: Notification creation MUST occur asynchronously so notification delay or temporary failure does not block or change the outcome of the triggering follow or reply. *(Covered by Story 5.)*
- **FR-029**: Duplicate delivery of a triggering event MUST NOT create duplicate visible notifications. *(Covered by Story 5.)*
- **FR-030**: An authenticated user MUST be able to retrieve their own notifications newest first and MUST NOT be able to retrieve another user's notifications. *(Covered by Story 5.)*

#### Architecture Demonstration and Operations

- **FR-031**: User, Post, Follow, Timeline, Notification, and Gateway responsibilities MUST be independently startable, stoppable, and redeployable, with domain data owned exclusively by its responsible service. *(Covered by Story 6.)*
- **FR-032**: Services MUST exchange only stable identifiers and explicit versioned interaction contracts; no service may access another service's private data store or internal domain model. *(Covered by Story 6.)*
- **FR-033**: The system MUST provide a repeatable local demonstration that starts all required components and exercises the registration, sign-in, post, follow, timeline, reply, and notification flows. *(Covered by Story 6.)*
- **FR-034**: The project MUST document the fan-out-on-write choice against fan-out-on-read, including read cost, publication amplification, consistency behavior, failure handling, and the high-follower or "celebrity" problem. *(Covered by Story 6.)*
- **FR-035**: The project MUST include a repeatable high-follower demonstration that makes fan-out work growth observable without claiming the baseline solves that scaling problem. *(Covered by Story 6.)*
- **FR-036**: Every end-to-end operation MUST expose a correlation reference that allows a reviewer to trace it across boundaries without recording passwords, credentials, or other secrets. *(Covered by Story 6 and edge cases.)*
- **FR-037**: The demonstration surface MUST consist only of a version-controlled HTTP request collection and concise demonstration guide; a server-rendered interface, SPA, or other web client is excluded from the baseline. *(Covered by Story 6 and Out of Scope.)*
- **FR-038**: Feature verification MUST use documented manual demonstration steps and architecture review checklists; the baseline MUST NOT include automated test code or automated test suites. *(Covered by Story 6 and Out of Scope.)*
- **FR-039**: The baseline MUST explicitly demonstrate a focused hybrid event-driven architecture: REST for commands and queries requiring immediate outcomes, and Kafka domain events for asynchronous timeline fan-out and notification creation. Other interactions MUST NOT be converted to events solely to claim broader event-driven coverage. *(Covered by Stories 3, 5, and 6.)*
- **FR-040**: Every domain change that must emit a Kafka event MUST atomically persist a versioned outbox event in the same service-owned database transaction; an application-managed scheduled publisher MUST relay pending events with retry-safe, at-least-once semantics. *(Covered by Stories 3, 5, and 6.)*
- **FR-041**: Timeline MUST hydrate each non-empty page of post references through exactly one versioned bulk request to Post Service, preserve timeline order, and omit missing or deleted posts without issuing one downstream request per entry. *(Covered by Stories 3 and 6.)*
- **FR-042**: Each Gateway instance MUST allow at most 10 authentication attempts per minute per client address and 60 protected writes per minute per authenticated account. An exhausted bucket MUST return `429` before domain routing, rejected requests MUST NOT change domain state, and the baseline MUST NOT require distributed rate-limit coordination. *(Covered by Story 6.)*

### Key Entities *(include if feature involves data)*

- **Account**: A registered identity with a unique email, unique username, protected password representation, account status, and credential metadata.
- **Public Profile**: Public-facing account information containing username, display name, bio, and derived follower, following, and post counts.
- **Post**: Immutable authored text with a stable identifier, publication time, deletion state, and optional parent reference; a reply is a post whose parent reference is present.
- **Like**: A unique relationship recording that one user likes one existing post.
- **Follow Relationship**: A unique directional edge from one user (follower) to another (followed), including when the relationship began.
- **Timeline Entry**: A derived reference connecting a user to an eligible post, with ordering and event identity needed for stable reads and duplicate prevention; it does not duplicate Post-owned display content.
- **Notification**: A recipient-owned follow or reply notice with actor, subject reference, event type, event time, and duplicate-prevention identity.
- **Outbox Event**: A producer-owned, transactionally persisted event envelope containing event identity, type, aggregate identity, versioned payload, occurrence time, correlation metadata, publication state, and retry metadata.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can register, sign in, follow an author, have that author publish, and see the new post in their home timeline in under 3 minutes during a clean local demonstration.
- **SC-002**: During documented manual verification, posts containing exactly 1 and 280 Unicode code points are accepted, a post containing 281 Unicode code points is rejected, combining-character and emoji examples demonstrate the counting rule, and no accepted post can be edited.
- **SC-003**: In a manually demonstrated timeline of at least 200 eligible posts, all traversed pages contain each eligible post exactly once in deterministic newest-first order even when new posts are added between page requests.
- **SC-004**: In a documented manual run of 20 notification attempts—10 successful follows and 10 successful replies—at least 19 notifications MUST become visible to the correct recipient within 10 seconds, while all 20 triggering actions MUST complete without waiting for notification visibility.
- **SC-005**: During documented manual authorization verification, every demonstrated attempt to delete another user's post, follow as another user, or read another user's notifications is denied without changing protected data.
- **SC-006**: Each of the six deployable responsibilities can be stopped and restarted independently during the local demonstration while all unrelated components remain running; its owned capability is usable again within 60 seconds of restart.
- **SC-007**: A repeatable demonstration using an author with at least 1,000 followers shows one precomputed timeline entry per eligible follower and produces no duplicate visible entries when distribution work is retried.
- **SC-008**: The architecture review identifies exactly one owner for every key entity and finds zero direct cross-service access to another owner's private data.
- **SC-009**: A reviewer can use the project documentation to explain within 5 minutes why precomputed timelines improve read work, why publication work grows with follower count, how eventual consistency appears to users, and why the celebrity problem remains an explicit baseline limitation.
- **SC-010**: During the documented event-driven demonstration, a reviewer can trace at least one REST-accepted command through its correlated Kafka event to the eventual Timeline or Notification result, then replay the event without creating duplicate visible state.
- **SC-011**: During a documented non-empty timeline-page request, correlation logs show exactly one bulk Post Service lookup for the page and zero per-entry Post Service requests; returned summaries retain timeline order and exclude deleted posts.
- **SC-012**: During documented manual verification, exceeding either Gateway rate limit produces `429` responses before domain routing, the rejected requests cause no domain-state changes, and matching requests succeed again after the one-minute refill window.

## Assumptions

- The baseline serves a portfolio demonstration in a single local environment; production-scale availability, capacity, and geographic distribution are not claimed.
- Account deletion, profile editing, email verification, password recovery, account blocking, privacy controls, and moderation are excluded because they do not advance the selected architecture concepts.
- Usernames and email addresses are case-insensitively unique; usernames are immutable in the baseline, while display name and bio are established at registration and may remain unchanged.
- Successful authentication issues one time-limited credential; refresh credentials, multi-factor authentication, federated identity, and session revocation are outside baseline scope.
- Timeline eligibility begins when a follow succeeds. Historical post backfill is deliberately excluded to keep the fan-out-on-write lesson focused.
- A hard 10-second maximum applies to timeline distribution, post-deletion cleanup, and unfollow cleanup in every documented local run. Notification visibility uses the separate 95% target defined in the success criteria; synchronously composed profile counts reflect their owning services' committed values at query time.
- Notification retrieval is required; read/unread state, delivery to email or mobile devices, and push delivery are excluded.
- Likes are idempotent and permanent in the baseline; removing a like is excluded because it does not add a new interview-relevant concept.
- Replies can target original posts or other replies, but the baseline exposes only the direct parent relationship and does not build threaded conversation ranking.
- Java and Spring versions remain governed by the project constitution. Kafka, Eureka, UUIDv7 identifiers, opaque keyset pagination, and logically isolated PostgreSQL databases are stakeholder-selected constraints; remaining credential, contract, and local-runtime details are selected during planning.
- No automated tests will be implemented. Focused verification required by the constitution will be performed through documented manual scenarios, the version-controlled HTTP request collection, and architecture review checklists.
- The project depends on a local environment with sufficient resources to run all independently deployable responsibilities and their required supporting components.
