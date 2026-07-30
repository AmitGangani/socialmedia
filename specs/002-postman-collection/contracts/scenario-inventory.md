# Contract: Scenario Inventory (`.http` → Postman)

**Feature**: `002-postman-collection` | **Date**: 2026-07-24  
**Source of request shapes**: Existing public API (see platform OpenAPI under
`specs/001-mini-twitter-platform/contracts/openapi.yaml`)  
**Source of scenario list**: `http/socialmedia.http` (retired after migration)

This inventory is the **parity contract** for manual verification packaging. It does not
change service APIs. Implementation MUST map each row to a named Postman request or mark
`Disposition` as `doc-only` / `ops-manual` with rationale. Silent omission is not allowed
(spec SC-003).

## Legend

| Disposition | Meaning |
|-------------|---------|
| `request` | Named HTTP request in the collection |
| `runner` | Request designed for optional Collection Runner iterations + description |
| `doc-only` | Documented in quickstart/folder description only (no HTTP) |
| `ops-manual` | Request + mandatory operator steps (compose stop/start, private network) |

## Folder: `00 Happy Path`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| HP1 | Register Alice | Register Alice | request |
| HP2 | Register Bob | Register Bob | request |
| HP3 | Login Alice | Login Alice | request |
| HP4 | Login Bob by email | Login Bob by email | request |
| HP5 | Alice follows Bob | Alice follows Bob | request |
| HP6 | Bob publishes the Phase 7 like/reply parent | Bob publishes interaction parent | request |
| HP7 | Alice home timeline - first page | Alice home timeline - first page | request |
| HP8 | Alice replies to Bob's post | Alice replies to Bob's post | request |
| HP9 | Poll Bob's notifications for Alice's follow and reply | Poll Bob's notifications | request |

Folder description MUST document login-first re-run (skip HP1–HP2 or accept 409, run HP3–HP4+).

## Folder: `01 Auth & Users`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| A1 | Register Alice | (shared / see Happy Path; may alias or duplicate for domain browse) | request |
| A2 | Register Bob | (shared) | request |
| A3 | Login Alice | (shared) | request |
| A4 | Login Bob by email | (shared) | request |
| A5 | Alice - own account | Alice - own account | request |
| A6 | Bob - public profile | Bob - public profile | request |
| A7 | Reject duplicate username ignoring case | Reject duplicate username | request |
| A8 | Reject duplicate email ignoring case | Reject duplicate email | request |
| A9 | Reject wrong credentials uniformly | Reject wrong credentials | request |

## Folder: `02 Posts`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| P1 | Bob publishes a one-code-point post | Bob publishes one-code-point post | request |
| P2 | Bob publishes a 280-code-point post | Bob publishes 280-code-point post | request |
| P3 | Retrieve Bob's visible post | Retrieve Bob's visible post | request |
| P4 | First Bob profile-post page | First Bob profile-post page | request |
| P5 | Next Bob profile-post page | Next Bob profile-post page | request |
| P6 | Reject an empty post | Reject empty post | request |
| P7 | Reject a whitespace-only post | Reject whitespace-only post | request |
| P8 | Reject a 281-code-point post | Reject 281-code-point post | request |
| P9 | Reject client-supplied ownership | Reject client-supplied ownership | request |
| P10 | Reject a malformed profile-post cursor | Reject malformed profile-post cursor | request |
| P11 | Reject an oversized profile-post page | Reject oversized profile-post page | request |
| P12 | Reject a protected publish without a JWT | Reject publish without JWT | request |
| P13 | Alice cannot delete Bob's boundary post | Alice cannot delete Bob's post | request |
| P14 | The forbidden delete left the post visible | Forbidden delete left post visible | request |
| P15 | Bob deletes his boundary post | Bob deletes his boundary post | request |
| P16 | Bob repeats the idempotent delete | Bob repeats delete (idempotent) | request |
| P17 | Deleted post is unavailable directly | Deleted post unavailable | request |
| P18 | Deleted post is omitted from Bob's profile-post list | Deleted post omitted from profile list | request |
| P19 | Internal bulk lookup omits the deleted post | Internal bulk lookup omits deleted | ops-manual |
| P20 | No post-edit route exists | No post-edit route | request |

## Folder: `03 Follows & Timeline`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| F1 | Alice follows Bob | Alice follows Bob | request |
| F2 | Repeating Alice follows Bob is idempotent | Follow Bob repeat (idempotent) | request |
| F3 | Reject Alice following herself | Reject self-follow | request |
| F4 | Bob publishes for Alice's home timeline | Bob publishes for home timeline | request |
| F5 | Alice home timeline - first page | Alice home timeline - first page | request |
| F6 | Alice home timeline - next stable page | Alice home timeline - next page | request |
| F7 | Reject malformed Timeline cursor | Reject malformed timeline cursor | request |
| F8 | Reject oversized Timeline page | Reject oversized timeline page | request |
| F9 | Alice unfollows Bob | Alice unfollows Bob | request |
| F10 | Repeating Alice unfollows Bob is idempotent | Unfollow Bob repeat (idempotent) | request |
| F11 | Alice re-follows Bob without historical backfill | Alice re-follows Bob | request |
| F12 | Bob publishes after Alice re-follows | Bob publishes after re-follow | request |
| F13 | Verify the re-follow boundary in Alice's home timeline | Verify re-follow timeline boundary | request |
| F14 | Bob follows Alice so Alice's reply is eligible… | Bob follows Alice | request |

## Folder: `04 Engagement`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| E1 | Bob publishes the Phase 7 like/reply parent | Bob publishes interaction parent | request |
| E2 | Alice likes Bob's post | Alice likes Bob's post | request |
| E3 | Repeating Alice's like is idempotent | Like repeat (idempotent) | request |
| E4 | Alice replies to Bob's post | Alice replies to Bob's post | request |
| E5 | Retrieve the visible reply and available parent | Retrieve reply with parent | request |
| E6 | Poll Bob's home timeline for Alice's reply | Poll Bob home for Alice reply | request |
| E7 | Bob deletes the parent while preserving the reply | Bob deletes parent post | request |
| E8 | The reply survives with parent.available=false | Reply survives unavailable parent | request |
| E9 | Bob's timeline still hydrates the reply… | Bob timeline hydrates unavailable parent | request |
| E10 | Reject a new like against the deleted parent | Reject like on deleted parent | request |
| E11 | Reject a new reply against the deleted parent | Reject reply on deleted parent | request |
| E12 | Bob replies to Bob's own visible post without notifying himself | Bob self-reply (no self-notification) | request |

## Folder: `05 Notifications`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| N1 | Poll Bob's notifications for Alice's follow and reply | Poll Bob's notifications | request |
| N2 | Continue Bob's stable notification keyset | Bob notifications next page | request |
| N3 | Alice retrieves only Alice-owned notifications | Alice own notifications | request |
| N4 | A client-supplied target cannot expose Bob's notifications to Alice | Reject cross-user notification access | request |
| N5 | Reject a malformed Notification cursor | Reject malformed notification cursor | request |
| N6 | Reject an oversized Notification page | Reject oversized notification page | request |
| N7 | Notification DLT inspection and unchanged-event replay | Kafka DLT + replay steps | doc-only |

## Folder: `06 Gateway Limits`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| G1 | Scenario 12 prep - register dedicated rate-limit account | Rate-limit register | request |
| G2 | Scenario 12 prep - login dedicated rate-limit account | Rate-limit login | request |
| G3 | Auth bucket - exhaust with 11 wrong-password logins | Auth bucket exhaust (×11) | runner |
| G4 | Auth bucket - rejected unique registration while exhausted | Auth exhausted register rejected | request |
| G5 | Auth bucket - prove the rejected username has no public profile | Auth exhausted profile absent | request |
| G6 | Auth bucket - after one-minute refill… | Auth bucket after refill | request |
| G7 | Write bucket - exhaust with 61 idempotent follow commands | Write bucket exhaust (×61) | runner |
| G8 | Write bucket - rejected unique post while exhausted | Write exhausted post rejected | request |
| G9 | Write bucket - confirm the rejected marker is absent… | Write exhausted post absent | request |
| G10 | Write bucket - after one-minute refill… | Write bucket after refill | request |

## Folder: `07 Optional Load`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| L1 | Data runner - create 200 posts for stable Timeline traversal | Seed 200 timeline posts | runner |
| L2 | Data runner - register 1,000 follower accounts | Register fan-out followers | runner |
| L3 | Data runner - login 1,000 follower accounts… | Login fan-out followers | runner |
| L4 | Data runner - make all 1,000 accounts follow Bob | Fan-out follow Bob | runner |
| L5 | Publish once after the 1,000-follower runner | Publish high-follower post | request |

## Folder: `08 Ops Notes`

| # | Legacy section | Collection request name | Disposition |
|---|----------------|-------------------------|-------------|
| O1 | Timeline dependency failure (manual command) | Home timeline during post-service stop | ops-manual |
| O2 | (see N7) | DLT/replay documentation pointer | doc-only |
| O3 | (see P19) | Internal bulk lookup | ops-manual |

## Coverage summary

| Disposition | Count (approx.) | Notes |
|-------------|-----------------|-------|
| request | majority of ~70 HTTP sections | Including shared Happy Path duplicates allowed |
| runner | 6 | Rate-limit bursts + optional load |
| doc-only | 1–2 | Kafka DLT/replay |
| ops-manual | 2 | Internal bulk + dependency failure |

**Parity gate**: After implementation, every legacy `###` section appears in this table with a
final status checkmark in tasks; any `doc-only`/`ops-manual` row has description text in
collection or quickstart.

## Non-goals for this contract

- Changing OpenAPI or AsyncAPI definitions
- Adding Newman/CI automated execution
- Defining service DTOs (owned by platform contracts)


## Implementation status (2026-07-24)

All inventory rows are implemented in `postman/SocialMedia.postman_collection.json` unless noted:

| Area | Status |
|------|--------|
| HP1–HP9 Happy Path | **request** in `00 Happy Path` |
| A5–A9 Auth & Users | **request** in `01 Auth & Users` |
| P1–P18, P20 Posts | **request** in `02 Posts` |
| P19 Internal bulk | **ops-manual** in `08 Ops Notes` |
| F1–F14 Follows & Timeline | **request** in `03 Follows & Timeline` |
| E1–E12 Engagement | **request** in `04 Engagement` |
| N1–N6 Notifications | **request** in `05 Notifications` |
| N7 Kafka DLT/replay | **doc-only** in folder description + quickstart |
| G1–G10 Gateway Limits | **request** / **runner** in `06 Gateway Limits` + `postman/data/*` |
| L1–L5 Optional Load | **runner** / **request** in `07 Optional Load` + data CSVs |
| O1 Timeline dependency | **ops-manual** in `08 Ops Notes` |

Parity gate: **PASS** (no silent drops; doc-only/ops-manual explicitly documented).
