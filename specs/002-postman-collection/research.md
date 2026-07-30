# Research: Postman API Collection

**Feature**: `002-postman-collection` | **Date**: 2026-07-24

## 1. Collection format and files

- **Decision**: Ship **Postman Collection Format v2.1** as
  `postman/SocialMedia.postman_collection.json` and a companion environment as
  `postman/Local.postman_environment.json`. Document Postman desktop/web import as the
  supported path; compatible importers are optional consumers.
- **Rationale**: v2.1 is the common import target, version-controls cleanly as pretty-printed
  JSON, and matches the user’s request for a Postman collection “instead of” `.http` files.
- **Alternatives considered**:
  - Collection v2.0 — older; no benefit.
  - Bruno/Insomnia-native formats — not the requested target.
  - OpenAPI-generated collection as primary — rejected in clarify (hand-curated only).
  - Multiple per-service collections — worse for one-shot platform demos.

## 2. Variable scope (environment vs collection)

- **Decision**: Put **stable demo config** in the environment (`gateway`, Alice/Bob emails,
  usernames, passwords, rate-limit demo account). Put **runtime captures** also in the
  environment (`aliceId`, `bobId`, `aliceJwt`, `bobJwt`, post IDs, cursors, etc.) so a re-import
  of the collection does not wipe session state mid-demo. Collection-level description lists
  required variable names for partial-import recovery.
- **Rationale**: Mirrors how the `.http` file used globals for IDs/JWTs while keeping host and
  credentials environment-specific. Single Local environment covers Compose default.
- **Alternatives considered**: Collection variables only (harder multi-host later); globals only
  (easy to clobber across projects).

## 3. Capture-only scripts (constitution alignment)

- **Decision**: Use Postman **Tests** (post-response) scripts solely for
  `pm.environment.set(...)` from `pm.response.json()`. Optionally use **Pre-request** scripts
  only for dynamic correlation IDs or header attachment. **Never** call `pm.test`,
  `pm.expect`, or status/body assertions. Expected outcomes live in request/folder
  descriptions for human verification.
- **Rationale**: Spec FR-005/SC-007 and constitution principle X forbid automated test suites;
  capture scripts are demo friction reduction, not a test harness.
- **Alternatives considered**: Full Tests-tab assertions; Newman CI; Collection Runner as CI —
  all out of scope.

## 4. Folder layout

- **Decision**:

  | Folder | Role |
  |--------|------|
  | `00 Happy Path` | Ordered first-time/register or re-run/login demo story |
  | `01 Auth & Users` | Register/login/self/profile + identity failures |
  | `02 Posts` | Publish, list, delete, validation, authz, no-edit |
  | `03 Follows & Timeline` | Follow/unfollow/refollow, home pages, cursor errors |
  | `04 Engagement` | Like, reply, deleted parent, self-reply |
  | `05 Notifications` | Poll pages, ownership isolation, cursor errors |
  | `06 Gateway Limits` | Rate-limit account, exhaust, refill (optional everyday skip) |
  | `07 Optional Load` | 200-post seed, 1k-follower fan-out (optional) |
  | `08 Ops Notes` | Requests that need compose stop/start or non-Gateway paths, with long descriptions |

- **Rationale**: Clarified “Happy Path + domain folders”; numbered prefixes keep demo order
  obvious (SC-004). Heavy/rate-limit isolation matches FR-008.
- **Alternatives considered**: Flat list (old `.http` feel); scenario-only folders without domain
  browse; multi-collection split.

## 5. Happy Path contents and re-run

- **Decision**: Happy Path includes: Register Alice/Bob (first-time), Login Alice/Bob,
  Alice follows Bob, Bob publishes interaction parent, Alice home timeline poll, Alice replies,
  Bob notifications poll. Folder description documents **login-first re-run**: skip register
  (or ignore 409), run logins, continue. Environment keeps stable Alice/Bob credentials.
- **Rationale**: Matches clarify session and SC-001 second-run target.
- **Alternatives considered**: Always-unique identities; mandatory DB wipe; login-only collection.

## 6. Parity migration from `http/socialmedia.http`

- **Decision**: Treat every `###` HTTP request section as a named collection request unless
  listed in the scenario inventory as **doc-only** (Kafka DLT/replay shell steps) or
  **ops-manual** (compose stop post-service). Target ≥95% named-request parity (SC-003).
  Track mapping in [contracts/scenario-inventory.md](./contracts/scenario-inventory.md).
- **Rationale**: Spec requires zero silent drops; inventory makes review objective.
- **Alternatives considered**: Happy-path-only collection (fails FR-007); dual-maintain `.http`
  (fails FR-010).

## 7. Bulk and rate-limit iteration (IntelliJ → Postman)

- **Decision**:
  - **Rate-limit exhaust**: Prefer a small set of explicit sequential requests *or* document
    Collection Runner with a simple data file / iteration count for 11 auth probes and 61
    follow writes—**without** assertion scripts. Folder description states burst timing and
    60s refill waits.
  - **200 posts / 1,000 followers**: Document as optional Runner data-file scenarios; if Runner
    UX is too heavy for hand maintenance, ship a **reduced documented subset** plus inventory
    note that full volume remains an ops script outside the everyday collection (must not
    silently omit—either implement Runner data files or document omission with reason per
    FR-007). Prefer implementing Runner-oriented folders with clear “optional load” labeling
    and sample data files under `postman/data/` if needed for iterations.
- **Rationale**: IntelliJ collection expansion does not map 1:1 to Postman; constitution still
  forbids turning this into automated tests—Runner is a manual bulk sender only.
- **Alternatives considered**: External shell scripts as primary (weaker single-tool demo);
  dropping bulk scenarios without inventory note (violates SC-003).

## 8. Internal and non-Gateway steps

- **Decision**: Keep **internal bulk post lookup** (`/internal/v1`) as an Ops Notes request
  pointed at `postService` environment variable (default empty/off), with description that
  Gateway does not route it—private network only. Keep **Kafka DLT/replay** as Markdown in
  quickstart + folder description, not fake HTTP requests.
- **Rationale**: Preserves parity of verification intent without inventing public routes.
- **Alternatives considered**: Dropping internal/Kafka evidence (weakens platform demos);
  adding Gateway routes solely for demos (out of scope API change).

## 9. Documentation update strategy

- **Decision**: Feature [quickstart.md](./quickstart.md) is the import/run authority for this
  feature. Update `specs/001-mini-twitter-platform/quickstart.md` (and any root README if
  present) so live operator steps point to `postman/` instead of `http/socialmedia.http`.
  Leave historical completed tasks under 001 as past-tense history; do not rewrite entire 001
  task history. After collection is complete, **delete `http/`**.
- **Rationale**: FR-009/FR-010/SC-005; avoids dual primary surfaces.
- **Alternatives considered**: Deprecate-in-place `.http`; only update 002 docs (leaves
  stale primary path in 001 quickstart).

## 10. Authoring workflow

- **Decision**: Hand-edit pretty-printed JSON in-repo (or export from Postman after local edit
  and reformat for stable diffs). No OpenAPI→Postman pipeline. Prefer stable request `name`
  strings matching inventory. Avoid cloud sync/workspace IDs in committed files.
- **Rationale**: Clarify hand-curated; free Postman only; reviewable git diffs.
- **Alternatives considered**: Generated primary artifact; binary/workspace sync.

## 11. Correlation IDs

- **Decision**: Set `X-Correlation-Id` header on requests that previously had scenario-tagged
  IDs; use static scenario strings or a pre-request `pm.variables.replaceIn` / timestamp suffix
  where uniqueness helps log demos. Do not log tokens in scripts.
- **Rationale**: FR-012 and existing observability demos.
- **Alternatives considered**: Omitting correlation headers (regress demo); random UUID on every
  request without documented scenario tags (harder narration).

## Resolved unknowns

All Technical Context items for this feature are decided above; no remaining
`NEEDS CLARIFICATION` blockers for `/speckit-tasks`.
