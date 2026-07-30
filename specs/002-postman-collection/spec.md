# Feature Specification: Postman API Collection

**Feature Branch**: `002-postman-collection`

**Created**: 2026-07-24

**Status**: Draft

**Input**: User description: "api in .http files looks old and boring way can we make postman collaction instead?"

## Clarifications

### Session 2026-07-24

- Q: What should happen to `http/socialmedia.http` once the Postman collection is primary? → A: Delete the entire `http/` directory after migration completes (no retained or deprecated `.http` surface).
- Q: What should collection scripts do during a run? → A: Capture-only (set tokens, IDs, cursors, and auth headers); no pass/fail response assertions—expected outcomes are documented for human verification.
- Q: How should the Postman collection be authored and maintained? → A: Hand-curated only—version-controlled collection and environment built and maintained by hand; not generated from OpenAPI as the primary artifact.
- Q: How should demos handle a second run when Alice/Bob already exist? → A: Login-first default—register once; re-runs start with login. Rotate environment emails/usernames only when deliberately re-demonstrating registration on retained data.
- Q: How should requests be organized inside the collection? → A: Happy path + domain folders—one ordered Happy Path (demo) folder plus domain folders (Auth, Posts, Follows & Timeline, Engagement, Notifications, Gateway limits, optional load) for deep and negative cases.

## User Scenarios & Verification *(mandatory)*

### User Story 1 - Explore and demo the platform API from a modern collection (Priority: P1)

A developer or interviewer demoing the SocialMedia platform opens a single importable API collection, selects a local environment (gateway base URL and demo users), and runs the happy-path flow: register or log in two users, follow, publish, read home timeline, like/reply, and read notifications—without relying on IDE-specific `.http` request files.

**Why this priority**: This is the core ask—replace the aging `.http` workflow with a Postman-style collection as the primary way people exercise the public API during demos and local verification.

**Independent Verification**: With the platform stack already running, import the collection and environment, run the ordered happy-path requests, and confirm each step returns the expected success responses and that later steps reuse tokens and IDs captured from earlier ones.

**Acceptance Scenarios**:

1. **Given** a running local platform and the collection imported into an API client that supports Postman collections, **When** the user selects the local environment and runs the ordered happy-path requests, **Then** they complete registration/login, social graph, post, timeline, engagement, and notification checks without editing request bodies for IDs or tokens by hand.
2. **Given** the collection and a documented environment template, **When** a new team member imports both for the first time, **Then** they can identify which variable is the gateway base URL and which variables hold demo credentials within two minutes of reading the collection description or quickstart pointer.
3. **Given** a successful login request in the collection, **When** subsequent authenticated requests run, **Then** capture-only scripts store the access token for later requests so the demo does not require pasting tokens by hand (humans still judge success from the response UI and request descriptions, not automated green/red assertions).

---

### User Story 2 - Reproduce contract and failure checks used today (Priority: P2)

A developer validating the platform against its known behaviors runs the same categories of checks currently covered by the version-controlled request file: validation errors, authorization denials, idempotent follow/like/delete, pagination/cursor rejections, deleted-parent reply behavior, notification ownership isolation, and rate-limit demonstration—organized so each scenario is easy to find by name or folder.

**Why this priority**: The existing request file is not only a happy path; it is the manual verification surface for platform behavior. Parity preserves confidence when retiring `.http` files.

**Independent Verification**: After the Happy Path folder, walk domain folders (auth, posts, follows/timeline, engagement, notifications, gateway limits); run one representative failure and one representative success request per area and confirm outcomes match the documented expected behavior (status and business meaning), without needing the old `.http` file.

**Acceptance Scenarios**:

1. **Given** the collection is complete, **When** a reviewer compares scenario coverage to the previous request file and quickstart flows, **Then** every publicly documented manual API scenario has a corresponding named request (or explicit “intentionally omitted” note with rationale).
2. **Given** a negative case such as wrong credentials, oversized page, or cross-user notification access, **When** the matching collection request is sent, **Then** the response demonstrates the expected denial or validation outcome in a form suitable for a live demo.
3. **Given** idempotent operations (repeat follow, repeat like, repeat delete), **When** the same request is run twice, **Then** the collection documents and demonstrates the stable success outcome without requiring extra setup steps beyond the documented prerequisites.

---

### User Story 3 - Onboard without IDE lock-in (Priority: P3)

A contributor who does not use the previous IDE HTTP client follows project docs, imports the collection, and completes the primary demo flow using Postman (or a compatible importer), treating the collection as the supported manual API surface.

**Why this priority**: Moving off IDE-specific `.http` files reduces friction for contributors and interviewers who already know Postman-style tools.

**Independent Verification**: Follow only the updated quickstart/import instructions (no reference to the retired `.http` path as the primary path), import the collection, and complete the happy path.

**Acceptance Scenarios**:

1. **Given** a clean machine with Postman (or compatible client) and a running stack, **When** the contributor follows the project’s documented import steps, **Then** they do not need the retired `.http` file to finish the primary demo.
2. **Given** project documentation that previously pointed at `http/socialmedia.http`, **When** a reader opens the current quickstart, **Then** they are directed to the Postman collection (and environment) as the primary manual request surface.

---

### Edge Cases

- What happens when demo users already exist from a previous run (duplicate registration)? Default re-run path is **login-first**: skip or expect registration to fail, run the login requests to refresh tokens, then continue the demo. Rotating environment emails/usernames (or resetting local data) is only for a deliberate full registration demo—not the normal path.
- What happens when the gateway base URL differs (different host/port)? The environment MUST isolate the base URL so requests do not hard-code a single machine-specific host.
- What happens when a token expires mid-demo? Document re-running login (or the auth folder) to refresh the token variable before protected calls.
- What happens when optional heavy scenarios (bulk timeline load, large follower fan-out, rate-limit exhaustion) are run on a resource-constrained machine? Those scenarios MUST be clearly separated from the core happy path so demos can skip them without breaking ordered variables.
- How is partial import handled (collection without environment)? Collection descriptions MUST list required variables so the user can recreate them manually.

## Service Boundary & Scope *(mandatory)*

- **Owning Service**: Developer experience / verification assets (not a runtime microservice). The collection exercises the existing public surface via the API Gateway; it does not own business data or change service contracts.
- **Boundary Rationale**: This feature replaces how humans invoke existing contracts for demos and manual checks; it does not introduce new domain behavior inside User, Post, Follow, Timeline, Notification, or Gateway services.
- **Affected Services**: None required to change runtime behavior. Documentation and verification assets that currently reference the `.http` collection are updated. Contract files (OpenAPI/AsyncAPI) remain the source of API shape unless a gap is found while building requests.
- **Owned Data**: Version-controlled request collection, environment template(s), and documentation updates that describe how to import and run them. No new production user data is owned.
- **Cross-Service Outcomes**: None beyond what the existing platform already produces when the same HTTP calls are made (auth tokens, posts, follows, timeline entries, notifications, rate-limit responses).
- **Out of Scope**:
  - Redesigning or extending product APIs solely for the collection
  - Automated CI test frameworks, load-test platforms, or replacing unit/integration tests
  - Postman Tests-tab (or equivalent) pass/fail assertions, Collection Runner as a test suite, or Newman/CI execution of the collection as automated tests
  - Postman Cloud team workspaces, paid Postman features, or mandatory cloud sync
  - Retaining, deprecating-in-place, or dual-maintaining any `.http` request surface after migration completes (the `http/` directory is removed)
  - GUI/mobile client development
  - Mock servers or contract-test generation pipelines (may be future work)
  - OpenAPI-to-Postman generation as the primary authoring path or multi-collection-per-service split for v1

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST provide a version-controlled, hand-curated Postman collection that covers the SocialMedia public API demo and manual verification flows previously driven by the `.http` request file. The collection MUST be maintained as a first-class authored artifact (not generated as the primary source from OpenAPI or other contracts).
- **FR-002**: The collection MUST be importable into Postman (Collection format supported by current Postman desktop/web import) without manual reconstruction of individual requests.
- **FR-003**: The project MUST provide a version-controlled environment template (or equivalent variable set) that includes at least: gateway base URL, two demo user credentials, and placeholders for captured user IDs and access tokens.
- **FR-004**: Requests MUST be organized with (1) a dedicated ordered **Happy Path** (or equivalently named demo) folder that a presenter can run top-to-bottom for the core story, and (2) clearly named **domain folders** (Auth, Users, Posts, Follows & Timeline, Engagement, Notifications, Gateway limits, optional load demos) holding deep and negative cases so a presenter can jump by product area without a flat list.
- **FR-005**: Authenticated flows MUST support automatic or scripted capture of access tokens and entity IDs (and pagination cursors where needed) from responses into collection/environment variables so later requests reuse them without hand-editing bodies or headers for each run. Scripts MUST be limited to capture, variable assignment, and attaching auth/correlation headers—they MUST NOT assert status codes, body fields, or otherwise produce automated pass/fail results.
- **FR-006**: The happy-path sequence MUST be ordered and documented so a user can run it top-to-bottom (or via an explicit folder order) to demonstrate the core product story end to end. Documentation MUST distinguish (1) first-time path including registration and (2) default re-run path that starts at login with existing demo credentials.
- **FR-007**: Negative and boundary scenarios that the previous request file used for verification MUST have corresponding requests (or an explicit documented omission with reason) including: duplicate identity, wrong credentials, validation failures, unauthorized access, malformed/oversized pagination, idempotent operations, deleted resource behavior, and notification ownership isolation.
- **FR-008**: Heavy or destructive demo scenarios (bulk data generation, rate-limit exhaustion) MUST be isolated from the core happy path so they are optional and do not block everyday demos.
- **FR-009**: Project quickstart and related verification docs that currently instruct users to open the `.http` file MUST be updated to treat the Postman collection as the primary manual API surface.
- **FR-010**: After the collection is complete and docs point to it, the project MUST delete the entire `http/` directory (including `socialmedia.http`) so no legacy IDE request-file surface remains to maintain or discover by accident.
- **FR-011**: Each request (or folder) MUST include a short description of purpose and expected outcome in business terms (success vs denial), suitable for interview/demo narration and as the sole verification guide for humans (not automated assertions).
- **FR-012**: Correlation or request-tracing headers used by the platform today for demo observability MUST remain available on collection requests where the previous file used them, so log-correlation demos still work.
- **FR-013**: The collection MUST not embed real production secrets; only local demo credentials and empty secret placeholders are allowed in version control.

### Key Entities

- **API Collection**: The importable set of named requests, folders, scripts, and descriptions that represent manual platform scenarios.
- **Environment Template**: Named variable set for a target deployment (initially local), including base URL, demo identities, and runtime-captured tokens/IDs.
- **Scenario**: A coherent demo or verification path (happy path, auth failures, timeline pagination, rate limit, etc.) composed of one or more ordered requests.
- **Captured Runtime Variable**: A value produced by one response (token, user id, post id, cursor) and consumed by later requests.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new contributor with a compatible API client and a running local stack completes the primary happy-path demo (two users, follow, post, timeline, engagement, notifications) in under 15 minutes using only the provided collection, environment template, and updated docs—without the retired request-file workflow. On a second attempt against the same data, they complete the same demo in under 10 minutes via the documented login-first re-run path without inventing new accounts.
- **SC-002**: 100% of quickstart-documented manual API steps map to named collection requests; a reviewer can check them off without discovering a missing step.
- **SC-003**: At least 95% of discrete scenarios present in the previous request file appear as named requests in the collection, or are listed in a short “not migrated” appendix with justification; zero silent drops of previously documented verification cases.
- **SC-004**: In a dry-run demo with three observers unfamiliar with the old file, at least two of three correctly identify where to change the base URL and where the **Happy Path** demo folder starts within one minute of opening the collection.
- **SC-005**: After migration, the repository has no `http/` directory, and project documentation has zero instructions that depend on `.http` request files (historical notes only if needed, without a live path).
- **SC-006**: A full happy-path collection run requires zero manual copy-paste of access tokens or resource IDs between requests when variable capture is enabled as documented.
- **SC-007**: The shipped collection contains no automated pass/fail test assertions; a reviewer inspecting scripts finds only capture/header helpers, and verification remains human review of responses against request descriptions.

## Assumptions

- “Postman collection” means a version-controlled collection file (and environment) that Postman can import; compatible clients that understand the same format are acceptable consumers but Postman remains the documented target.
- The existing public API contracts and gateway routing stay as they are; this feature is a presentation and verification packaging change, not an API redesign.
- Local Compose (or equivalent) remains the primary demo environment; additional environments (staging) may be added later but are not required for v1.
- Default demo identities (e.g., Alice/Bob) are stable across re-runs; operators re-login rather than always re-registering.
- Full parity with `http/socialmedia.http` scenario coverage is the default migration goal; optional bulk/rate-limit sections stay optional folders.
- Dual-maintenance of `.http` and Postman is temporary at most during the switch; the end state is Postman-only with the `http/` directory fully removed.
- Automated Newman/CLI pipeline in CI is out of scope for this feature (and would conflict with capture-only, manual-verification intent unless explicitly reopened later).
- Collection scripts exist only to reduce demo friction (tokens/IDs/headers), consistent with the project rule that verification is manual rather than automated test suites.
- Contributors may use free Postman features only; no dependency on paid Postman Cloud collaboration features.
- OpenAPI remains the contract source of truth for request/response shapes; the hand-curated collection is for runnable demos and manual checks, not a second contract authority and not an OpenAPI export pipeline.
- A single platform-wide collection (plus environment template) is preferred over one collection per microservice for interview/demo flow.
