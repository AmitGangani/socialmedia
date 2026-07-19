<!--
Sync Impact Report
- Version change: 2.0.0 -> 2.0.1
- Modified principles: None; all principle names and obligations are unchanged.
- Project identity: Mini Twitter -> SocialMedia
- Added principles: None.
- Added sections: None.
- Removed sections: None.
- Modified sections: Constitution title only.
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md
  - ✅ .specify/templates/spec-template.md (reviewed; no change required)
  - ✅ .specify/templates/tasks-template.md (reviewed; no change required)
  - ✅ .specify/templates/constitution-template.md (reviewed; no change required)
  - ✅ .specify/templates/checklist-template.md (reviewed; no change required)
- Active feature artifacts:
  - ✅ specs/001-mini-twitter-platform/spec.md (reviewed; already aligned)
  - ✅ specs/001-mini-twitter-platform/plan.md
  - ✅ specs/001-mini-twitter-platform/research.md
  - ✅ specs/001-mini-twitter-platform/data-model.md
  - ✅ specs/001-mini-twitter-platform/quickstart.md
  - ✅ specs/001-mini-twitter-platform/tasks.md
  - ✅ specs/001-mini-twitter-platform/contracts/openapi.yaml
  - ✅ specs/001-mini-twitter-platform/contracts/events.asyncapi.yaml
  - ✅ specs/001-mini-twitter-platform/checklists/requirements.md
- Command/runtime guidance:
  - ✅ .agents/skills/speckit-plan/SKILL.md
  - ✅ .agents/skills/speckit-specify/SKILL.md
  - ✅ .agents/skills/speckit-tasks/SKILL.md
  - ✅ .agents/skills/speckit-implement/SKILL.md
  - ✅ .agents/skills/speckit-analyze/SKILL.md
  - ✅ .agents/skills/speckit-clarify/SKILL.md
  - ✅ .agents/skills/speckit-constitution/SKILL.md
  - ✅ Remaining .agents/skills/speckit-*/SKILL.md files reviewed; no stale project-name
    references found
  - ✅ Runtime guidance (none present)
- Follow-up TODOs: None.
-->
# SocialMedia Constitution

## Core Principles

### I. Microservices Boundaries

Each domain service MUST have one clear responsibility and MUST own its persisted data.
Stateful services MUST use a service-owned database with exclusive access credentials. A
service MUST NOT read or write another service's database or tables. Services MUST
communicate only through versioned REST contracts or explicit asynchronous message
contracts. A service
MUST NOT depend directly on another service's repository, entity classes, or internal
implementation module.

Every boundary decision MUST be explainable in one sentence. For example: "Timeline is
separate because fan-out read/write tradeoffs are a distinct scaling problem." This rule
makes ownership, coupling, and scaling decisions visible and defensible.

### II. Current, Coherent Spring Stack

All services MUST target Java 21 LTS and Spring Boot 4.1.x on Spring Framework 7. The
baseline stack is Spring Data JPA, Spring Security 7 with JWT, and a Spring Cloud release
train compatible with Spring Boot 4.1.x. Service discovery MUST use either Eureka or
Consul; the plan MUST choose one and MUST NOT configure both.

HTTP services MUST use the conventional Spring MVC/blocking model with Java 21 virtual
threads. Spring Cloud Gateway MUST use its Server Web MVC flavor for routing. Reactive
WebFlux or Project Reactor application code requires an explicit constitution exception
that demonstrates a measured need not met by virtual threads. This keeps the concurrency
model simple enough to explain while preserving a meaningful tradeoff discussion.

### III. Minimal, Explainable Code Over Framework Magic

Code MUST favor direct, readable control flow over annotation-heavy, generated, or
"clever" machinery. A Controller -> Service -> Repository path MUST include only the
layers that perform a distinct responsibility. DTOs, mappers, factories, wrappers, and
interfaces MUST NOT be added by habit; each MUST protect a boundary, enforce a contract,
or demonstrate a named design tradeoff.

Reviewers MUST be able to trace an important request from entry point to persistence or
message publication without reverse-engineering framework indirection. The purpose is
interview-ready code whose design can be explained from first principles.

### IV. One Core Concept Per Service, Done Well

Each service MUST remain a focused vehicle for one or two coherent HLD/LLD concept areas:

- User Service: authentication, BCrypt password hashing, and JWT issuance.
- Post Service: write-heavy data modeling and cursor or stable page-based pagination.
- Follow Service: graph-like relationships and N+1 query avoidance.
- Timeline Service: fan-out-on-write versus fan-out-on-read, with the chosen tradeoff
  documented and demonstrated.
- Notification Service: asynchronous messaging and eventual consistency.
- API Gateway: routing, rate limiting, and authentication delegation.

Features outside a service's teaching purpose MUST be rejected, deferred, or justified as
necessary for an existing core concept. Depth of reasoning and demonstrable tradeoffs take
priority over feature count or enterprise-tool breadth.

### V. Resilience, Kept Simple

Resilience4j circuit breakers and timeouts MUST be applied only to one or two critical
inter-service calls identified in the implementation plan, such as Timeline calling Post
Service. Each protected call MUST document its failure behavior and why protection matters.
Calls that are not critical MUST remain unwrapped unless an observed failure mode justifies
the added policy. Retries MUST NOT be added without an idempotency analysis.

This constraint makes resilience behavior visible and manually demonstrable instead of burying the
lesson under uniform configuration.

### VI. Observability, Minimal Viable

Every service MUST emit structured logs through SLF4J and Logback. Every inbound request
MUST accept or create a correlation ID, include it in all logs, return it to the caller,
and propagate it across REST calls and asynchronous messages. Logs MUST identify the
service and operation without recording passwords, JWTs, or other secrets.

An ELK stack, Zipkin, or another full telemetry platform MUST NOT be required for the
baseline. It MAY be added only as a separately scoped stretch goal after the correlation
flow works end to end. The system MUST first demonstrate the tracing concept with minimal
operational machinery.

### VII. Containerization Without Orchestration Distraction

Every deployable service MUST have a lightweight, reproducible Dockerfile. A root Docker
Compose definition MUST start the services, their independently owned datastores, the
selected discovery server, and any required message broker for local demonstration.
Configuration and secrets MUST be supplied externally rather than baked into images.

Kubernetes, Helm, and cloud orchestration MUST remain out of baseline scope unless approved
as a stretch goal. Local reproducibility and service interaction take priority over
orchestration breadth.

### VIII. Security Baseline, Not Enterprise IAM

Passwords MUST be hashed with BCrypt and MUST never be logged or stored in plaintext. The
User Service MUST issue stateless JWTs; the API Gateway MUST validate their signature,
expiry, and required claims before forwarding protected requests. Services MUST apply Bean
Validation at input boundaries, and authorization decisions MUST use the authenticated
identity rather than client-supplied user identifiers.

OAuth2 federation, SSO, multi-tenancy, and enterprise IAM policy engines MUST remain out of
scope unless introduced later as an explicit, interview-relevant concept. The baseline is
secure enough to demonstrate sound fundamentals without turning identity into the entire
project.

### IX. Avoid Premature Abstraction

The codebase MUST NOT contain generic repositories, speculative extension points,
single-implementation interfaces, or configuration for hypothetical requirements. A new
abstraction MUST map to an active requirement and MUST carry a concise tradeoff statement
in the plan or code review: "I used X because Y tradeoff." A specific implementation MUST
remain concrete until a second use case or a deliberate pattern demonstration earns the
abstraction.

This rule preserves a small design surface and makes every pattern a defendable decision
rather than resume-driven ceremony.

### X. Manual Verification, No Automated Tests

The baseline project MUST NOT contain automated unit, integration, contract, component,
end-to-end, load, or acceptance test source files. It MUST NOT add test-only dependencies,
test runners, mocking frameworks, coverage tools, generated test reports, or CI stages that
execute application tests. Plans and task lists MUST NOT create automated-test work, even as
optional or stretch tasks.

Verification MUST use documented manual scenarios, a request collection or minimal client,
observable logs and correlation IDs, service health checks, and architecture review
checklists. Compilation, packaging, container builds, service startup, and health checks
remain required engineering checks; they are not application test suites. Requirements
quality checklists MAY be used because they review documentation rather than execute
application behavior. This keeps the portfolio effort focused on explainable architecture
and demonstrable flows while honoring the project's intentionally limited delivery scope.

## Architecture and Technology Constraints

The baseline system consists of User, Post, Follow, Timeline, and Notification services,
plus an API Gateway. Domain state MUST be owned by its corresponding service; the gateway
MUST NOT become a domain service. Cross-service data MUST be represented by stable IDs and
contracts rather than shared ORM models.

REST is the default for synchronous queries and commands that require an immediate result.
Asynchronous messaging is reserved for decoupled side effects and eventually consistent
work, especially notifications and any selected timeline fan-out path. Each asynchronous
flow MUST state its delivery assumption, idempotency behavior, and acceptable consistency
window in the plan.

The baseline implementation MUST use Spring Boot 4.1.x dependency management rather than
pinning unrelated Spring component versions independently. Docker Compose is the required
local runtime. Kubernetes, full distributed tracing stacks, OAuth2/SSO, multi-tenancy, and
additional infrastructure are stretch goals and MUST NOT enter baseline tasks implicitly.

## Delivery and Review Workflow

Every feature specification MUST identify its owning service, affected user behavior,
cross-service impact, and explicit out-of-scope items. Every implementation plan MUST:

- state each affected service boundary in one sentence;
- identify the service's core teaching concept and the tradeoff being demonstrated;
- list owned data and prove that no shared database access is introduced;
- identify every REST or message contract changed;
- justify every new layer, interface, DTO, mapper, resilience policy, and infrastructure
  component; and
- define focused manual verification for the core concept and any failure or consistency
  behavior without creating automated test artifacts.

Every quickstart MUST provide repeatable manual steps and expected outcomes for the primary
flow, one relevant failure path, and any eventual-consistency behavior. Tasks MUST include
manual demonstration checkpoints where evidence is needed, but MUST NOT include automated
test creation, test execution, coverage measurement, or TDD steps.

The Constitution Check is a blocking gate before design and after design. Any violation
MUST be recorded in Complexity Tracking with the simpler alternative considered and a
specific reason it cannot satisfy the current requirement. "Best practice," "future
proofing," and "enterprise readiness" are not sufficient justifications by themselves.

## Governance

This constitution is the highest-priority project guidance. Specifications, plans, tasks,
and implementation reviews MUST verify compliance. When another document conflicts with a
MUST rule here, that document MUST change or the constitution MUST be amended first.

Amendments require a written rationale, an impact review of dependent templates and active
feature artifacts, and an explicit migration plan for affected code. Constitution versions
follow semantic versioning: MAJOR for incompatible principle removals or redefinitions,
MINOR for new principles or materially expanded obligations, and PATCH for clarifications
that do not change obligations.

Compliance MUST be reviewed during planning, after design, and before implementation is
declared complete using documented manual evidence and architecture review checklists.
Complexity exceptions MUST be narrow, documented, and revisited when their triggering
requirement changes. Portfolio scope MUST favor an explainable local demonstration over
unsupported claims of production-scale maturity.

**Version**: 2.0.1 | **Ratified**: 2026-07-18 | **Last Amended**: 2026-07-18
