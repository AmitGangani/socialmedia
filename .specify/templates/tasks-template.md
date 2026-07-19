---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Verification**: Automated test tasks are prohibited by the constitution. Use documented
manual scenarios, request-collection steps, logs/correlation evidence, health checks, and
architecture review checkpoints.

**Organization**: Tasks are grouped by user story to enable independent implementation and manual verification of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Domain services**: `services/<service-name>/src/main/java/`
- **Gateway**: `api-gateway/src/main/java/`
- **Discovery**: `service-discovery/`
- **Contracts**: `contracts/`
- **Local runtime**: root `docker-compose.yml` plus one Dockerfile per deployable service
- Paths shown below are illustrative; use the exact structure selected in plan.md.

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit-tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/

  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Verified independently through documented manual steps
  - Delivered as an MVP increment

  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize Java 21 and Spring Boot 4.1.x modules with compatible Spring Cloud dependency management
- [ ] T003 [P] Add a lightweight Dockerfile to each deployable service
- [ ] T004 Configure root docker-compose.yml for services, service-owned datastores, discovery, and required messaging

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T005 Configure each stateful service datastore with exclusive credentials
- [ ] T006 [P] Implement gateway JWT validation and authenticated identity propagation
- [ ] T007 [P] Configure Spring Cloud Gateway Server Web MVC routes and selected service discovery
- [ ] T008 [P] Configure MVC virtual threads for blocking service workloads
- [ ] T009 Add structured SLF4J/Logback logging and correlation-ID propagation
- [ ] T010 Setup externalized environment configuration without embedded secrets

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Verification**: [Manual steps and expected evidence showing this story works on its own]

### Implementation for User Story 1

- [ ] T011 [P] [US1] Create [Entity1] in services/[service]/src/main/java/[path]/[Entity1].java
- [ ] T012 [US1] Implement [Service] in services/[service]/src/main/java/[path]/[Service].java
- [ ] T013 [US1] Implement [endpoint/feature] in services/[service]/src/main/java/[path]/[Controller].java
- [ ] T014 [US1] Add Bean Validation and explicit error handling in services/[service]/src/main/java/[path]/
- [ ] T015 [US1] Propagate correlation ID through affected REST/message contracts in [exact paths]
- [ ] T016 [US1] Document and execute the story's manual verification steps in specs/[feature]/quickstart.md

**Checkpoint**: At this point, User Story 1 should be fully functional and manually verifiable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Verification**: [Manual steps and expected evidence showing this story works on its own]

### Implementation for User Story 2

- [ ] T017 [P] [US2] Create [Entity] in services/[service]/src/main/java/[path]/[Entity].java
- [ ] T018 [US2] Implement [Service] in services/[service]/src/main/java/[path]/[Service].java
- [ ] T019 [US2] Implement [endpoint/feature] in services/[service]/src/main/java/[path]/[Controller].java
- [ ] T020 [US2] Integrate through the documented REST/message contract in [exact paths]
- [ ] T021 [US2] Document and execute the story's manual verification steps in specs/[feature]/quickstart.md

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Verification**: [Manual steps and expected evidence showing this story works on its own]

### Implementation for User Story 3

- [ ] T022 [P] [US3] Create [Entity] in services/[service]/src/main/java/[path]/[Entity].java
- [ ] T023 [US3] Implement [Service] in services/[service]/src/main/java/[path]/[Service].java
- [ ] T024 [US3] Implement [endpoint/feature] in services/[service]/src/main/java/[path]/[Controller].java
- [ ] T025 [US3] Document and execute the story's manual verification steps in specs/[feature]/quickstart.md

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Remove unjustified interfaces, generic repositories, DTOs, and mappers
- [ ] TXXX Validate service boundary rationales and database ownership
- [ ] TXXX Validate Docker Compose startup and per-service Docker images
- [ ] TXXX Execute documented manual failure and eventual-consistency scenarios
- [ ] TXXX Validate BCrypt, gateway JWT checks, input validation, and secret-safe logging
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently verifiable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently verifiable

### Within Each User Story

- Models before services
- Services before endpoints
- Core implementation before integration
- Manual verification after the story's implementation and integration work
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch independent service artifacts for User Story 1 together:
Task: "Create [Entity1] in services/[service]/src/main/java/[path]/[Entity1].java"
Task: "Update [contract] in contracts/[service]/[contract-file]"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Execute User Story 1's manual verification steps
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Verify manually → Deploy/Demo (MVP!)
3. Add User Story 2 → Verify manually → Deploy/Demo
4. Add User Story 3 → Verify manually → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and manually verifiable
- Do not add automated test source, test-only dependencies, test runners, coverage tools,
  or CI test stages
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Do not add Kubernetes, full telemetry stacks, enterprise IAM, or speculative abstractions
  unless the plan records an approved stretch goal or constitution exception.
