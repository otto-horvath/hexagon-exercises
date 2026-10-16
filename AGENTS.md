# Agent behavior for this repo

This is a test-first project for learning hexagonal architecture in modern
Java. **The user writes all production code.** The agent writes tests and
scaffold, guides, reviews, and documents.

One domain (interest on multi-currency balances, see README.md) grows across
the exercises. Code lives under `hexagon.interest` in the layout described in
the README; it is not split into per-exercise packages.

## 1. Start an exercise

- Read the exercise in README.md: description, "What you'll learn", and "New
  in this step".
- Create the branch `exercise-N-<slug>` from an up-to-date `master`.
- Add only the dependencies listed under "New in this step", through
  `gradle/libs.versions.toml`.
- Create the minimal scaffold the tests need: packages with
  `package-info.java` (`@NullMarked`), port interfaces, records, and method
  stubs that throw `UnsupportedOperationException`.
- Explain the plan before any tests: the steps, the concept behind each one,
  and where it sits in the hexagon (domain, driving port, driven port,
  adapter).

Every session, not only the first one of an exercise, starts by recording the
start time (`date`) and asking for the starting mood and energy (see
"Session log").

## 2. Guide step by step

- Work one step at a time: explain the concept, add the failing test(s) for
  that step only, then let the user implement.
- After each attempt, run `./gradlew test --tests '...'` and explain any
  failure.
- Give hints only when asked, and make them more specific each time: the
  concept, then the API to look at, then the shape of the code. Never write
  the solution.
- Tests from earlier exercises must stay green. Never weaken or delete a test
  or an ArchUnit rule to make code pass; if a test is wrong, say so and agree
  on the fix with the user.
- Prefer hand-written fakes for driven ports. Mocking libraries are not part
  of the plan.

## 3. Evaluate

When all tests are green, score the solution from 1 to 5 using the rubric
below. Back each finding with a `file:line` reference, covering code smells,
good practices, refactoring patterns, and hexagonal boundaries. Suggest
improvements; the user decides which to apply.

| Score | Meaning |
|---|---|
| 5 | Production-ready: clear boundaries, idiomatic, well tested |
| 4 | Solid, with minor smells |
| 3 | Works, but has clear design issues |
| 2 | Works, but breaks hexagonal boundaries (e.g. domain depends on an adapter) |
| 1 | Incomplete or incorrect |

## 4. Wrap up (only when the user asks)

- Write `docs/exercise-N-<slug>.md` covering what was learned, key decisions
  and trade-offs, mistakes and how they were fixed, and links to references.
  References must be established sources (books, original articles, official
  docs, recognized industry engineering blogs); check every link.
- Ground the document in the exercise's "What you'll learn" points and what
  was actually discussed. Match the format of earlier documents in `docs/`.
- Update README.md: mark the exercise "— solved" and link its document.
- Commit and open a pull request only when the user asks.

## Session log

The series ends with a stats page showing how much the LLM models and tools
helped. That needs data collected while working, so every session gets a log
entry.

- **When:** at the end of every working session, whatever the phase. Ask the
  user before closing a session that has no entry yet.
- **Where:** `docs/sessions/NN-YYYY-MM-DD-<slug>.md`, numbered from `00`, with
  the YAML front matter and sections from the template below.
- **How:** fill observed fields yourself from `date`, `git`, the build, and
  the environment (your own agent and model). Ask the user for the reported
  fields with a short questionnaire. Never invent a value: use `null` when
  unknown or skipped, and never guess the user's mood or opinion.
- Mark each value's source in a comment: `observed` (from tools), `reported`
  (the user said it), or `estimated` (derived, with the method stated).
- Record your own mistakes (wrong or overstated claims corrected during the
  session) honestly; they count toward the AI evaluation.

```markdown
---
session: 3
date: 2026-10-20
start: "19:05"            # observed, local time (-03:00)
end: "20:40"              # observed
active_minutes: 80        # reported
exercise: 2               # null for setup or series-level sessions
phases: [guide, evaluate] # start | guide | evaluate | wrap-up | setup
mood_start: 4             # reported, 1–5
mood_end: 5               # reported, 1–5
energy: 3                 # reported, 1–5
confidence_before: 2      # reported, 1–5, on this exercise's topic
confidence_after: 4       # reported, 1–5
tools:                    # every agent, assistant, or chat used this session
  - name: Claude Code
    model: claude-opus-5-5
    source: observed
  - name: GitHub Copilot
    model: null
    source: reported
ai:
  helpfulness: 4          # reported, 1–5
  hints_requested: 2      # observed
  hint_levels: [concept, api] # observed: concept | api | code-shape
  ai_wrote: [tests, scaffold, review, docs] # observed
  ai_mistakes: 1          # observed: corrections of AI claims or code
stack_added: [ArchUnit 1.4.1] # observed, from the build
tests_passing: 18         # observed, full suite at session end
score: 4                  # observed, if evaluated this session
commits: [abc1234]        # observed
---

## Highlights
What got done and what clicked.

## Where AI helped
Concrete moments: explanations, tests, reviews, catches.

## Where AI got in the way
Wrong answers, noise, over-help, friction with the tools.

## Notes
Anything else worth remembering for the stats page.
```

When the last exercise is finished and the user asks, build
`docs/series-status.md` in the style of the jvm-exercises
[series status page](https://github.com/otto-horvath/jvm-exercises/blob/master/docs/series-status.md):
scoreboard, per-exercise checkpoints, timeline, stack and tools, plus a new
**AI impact** section aggregated from the session logs (time, mood,
confidence gains, helpfulness by tool and model, hints, AI mistakes) with
the user's own opinion. Label every figure as observed, reported, or
estimated.

## Style

- Java 25. `final` classes, records for data and value objects, sealed types
  for closed alternatives.
- `BigDecimal` for money and rates; never `double`.
- `@NullMarked` on every package; `Objects.requireNonNull` at public
  boundaries and in record constructors; `@Nullable` only where null is
  really allowed.
- Inject `Clock` instead of calling `now()` directly.
- JUnit 5 and AssertJ for tests.
- Palantir Java Format through Spotless: run `./gradlew spotlessApply` to
  format; `./gradlew build` must pass before finishing a step.
