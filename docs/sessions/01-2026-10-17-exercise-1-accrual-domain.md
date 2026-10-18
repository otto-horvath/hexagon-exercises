---
session: 1
title: "Now that you know the solution, what would you do differently?" # reported reflection question
date: 2026-10-17 # observed: local date at session start
start: "20:00" # reported by user as 8pm BRT (-03:00); first date capture was 20:52
end: "01:10" # observed, local time (-03:00), following calendar day
active_minutes: 220 # estimated: 20:00 to 01:10 = 310 minutes elapsed, minus user's reported 90-minute cooking/dinner break
exercise: 1
phases: [start, guide, evaluate, wrap-up]
mood_start: 3 # reported, 1–5; initially described as curious and calm
mood_end: 3 # reported, 1–5; similar to start, with a qualitative slight uplift from learning and dinner
energy: 3 # reported, 1–5; initially described as medium
confidence_before: 3 # reported, 1–5, on Exercise 1 domain modeling and monetary calculations
confidence_after: 3.5 # reported, 1–5; user gave 3,5
tools:
  - name: OpenCode
    model: gpt-6-luna
    source: observed
ai:
  helpfulness: 4 # reported, 1–5
  hints_requested: null # not formally counted during the session
  hint_levels: [concept, api, code-shape] # observed
  ai_wrote: [tests, scaffold, production-refactor, exercise-guide, session-log, agent-instructions] # observed; production refactor explicitly authorized by user
  ai_mistakes: 5 # observed: initially weak Money test, string-switch recommendation, missing test import, stale patch context, failed to prompt for numeric session ratings
stack_added: [] # observed: no dependencies added in Exercise 1
tests_passing: 26 # observed: full build after the final code refactor
score: 4 # observed: Exercise 1 evaluation
commits: [6b4fdb5] # observed: Exercise 1 implementation and guide commit; session log committed separately
---

# Session 1 — Now that you know the solution, what would you do differently?

## Highlights

- Started Exercise 1 on `exercise-1-accrual-domain`, keeping the work in the
  domain package with no ports or adapters.
- Built and tested `Money`, `AnnualRate`, `DayCountConvention`, and daily
  interest calculation. The learner implemented production behavior; after
  explicit permission, the assistant performed the convention-map refactor.
- Chose 10 decimal places with `HALF_UP` for daily accrual and currency minor
  units with `HALF_UP` at payout. Confirmed that currency metadata provides
  the minor-unit scale, while the rounding mode remains domain policy.
- Replaced independently supplied conventions with selection from the
  balance's currency, preventing currency/convention mismatches.
- Updated the schedule to list each exercise separately and mark only Exercise
  1 as solved.
- Kept the current model after discussing task-based model selection.
- The user noticed that this model's precision and method-naming choices
  resembled decisions in their work project, and suggested tracking whether
  different models converge on similar approaches. This is a user-reported
  observation; the other model identities were not provided.

## Where AI helped

- Explained the core/domain/port/adapter picture before continuing the
  implementation.
- Worked through the daily-interest formula and the `BigDecimal.divide`
  overload needed for a repeating quotient.
- Added parameterized tests for currency conventions, day-count calculations,
  and minor-unit rounding; checked the Java SE 25 API references.
- The `Currency`-keyed map made the convention lookup more type-aligned and
  easier to understand than matching on currency-code strings.

## Where AI got in the way

- The first `Money` test only checked record accessors; the user pointed out
  that removing the scaffold exception made it pass without testing a
  meaningful domain rule. Stronger behavior tests followed.
- The assistant initially recommended switching on currency-code strings.
  The user challenged that choice, leading to the `Map<Currency,
  DayCountConvention>` design.
- One test patch initially omitted the JUnit `@Test` import, and a later patch
  context was stale after formatting. Both were corrected during the session.

## Notes

- The user initially planned 40 minutes. Active time is estimated from the
  reported 20:00 start and the observed 00:48 end, less the user-reported
  90-minute cooking/dinner break; elapsed time includes that break.
- Starting mood and energy, and the later mood change, were first described
  qualitatively; the user later provided numeric ratings for the log.
- The first `date` capture happened at 20:52 local time (-03:00), after work
  had started; the user reported the actual start as 20:00 BRT.
- The model-selection analysis is included in the Exercise 1 guide appendix,
  not the series setup notes.
- After dinner and wine, the user had a good conversation with his girlfriend
  about life and the exercise, centered on the question: “Now that you know
  the solution, what would you do differently?”
