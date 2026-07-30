# Tasks: Postman API Collection

**Input**: Design documents from `/specs/002-postman-collection/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Verification**: Automated test tasks are prohibited by the constitution. Use documented
manual scenarios, Postman collection steps (capture-only scripts, human response review),
logs/correlation evidence, health checks, and architecture review checkpoints. Do **not**
add Newman/CI, `pm.test` assertions, or application test source.

**Organization**: Tasks are grouped by user story to enable independent implementation and manual verification of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Collection**: `postman/SocialMedia.postman_collection.json`
- **Environment**: `postman/Local.postman_environment.json`
- **Optional Runner data**: `postman/data/`
- **Feature docs**: `specs/002-postman-collection/`
- **Platform quickstart**: `specs/001-mini-twitter-platform/quickstart.md`
- **Legacy (remove at end)**: `http/socialmedia.http` and entire `http/` directory
- **Scenario map**: `specs/002-postman-collection/contracts/scenario-inventory.md`
- **Request shape reference**: `http/socialmedia.http` (until deleted) and platform OpenAPI under `specs/001-mini-twitter-platform/contracts/openapi.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the version-controlled Postman asset locations and empty skeletons

- [x] T001 Create directory `postman/` and optional `postman/data/` per plan structure in plan.md
- [x] T002 [P] Create skeleton Postman Environment v2.1 JSON in `postman/Local.postman_environment.json` with name `Local` and empty `values` array ready for variables
- [x] T003 [P] Create skeleton Postman Collection v2.1 JSON in `postman/SocialMedia.postman_collection.json` with `info.name` SocialMedia, Collection v2.1 schema URL, and placeholder `item` array

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared collection shell, environment variables, and authoring rules required before any user-story requests

**⚠️ CRITICAL**: No user story request population can begin until this phase is complete

- [x] T004 Populate `postman/Local.postman_environment.json` with config variables from data-model.md (`gateway`, Alice/Bob credentials, rate-limit demo credentials, optional `postService`) using only local demo secrets
- [x] T005 Add capture-slot variables (empty initial values) to `postman/Local.postman_environment.json` for `aliceId`, `bobId`, `aliceJwt`, `bobJwt`, post IDs, cursors, `followRelationshipId`, and rate-limit capture fields per data-model.md
- [x] T006 Add numbered empty folders with descriptions to `postman/SocialMedia.postman_collection.json`: `00 Happy Path`, `01 Auth & Users`, `02 Posts`, `03 Follows & Timeline`, `04 Engagement`, `05 Notifications`, `06 Gateway Limits`, `07 Optional Load`, `08 Ops Notes` per research.md §4
- [x] T007 Write collection-level `info.description` in `postman/SocialMedia.postman_collection.json` covering import steps, required variables, first-time vs login-first re-run, and capture-only script policy (no `pm.test`)
- [x] T008 Document capture-script convention in `specs/002-postman-collection/data-model.md` or collection description: post-response `pm.environment.set` only; pre-request only for correlation helpers; never `pm.test`/`pm.expect`

**Checkpoint**: Foundation ready — Happy Path and domain folders can be filled

---

## Phase 3: User Story 1 - Explore and demo from Happy Path (Priority: P1) 🎯 MVP

**Goal**: Importable collection + Local environment where a presenter runs `00 Happy Path` end-to-end (register/login Alice & Bob, follow, publish, timeline, reply, notifications) with automatic token/ID capture and no manual JWT paste

**Independent Verification**: With Compose up, import both JSON files, select Local, run `00 Happy Path` top-to-bottom (or login-first re-run), confirm IDs/JWTs appear in environment and later requests succeed without hand-edited tokens; human-check responses against request descriptions

### Implementation for User Story 1

- [x] T009 [US1] Add Happy Path requests HP1–HP4 (Register Alice, Register Bob, Login Alice, Login Bob by email) to folder `00 Happy Path` in `postman/SocialMedia.postman_collection.json` using bodies/headers from `http/socialmedia.http`, `{{gateway}}`, and `X-Correlation-Id`
- [x] T010 [US1] Add capture-only post-response scripts on register/login Happy Path requests in `postman/SocialMedia.postman_collection.json` to set `aliceId`, `bobId`, `aliceJwt`, `bobJwt` from response JSON into the environment
- [x] T011 [US1] Add Happy Path requests HP5–HP9 (Alice follows Bob, Bob publishes interaction parent, Alice home timeline first page, Alice replies, Poll Bob's notifications) to `00 Happy Path` in `postman/SocialMedia.postman_collection.json` with Bearer tokens from env vars and capture scripts for relationship/post/reply IDs as needed
- [x] T012 [US1] Write first-time vs login-first re-run instructions on the `00 Happy Path` folder description in `postman/SocialMedia.postman_collection.json` (stable Alice/Bob; skip/ignore register on re-run)
- [x] T013 [US1] Align Happy Path steps and evidence checklist in `specs/002-postman-collection/quickstart.md` with the actual request names in `postman/SocialMedia.postman_collection.json`
- [x] T014 [US1] Manually execute first-time Happy Path (or login-first if users exist) against local Gateway and record pass/fail notes in `specs/002-postman-collection/quickstart.md` evidence checklist

**Checkpoint**: User Story 1 is demoable independently as MVP without domain parity folders complete

---

## Phase 4: User Story 2 - Contract and failure parity (Priority: P2)

**Goal**: Domain folders mirror verification scenarios from `http/socialmedia.http` (validation, authz, idempotency, pagination errors, engagement/deleted-parent, notifications ownership, optional rate-limit and load) with human-readable expected outcomes; ≥95% inventory coverage

**Independent Verification**: After Happy Path, open each domain folder, run one success and one failure request per area, confirm outcomes match descriptions without using `http/socialmedia.http`; review inventory rows marked request/runner/ops-manual/doc-only

### Implementation for User Story 2

- [x] T015 [P] [US2] Populate folder `01 Auth & Users` in `postman/SocialMedia.postman_collection.json` with inventory A5–A9 (self, public profile, duplicate username/email, wrong credentials) plus descriptions; share or duplicate register/login as needed for domain browse
- [x] T016 [P] [US2] Populate folder `02 Posts` in `postman/SocialMedia.postman_collection.json` with inventory P1–P18 and P20 (publish, list, validation, authz, delete idempotency, no-edit) using capture scripts for post IDs/cursors where needed
- [x] T017 [P] [US2] Populate folder `03 Follows & Timeline` in `postman/SocialMedia.postman_collection.json` with inventory F1–F14 (follow/unfollow/refollow, home pages, cursor errors) and capture scripts for follow IDs and timeline cursors
- [x] T018 [P] [US2] Populate folder `04 Engagement` in `postman/SocialMedia.postman_collection.json` with inventory E1–E12 (like, reply, deleted parent, self-reply) and required capture scripts
- [x] T019 [P] [US2] Populate folder `05 Notifications` in `postman/SocialMedia.postman_collection.json` with inventory N1–N6 (poll, keyset, ownership isolation, cursor errors) and document N7 Kafka DLT/replay as doc-only in folder description
- [x] T020 [US2] Populate folder `06 Gateway Limits` in `postman/SocialMedia.postman_collection.json` with G1–G10 (rate-limit account, exhaust, reject-while-exhausted, refill); implement exhaust as runner-oriented requests and document burst/wait rules in folder description
- [x] T021 [P] [US2] Add optional Runner data files under `postman/data/` (e.g. auth exhaust ×11, write exhaust ×61) if used by `06 Gateway Limits` requests in `postman/SocialMedia.postman_collection.json`
- [x] T022 [US2] Populate folder `07 Optional Load` in `postman/SocialMedia.postman_collection.json` with L1–L5 (200 posts, 1k followers register/login/follow, high-follower publish) as runner-oriented optional requests with pacing notes; add `postman/data/` fixtures if required
- [x] T023 [US2] Populate folder `08 Ops Notes` in `postman/SocialMedia.postman_collection.json` with O1 timeline-during-post-stop request, P19 internal bulk lookup ops-manual request (`{{postService}}`), and pointers to Kafka doc-only steps; descriptions must state compose/private-network prerequisites
- [x] T024 [US2] Ensure every request/folder in populated folders has purpose + expected outcome description text in `postman/SocialMedia.postman_collection.json` (FR-011); retain `X-Correlation-Id` where legacy file had scenario tags (FR-012)
- [x] T025 [US2] Update `specs/002-postman-collection/contracts/scenario-inventory.md` with implementation status for each row (mapped request name or confirmed doc-only/ops-manual/runner)
- [x] T026 [US2] Spot-check manual verification: one success + one failure per folders `01`–`05` and skim `06`–`08` descriptions against `specs/002-postman-collection/quickstart.md` domain sampling section

**Checkpoint**: User Story 2 parity surface is independently browsable; Happy Path still works

---

## Phase 5: User Story 3 - Onboard without IDE lock-in (Priority: P3)

**Goal**: Docs and repo layout treat Postman as the only primary manual API surface; contributors never need JetBrains `.http` or the `http/` directory

**Independent Verification**: Follow only updated docs from a clean mental model—import `postman/*`, run Happy Path; repository has no live instruction requiring `http/socialmedia.http` as primary; after T030, `http/` is gone

### Implementation for User Story 3

- [x] T027 [US3] Rewrite operator request-collection sections in `specs/001-mini-twitter-platform/quickstart.md` to import/run `postman/SocialMedia.postman_collection.json` and `postman/Local.postman_environment.json` instead of `http/socialmedia.http`
- [x] T028 [P] [US3] Finalize import, re-run, and migration-completion checklists in `specs/002-postman-collection/quickstart.md` so they match shipped folder/request names
- [x] T029 [P] [US3] Search live operator-facing docs under repo root and `specs/` for `http/socialmedia.http` / `http/` primary paths and update or neutralize them (leave historical completed task text in `specs/001-mini-twitter-platform/tasks.md` as past tense if needed)
- [x] T030 [US3] Delete the entire `http/` directory including `http/socialmedia.http` after collection + docs point to Postman (FR-010 / SC-005)
- [x] T031 [US3] Confirm `git status` shows no remaining `http/` tree and that `postman/` JSON files remain the sole request surface

**Checkpoint**: Onboarding works without IDE HTTP Client or `http/` directory

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Parity gate, constitution-safe scripts, final manual validation

- [x] T032 Audit `postman/SocialMedia.postman_collection.json` for any `pm.test`, `pm.expect`, or assertion libraries and remove them (SC-007)
- [x] T033 [P] Verify pretty-printed stable JSON formatting in `postman/SocialMedia.postman_collection.json` and `postman/Local.postman_environment.json` for readable diffs
- [x] T034 [P] Confirm scenario coverage ≥95% against `specs/002-postman-collection/contracts/scenario-inventory.md` with zero silent drops (SC-003)
- [x] T035 Re-run Happy Path login-first path and confirm under-10-minute re-run checklist in `specs/002-postman-collection/quickstart.md` (SC-001)
- [x] T036 Architecture review: no new services, no OpenAPI changes required, no automated tests or Newman CI added; collection only targets Gateway public API except documented ops-manual internal path

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational — MVP
- **User Story 2 (Phase 4)**: Depends on Foundational; practical dependency on US1 capture vars/patterns recommended but domain folders can start after T008
- **User Story 3 (Phase 5)**: Depends on US1 minimum for docs accuracy; **T030 delete `http/`** MUST wait until US1+US2 collection content is complete (use US2 inventory as source until delete)
- **Polish (Phase 6)**: Depends on US1–US3 desired scope complete

### User Story Dependencies

- **User Story 1 (P1)**: After Phase 2 only — no dependency on US2/US3
- **User Story 2 (P2)**: After Phase 2; may reuse US1 capture patterns; independently verifiable via domain folders
- **User Story 3 (P3)**: Docs can draft after US1; `http/` deletion only after US2 parity migration content is done

### Within Each User Story

- Environment/folders before requests
- Requests before capture scripts where scripts depend on response shape
- Descriptions on every request
- Manual verification after implementation tasks

### Parallel Opportunities

- T002/T003 in parallel after T001
- T015–T019 domain folders can run in parallel after Phase 2 (different folder sections; merge carefully if single JSON file — prefer sequential edits to same collection file **or** one owner for the JSON)
- T021 data files parallel to T020 design once request names known
- T028/T029 docs parallel after collection names stable
- **Note**: `postman/SocialMedia.postman_collection.json` is a single file — treat folder population tasks as sequential for one implementer; [P] marks conceptual independence for multi-person merge planning

---

## Parallel Example: User Story 2

```bash
# Conceptual parallelization (coordinate merges on the single collection JSON):
Task: "Populate 01 Auth & Users in postman/SocialMedia.postman_collection.json"
Task: "Populate 02 Posts in postman/SocialMedia.postman_collection.json"
Task: "Populate 05 Notifications in postman/SocialMedia.postman_collection.json"
Task: "Add postman/data/ runner fixtures"
```

Prefer one implementer editing the collection JSON sequentially by folder (T015→T023) to avoid merge pain.

---

## Parallel Example: User Story 1

```bash
# After Phase 2:
Task: "Add HP1–HP4 register/login to 00 Happy Path"
# then (depends on HP1–HP4 existing):
Task: "Add capture scripts for IDs/JWTs"
Task: "Add HP5–HP9 social/timeline/notification steps"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (`postman/` skeletons)
2. Complete Phase 2: Foundational (env vars + folder shell + descriptions)
3. Complete Phase 3: User Story 1 (Happy Path + capture + manual run)
4. **STOP and VALIDATE**: Import collection, run Happy Path, confirm no JWT paste
5. Demo MVP before parity folders

### Incremental Delivery

1. Setup + Foundational → empty importable collection
2. US1 → Happy Path demo (MVP)
3. US2 → full verification parity folders
4. US3 → docs switch + delete `http/`
5. Polish → script audit + coverage gate

### Parallel Team Strategy

With multiple people:

1. One owner for `postman/SocialMedia.postman_collection.json` (serialize folder writes)
2. Another owner for `postman/Local.postman_environment.json` + `postman/data/` + docs (`quickstart.md`, platform quickstart)
3. Inventory status updates in `scenario-inventory.md` after each folder lands

---

## Notes

- [P] tasks = different files or independent doc sections; **single collection JSON requires merge discipline**
- [Story] label maps task to US1/US2/US3 for traceability
- Alice & Bob remain the Happy Path demo pair; rate-limit and fan-out accounts stay in optional folders
- Do not add automated tests, Newman CI, or `pm.test` assertions
- Do not generate the collection from OpenAPI as primary authoring
- Commit after each folder or logical group
- Stop at checkpoints to validate independently
- Source request bodies from `http/socialmedia.http` until T030 deletes it; after deletion use collection + OpenAPI only
