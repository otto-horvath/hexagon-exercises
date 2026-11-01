---
session: 5
title: "Where are the whys and what in the refactorings?"
date: 2026-11-01
start: "10:39" # observed, local time (-03:00)
end: "13:00" # observed
active_minutes: 90 # reported
exercise: 4 # observed
phases: [start, guide, evaluate, wrap-up] # observed
mood_start: 4 # reported, 1–5
mood_end: 4 # reported, 1–5
energy: 4 # reported, 1–5
confidence_before: 4 # reported, 1–5
confidence_after: 4 # reported, 1–5
tools:
  - name: OpenCode
    model: deepseek-v4.1-flash-free
    source: observed
ai:
  helpfulness: 4 # reported, 1–5
  hints_requested: 7 # observed
  hint_levels: [concept, api, code-shape] # observed
  ai_wrote: [tests, scaffold, refactor, review, docs] # observed
  ai_mistakes: 2 # observed
stack_added: [] # observed, JDK APIs only
tests_passing: 46 # observed
score: 4 # observed
commits: [d326e4c, 0986c25, 4de4ebd, ad7bad3, 643c3a0, 8dd9d94, b02fa0e, 21451c2, 34ed39a, b0b7af6, 9c24e0f, f55ffb9, 6176c93, 03f1a58, 488962a, 1772dc9, cfbf23d, 161b221] # observed
claude_usage: null # no Claude Code session used
---

# Session 5 — Where are the whys and what in the refactorings?

## Highlights

Exercise 4 was completed and documented. The scheduler now lives in the
driving-adapter ring, accrues daily with catch-up across month ends and leap
days, pays the previous month on the first day, and skips overlapping runs.
Executor wiring was moved to exercise 7 and the README records that decision.

The session also had a refactoring pass: the overlap guard became one helper,
command construction gained names, the account list was renamed for its role,
`MutableClock` became a shared test fixture, the test construction was
centralized, and the constructor contract gained null-check tests.

## Where AI helped

- Explained `LocalDate.now(clock)` and `datesUntil`, then turned the catch-up
  bug into tests before fixing it.
- Corrected the obsolete claim that `synchronized` always pins virtual threads:
  JEP 491 removed that in JDK 24, and the project targets Java 25.
- Clarified that adapters may reference domain types as data but must not
  orchestrate use cases, and added the ArchUnit rule that keeps driving
  adapters off `application.service`.
- Wrote the concurrency tests and explained the `CountDownLatch` mechanics in
  detail.
- Applied the requested refactors one at a time once the process was corrected.

## Where AI got in the way

- The first daily overlap test passed without a guard because the same-day
  dedupe hid the missing protection. The test was redesigned to advance the
  clock while the first run was blocked.
- The agent implemented the first refactors without the requested explanation
  of why and what improved. The user called it out, and the remaining
  refactors were explained before being applied.
- The bundled Java language server repeatedly reported bogus syntax errors on
  files that compiled and tested cleanly, adding noise to the session.

## Notes

The user reported these discoveries: the `Clock` API and how to build date
ranges from it, `Stream.distinct()` for account ids, that adapters may use
domain objects for translation but should stay out of business orchestration,
and that `Runnable` is the standard no-argument, void-returning functional
interface rather than a thread-specific type.

The user also observed that the session over-emphasized concurrency and
multi-day edge cases. Those are scheduler concerns, not the hexagonal
architecture lesson, and the current test coverage was judged sufficient.

Deferred by agreement: the monthly payout only runs on day 1, catch-up state
is in memory until exercise 6, a partial multi-day run needs idempotent accrual
storage, and `ScheduledExecutorService` wiring belongs to exercise 7.
