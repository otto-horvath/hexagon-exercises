---
session: 0
date: 2026-10-16
start: null               # unknown: the session began in jvm-exercises; this repo was created at 00:29 (observed)
end: "00:51"              # observed, local time (-03:00)
active_minutes: 25        # estimated: user reported "< 30 min"
exercise: null
phases: [setup]
mood_start: null          # not asked
mood_end: 4               # reported (asked together with energy)
energy: 4                 # reported (asked together with mood)
confidence_before: null   # not asked
confidence_after: 2       # reported, on hexagonal architecture
tools:
  - name: Claude Code
    model: claude-opus-5-5
    source: observed
ai:
  helpfulness: 5          # reported
  hints_requested: 0      # observed
  hint_levels: []         # observed
  ai_wrote: [review, plan, setup, docs] # observed
  ai_mistakes: 1          # observed: overstated "trust @NullMarked, drop null checks", corrected in session
stack_added: [Gradle 9.2.0, JUnit Jupiter 5.12.1, AssertJ 3.27.3, Spotless 8.10.4, JSpecify 1.0.0] # observed
tests_passing: 0          # observed: no tests yet
score: null
commits: []               # observed: nothing committed yet
---

## Highlights
- Reviewed and scored the five jvm-exercises (4, 4, 4, 4, 3) and listed
  cross-cutting improvements: explicit `BigDecimal` scale, keeping exception
  causes, no exceptions as control flow, one source of truth per rule.
- Discussed the history of null handling: JSR-305 per-parameter annotations,
  `Objects.requireNonNull`, and today's JSpecify `@NullMarked` plus runtime
  checks at boundaries.
- Chose the domain for the hexagonal series: daily interest accrual on
  multi-currency balances with month-end payout, inspired by work but built
  only from public concepts.
- Planned 7 core exercises plus an optional bonus, with tools added one
  exercise at a time; wrote README, AGENTS.md, and the build setup.

## Where AI helped
- The code review gave concrete, file-level findings across all exercises.
- Comparing the library and FX domains made it clear that FX gives more
  reasons to swap adapters (external APIs, events, scheduling).
- Explained the test libraries (AssertJ, ArchUnit, Mockito, WireMock, H2,
  Testcontainers) when the user had no opinion on them yet.

## Where AI got in the way
- One overstated recommendation in the review ("trust `@NullMarked`, drop
  defensive null checks"). Without a null checker in the build, that would
  remove the only enforcement. Corrected when the null-handling question came
  up.

## Notes
- Session data collection was added to AGENTS.md during this session, so
  start time and starting mood were not captured.
