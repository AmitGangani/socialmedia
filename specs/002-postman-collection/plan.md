# Implementation Plan: Postman API Collection

**Branch**: `002-postman-collection` | **Date**: 2026-07-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-postman-collection/spec.md`

## Summary

Replace the IDE-specific `http/socialmedia.http` surface with a **hand-curated Postman
Collection v2.1** plus a **Local environment** file under `postman/`. The collection is the
primary manual verification and interview-demo tool for the existing public Gateway API. It
uses capture-only scripts (tokens, IDs, cursors), a dedicated **Happy Path** folder, domain
folders for parity scenarios, optional heavy folders for bulk/rate-limit demos, updated
quickstart/docs, and final deletion of the entire `http/` directory. No runtime microservice
behavior, OpenAPI contracts, or automated tests are introduced.

## Technical Context

**Language/Version**: N/A for runtime code (no Java/Spring changes required). Authoring
artifacts are JSON (Postman Collection v2.1 / Environment) plus Markdown docs.

**Primary Dependencies**: Postman desktop or web import of Collection Format v2.1; existing
platform stack (Gateway `http://localhost:8080`) unchanged. Scripts use the Postman sandbox
(`pm.environment`, `pm.response`) for capture only—never `pm.test` assertions.

**Concurrency Model**: N/A (client-side manual requests). Optional Collection Runner iterations
for bulk/rate-limit folders only; not a CI or automated test pipeline.

**Storage**: N/A. Version-controlled JSON files only; no production secrets.

**Service Discovery**: Unchanged (Eureka). Collection always targets Gateway host port, not
individual service ports (except documented private-network notes for internal bulk lookup /
fan-out auth pacing, which remain operator docs, not public collection defaults).

**Service Communication**: Unchanged public REST under `/api/v1` via Gateway. API shape source
of truth remains `specs/001-mini-twitter-platform/contracts/openapi.yaml`. This feature does
not redefine service contracts.

**Manual Verification**: [quickstart.md](./quickstart.md), `postman/*.json` collection +
environment, correlation IDs on requests, human review of responses against request
descriptions. Automated application tests, Newman CI, and Postman Tests-tab pass/fail checks
are forbidden.

**Target Platform**: Local Docker Compose demo of the existing SocialMedia platform; Postman
(or compatible importer) on the operator machine.

**Project Type**: Developer-experience / verification packaging (not a new microservice).

**Core Concept**: Manual verification surface modernization—importable, shareable request
collection without IDE lock-in—while preserving constitution principle X (manual verification
only).

**Resilience Scope**: N/A (no new inter-service calls). Existing Timeline/Post breaker demos
remain manual compose stop/start steps documented in quickstart and optional collection notes.

**Observability**: Preserve `X-Correlation-Id` on collection requests where the previous
`.http` file set them so log-correlation demos still work. Never log or commit JWTs/passwords
beyond local environment variables.

**Security**: Local demo credentials only in the environment template; no production secrets.
Bearer tokens stored in environment variables after login capture, not hard-coded.

**Performance Goals**: First-time Happy Path under 15 minutes; login-first re-run under 10
minutes (spec SC-001). Optional bulk/rate-limit folders are not part of the everyday demo
timing budget.

**Constraints**: Capture-only scripts; hand-curated single collection; delete `http/` at end;
≥95% scenario parity with previous request file (SC-003); free Postman features only.

**Scale/Scope**: ~80 discrete named scenarios from `http/socialmedia.http`; one collection,
one Local environment; doc updates in feature quickstart and references that pointed at
`http/socialmedia.http` (notably platform quickstart paths that must be redirected or
superseded for operators of this feature).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Boundary**: PASS — No service boundary change. DX assets only; Gateway remains the
  public entry. One-sentence rationale: “Postman packaging is not a domain service.”
- **Contracts**: PASS — No new cross-service dependencies. Collection exercises existing
  versioned REST; OpenAPI remains contract authority.
- **Stack**: PASS — No Java/Spring/Gateway stack change required for this feature.
- **Teaching focus**: PASS — Does not expand any service’s concept set; improves how demos
  of existing concepts are run.
- **Simplicity**: PASS — Two JSON files + docs; no generators, dual collections, or CI runners.
- **Resilience**: PASS — N/A; no Resilience4j or retry policy changes.
- **Observability**: PASS — Correlation headers retained on requests; secrets not committed
  or required in logs.
- **Security**: PASS — Demo credentials only; JWT from login capture; no production secrets.
- **Delivery**: PASS — Compose/Dockerfiles untouched; local system remains runnable as today.
- **Verification**: PASS — Manual scenarios and human response review only; capture scripts
  without `pm.test`; no Newman/CI test stages; no application test source.

*Post-design re-check: still PASS. Contracts inventory is a verification map, not a new API
surface or automated test harness.*

## Project Structure

### Documentation (this feature)

```text
specs/002-postman-collection/
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1 validation/import guide
├── contracts/
│   └── scenario-inventory.md   # Request parity map (.http → collection folders)
└── tasks.md             # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
postman/
├── SocialMedia.postman_collection.json   # Hand-curated Collection v2.1
└── Local.postman_environment.json        # Local Gateway + Alice/Bob variables

# Removed after migration completes:
# http/
# └── socialmedia.http

# Docs that must stop pointing at http/ as primary (update during implementation):
specs/001-mini-twitter-platform/quickstart.md   # redirect operators to postman/
specs/002-postman-collection/quickstart.md      # feature import + run guide
# Historical plan/research/tasks under 001 may retain past tense references; live
# operator paths must not require http/.
```

**Structure Decision**: Place importable JSON under repository-root `postman/` so it is
obvious and parallel to the retired `http/` folder. Keep feature design under
`specs/002-postman-collection/`. Do not embed the collection inside a service module. Do not
generate from OpenAPI. Platform API contracts stay under `specs/001-mini-twitter-platform/contracts/`.

## Complexity Tracking

> No constitution violations requiring justification.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
