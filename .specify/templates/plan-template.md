# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 21 LTS (constitution-required)

**Primary Dependencies**: Spring Boot 4.1.x, Spring Framework 7, Spring Data JPA,
Spring Security 7; use a compatible Spring Cloud release train

**Concurrency Model**: Spring MVC/blocking I/O on Java 21 virtual threads; WebFlux requires
a documented constitution exception

**Storage**: [service-owned datastore and credentials, or N/A; shared databases are forbidden]

**Service Discovery**: [Eureka or Consul; select exactly one]

**Service Communication**: [versioned REST contract and/or async message contract]

**Manual Verification**: [quickstart scenarios, request collection steps, log/correlation
evidence, and architecture review; automated test code and suites are forbidden]

**Target Platform**: Linux containers via Docker Compose

**Project Type**: SocialMedia microservice(s)

**Core Concept**: [the 1-2 HLD/LLD concepts this service/feature demonstrates]

**Resilience Scope**: [critical call protected by timeout/circuit breaker, or N/A]

**Observability**: Structured SLF4J/Logback logs and correlation-ID propagation

**Security**: BCrypt, gateway-validated stateless JWT, and Bean Validation as applicable

**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]

**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]

**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Boundary**: Each affected service has one responsibility and a one-sentence boundary
  rationale; no shared database, entity module, or repository access is introduced.
- **Contracts**: Every cross-service dependency is a versioned REST or async message
  contract with explicit failure/consistency behavior.
- **Stack**: Java 21, Spring Boot 4.1.x/Framework 7, compatible Spring Cloud, MVC virtual
  threads, and exactly one of Eureka or Consul are used. Gateway work uses Server Web MVC.
- **Teaching focus**: Each service remains limited to its assigned 1-2 core concepts, and
  the primary tradeoff is documented.
- **Simplicity**: Every layer, interface, DTO, mapper, pattern, and infrastructure component
  is required by the current feature and has a defendable tradeoff statement.
- **Resilience**: Resilience4j is limited to identified critical calls; retries include an
  idempotency analysis.
- **Observability**: Structured logs and correlation IDs cross all affected REST/message
  boundaries without logging secrets.
- **Security**: BCrypt, gateway JWT validation, authenticated identity use, and input
  validation are preserved.
- **Delivery**: Each deployable service has a lightweight Dockerfile and the local system
  remains runnable with Docker Compose. Kubernetes and enterprise IAM remain stretch scope.
- **Verification**: The plan uses repeatable manual scenarios and review checklists only;
  no automated test source, test-only dependency, runner, coverage tool, or CI test stage is
  introduced.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unaffected services and expand the chosen structure
  with real paths. The delivered plan must include only actual project paths.
-->

```text
services/
├── user-service/
├── post-service/
├── follow-service/
├── timeline-service/
└── notification-service/

api-gateway/
service-discovery/
contracts/
docker-compose.yml
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., WebFlux in one service] | [measured need] | [why MVC virtual threads are insufficient] |
| [e.g., single-use interface] | [specific demonstrated pattern] | [why a concrete class is insufficient] |
