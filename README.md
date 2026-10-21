<p align="center">
  <img src="docs/assets/logo.svg" alt="hexagon-exercises: Remo the chameleon on a ports-and-adapters hexagon" width="720">
</p>

# Hexagonal Architecture Exercises

A test-first series for learning hexagonal architecture (ports and adapters)
in modern Java. One domain, **interest on multi-currency balances**, grows
across the exercises. Each step adds a port, an adapter, or a technology, and
all earlier tests must keep passing. The project targets Java 25, plain JDK
first, with a framework only as a final bonus.

[![Java CI with Gradle](https://github.com/otto-horvath/hexagon-exercises/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/otto-horvath/hexagon-exercises/actions/workflows/gradle.yml)

The series follows the same approach as
[jvm-exercises](https://github.com/otto-horvath/jvm-exercises): tests define
the behavior, the learner writes the production code, and each finished
exercise gets a learnings guide under `docs/`. See [AGENTS.md](AGENTS.md) for
the full workflow.

## Meet Remo 🦎

<img src="docs/assets/remo.svg" align="right" width="160" alt="Remo the chameleon, fading from red to green">

Remo is the series mascot: a chameleon who changes colour with the tests.

- **Red:** a failing test defines the next step.
- **Yellow:** work in progress, making it pass or refactoring.
- **Green:** every test passes.

Remo reports every `./gradlew test` run. Each exercise guide ends with
*Remo's strip*: **Built it → Broke it → Fixed it**, the real mess and redo of
that exercise, because learning is mostly building something, breaking it,
and rebuilding it better. His colours also mark the mood in the
[session logs](docs/sessions/).

## The domain

An **account** holds balances in several currencies. Every day the system
accrues interest on each end-of-day balance, and at month end it pays the
accrued interest out.

- **Balances** come from an external balance API (end-of-day amount per
  account, currency, and date).
- **Interest rates** come from an external rates API: an annual rate per
  currency, effective from a given date. Rates can change in the middle of a
  month.
- **Daily accrual** = `end-of-day balance × annual rate ÷ day-count basis`.
  Each currency follows a public money-market day-count convention:

  | Currency | Convention | Basis |
  |---|---|---|
  | GBP | ACT/365 Fixed | 365 |
  | EUR | ACT/360 | 360 |
  | USD | ACT/360 | 360 |

  Example: 10,000.00 GBP at 4.00% accrues `10000 × 0.04 ÷ 365 =
  1.0958904…` GBP for the day.
- **Precision:** daily accruals keep full precision; rounding to the
  currency's minor unit happens once, at payout.
- **Month-end payout:** sum the month's accruals per account and currency,
  round, pay, and publish an `InterestPaid` event. Payouts are **idempotent**:
  running the job twice pays once.
- **Scheduled jobs** trigger the daily accrual and the month-end payout.

```
             driving adapters                           driven adapters
  ┌───────────────────────────┐                ┌────────────────────────────┐
  │ Scheduler (daily/monthly) │──►┌────────┐──► │ Rates API client           │
  │ HTTP query/admin API      │──►│  core  │──► │ Balance API client         │
  └───────────────────────────┘   │ domain │──► │ Accrual/payout repository  │
                                  │ + use  │──► │ Event publisher (outbox)   │
                                  │ cases  │──► │ Clock                      │
                                  └────────┘    └────────────────────────────┘
```

All APIs, contracts, and data in this repository are invented for learning,
built only from public concepts.

## Package layout

Packages first, Gradle modules later (exercise 8). ArchUnit rules enforce the
boundaries from exercise 2 onward.

```
hexagon.interest
├── domain                  # entities, value objects, domain events, rules — no I/O, no frameworks
├── application
│   ├── port.in             # driving ports: use-case interfaces, commands, results
│   ├── port.out            # driven ports: what the core needs from the outside
│   └── service             # use-case implementations
└── adapter
    ├── in                  # scheduler, HTTP
    └── out                 # API clients, persistence, event publishing
```

Dependencies point inward only: `adapter → application → domain`.

## Schedule

Seven core exercises over about three weeks (two or three per week), plus an
optional bonus in week four.

| Week | Exercise | Status |
|---|---|---|
| 1 | 1. Accrual domain | solved |
| 1 | 2. Accrue-interest use case | solved |
| 1–2 | 3. Month-end payout | |
| 2 | 4. Scheduler as a driving adapter | |
| 2–3 | 5. External API adapters | |
| 3 | 6. Persistence and outbox | |
| 3 | 7. HTTP adapter and wiring | |
| 4 | 8. Modules and a framework *(optional)* | |

## Exercises

### 1. Accrual domain — solved

Model money, rates, and day-count conventions, and calculate one day of
interest for a balance. No ports and no I/O: just the core.

**What you'll learn**

- Model value objects with records: `Money`, `AnnualRate`,
  `DayCountConvention`.
- Keep the domain free of I/O and frameworks.
- Define a precision and rounding policy with `BigDecimal` and
  `java.util.Currency`.
- Cover conventions and edge cases with parameterized tests.

**New in this step:** Gradle, JUnit 5, AssertJ, parameterized tests.

See the [Exercise 1 guide](docs/exercise-1-accrual-domain.md) for the domain
decisions, precision policy, and lessons from the implementation.

### 2. Accrue-interest use case — solved

Accrue one day of interest for an account: fetch the balance and the rate,
calculate, and store the accrual.

**What you'll learn**

- Separate the driving port (the use case) from driven ports (rates,
  balances, accruals).
- Write an application service that coordinates the domain without holding
  rules itself.
- Test use cases with hand-written in-memory fakes.
- Enforce the dependency rule with a first architecture test.

**New in this step:** ArchUnit (first rule).

See the [Exercise 2 guide](docs/exercise-2-accrue-interest-use-case.md) for
the ports, application service, dependency rules, and lessons from the
implementation.

### 3. Month-end payout

Pay the month's accrued interest per account and currency, exactly once.

**What you'll learn**

- Publish domain events (`InterestPaid`) through a driven port.
- Make payouts idempotent with natural keys (account, currency, month).
- Model payout states with a sealed type.
- Handle a rate change in the middle of the month.

**New in this step:** more ArchUnit rules.

### 4. Scheduler as a driving adapter

Trigger the daily accrual and the month-end payout from scheduled jobs.

**What you'll learn**

- Treat a scheduler as just another driving adapter.
- Inject time through `Clock` instead of calling `now()`.
- Catch up on missed days, and handle month ends and leap years.
- Prevent overlapping job runs.

**New in this step:** `ScheduledExecutorService`, `java.time.Clock`.

### 5. External API adapters

Replace the rates and balance fakes with HTTP clients for the external APIs.

**What you'll learn**

- Translate external models at the boundary (anti-corruption layer).
- Configure timeouts and retries, and map API errors to domain errors.
- Test HTTP clients against a stubbed server, not the real service.

**New in this step:** `java.net.http.HttpClient`, Jackson, WireMock.

### 6. Persistence and outbox

Store accruals and payouts in a database, and publish events reliably.

**What you'll learn**

- Implement repositories with JDBC behind the existing driven ports.
- Apply the transactional outbox pattern for event publishing.
- Run one contract test suite against both the in-memory and JDBC adapters.
- Manage schema changes with migrations.

**New in this step:** H2, Flyway.

### 7. HTTP adapter and wiring

Expose "interest accrued so far this month" and "re-run payout" over HTTP,
and wire the whole application by hand.

**What you'll learn**

- Add a second driving adapter that calls the same use cases as the
  scheduler.
- Map between DTOs and the domain without leaking logic into adapters.
- Build a composition root in `main` with manual dependency injection.
- Enforce architecture rules across the whole application.

**New in this step:** JDK `HttpServer`.

### 8. Modules and a framework *(optional)*

Split the hexagon into Gradle modules, then add a framework at the edge only.

**What you'll learn**

- Let the compiler enforce boundaries with `domain`, `application`, and
  `adapters` modules.
- Introduce Spring Boot as an adapter without changing the core.
- Optionally, run adapters against real infrastructure in containers.

**New in this step:** Gradle modules, Spring Boot, optional Testcontainers
and Kafka.

## Session log and series stats

Every working session is logged under [docs/sessions/](docs/sessions/):
time, mood, confidence, tech added, how the AI tools and models helped, and
what each Claude Code session used in tokens and cost
([scripts/claude_usage.py](scripts/claude_usage.py)).
When the series is finished, those logs feed a series status page with data
and an honest opinion on the AI-assisted workflow.

Session logging is optional and not needed to do the exercises. In your own
fork, agents ask before logging; delete `docs/sessions/` if you don't want the
author's logs.

## References

- Alistair Cockburn,
  [*Hexagonal Architecture*](https://alistair.cockburn.us/hexagonal-architecture/)
  (2005), the original article.
- Alistair Cockburn and Juan Manuel Garrido de Paz,
  [*Hexagonal Architecture Explained*](https://alistaircockburn.com/Books)
  (2024; updated edition 2025), listed on the author's books page.
- Tom Hombergs,
  [*Get Your Hands Dirty on Clean Architecture*](https://reflectoring.io/book/),
  2nd edition.
- Netflix Technology Blog,
  [*Ready for changes with Hexagonal Architecture*](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)
  (2020).
- Herberto Graça,
  [*DDD, Hexagonal, Onion, Clean, CQRS, … How I put it all together*](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)
  (2017).
- Steve Freeman and Nat Pryce,
  [*Growing Object-Oriented Software, Guided by Tests*](http://www.growing-object-oriented-software.com/)
  (2009).
- Martin Fowler,
  [*Mocks Aren't Stubs*](https://martinfowler.com/articles/mocksArentStubs.html)
  and [*ContractTest*](https://martinfowler.com/bliki/ContractTest.html).
- Eric Evans, [*Domain-Driven Design*](https://www.domainlanguage.com/ddd/)
  (2003): value objects and anti-corruption layers.
- Chris Richardson, microservices.io,
  [*Pattern: Transactional outbox*](https://microservices.io/patterns/data/transactional-outbox.html).
- Brandur Leach, Stripe blog,
  [*Designing robust and predictable APIs with idempotency*](https://stripe.com/blog/idempotency)
  (2017).

Each exercise guide links the specific sources it relies on.

## Working with the project

Install a Java 25 JDK and use the Gradle wrapper (`./gradlew` on macOS/Linux,
`.\gradlew.bat` on Windows):

```shell
./gradlew build
```

Run one test class while working on an exercise:

```shell
./gradlew test --tests 'hexagon.interest.domain.MoneyTest'
```

Code is formatted with [Spotless](https://github.com/diffplug/spotless) using
Palantir Java Format: run `./gradlew spotlessApply` to format. `./gradlew
build` includes `spotlessCheck`, so CI fails on unformatted code.

## License

Copyright © 2026 Otto Horvath. Original source:
<https://github.com/otto-horvath/hexagon-exercises>.

All content in this repository, including code, exercises, docs, and Remo the
chameleon, is licensed under
[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). See
[LICENSE](LICENSE). The exception is the Gradle wrapper (`gradlew`,
`gradlew.bat`, `gradle/wrapper/`), which keeps its own Apache-2.0 license,
and [scripts/claude_usage.py](scripts/claude_usage.py), which is
[MIT](https://opensource.org/license/mit) so anyone can use it freely,
including at work.

Feel free to fork, run, change, and share it. Keep the credit to the author,
link to the original source, and share your changes under the same license.

**Teaching note:** paid teaching using this material, such as courses,
workshops, mentoring, and company training, is fine. Selling the material
itself, or a repackaged version of it, is not.
