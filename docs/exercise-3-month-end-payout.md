# Exercise 3 — Month-end payout

Exercise 3 added the month-end use case to the interest domain. It gathers
daily accruals for an account and month, totals them by currency, rounds each
monthly total, records each payout idempotently, and publishes an event only
when a payout is newly recorded.

## What we learned

### Aggregate by currency before rounding

Accruals are summed as `Money` values grouped by currency. Each monthly total
is rounded to the currency's minor unit only when creating the payout. This
keeps daily accrual precision intact and ensures currencies are never added
together.

The query covers the month as a whole, so accruals from before and after a
mid-month rate change both contribute to the same currency's payout. The
use case does not recalculate interest or need to know why the daily amounts
differ; it pays the accruals already recorded by the domain flow.

### Make payout idempotency explicit

The payout repository uses the account, currency, and month as the natural
identity for a payout. Its result is a sealed `PayoutResult`: a newly recorded
payout or one that had already been paid. The service publishes `InterestPaid`
only for the newly recorded result, so retrying a month-end job does not emit a
duplicate event.

### Publish domain events through a driven port

`InterestEventPublisher` is a driven port owned by the application. The service
constructs an `InterestPaid` event from a newly saved payout and delegates
publication; it remains independent of any messaging technology. The event
describes the business fact that interest was paid.

### Keep architectural boundaries executable

The architecture tests now check the additional port boundaries: ports remain
independent of services and of one another, alongside the existing inward
dependency rules. These checks make the intended design constraints visible
and guard them as adapters and use cases are added.

## Discussions and decisions

- Payouts are grouped by currency, and only the completed monthly sum is
  rounded to the minor unit.
- Repository outcomes are represented as a sealed alternative rather than a
  boolean, making the difference between a new payout and a duplicate
  explicit to callers.
- Event publication follows successful creation of a new payout; an already
  paid result produces no second `InterestPaid` event.
- Accruals on either side of a mid-month rate change are included because the
  payout consumes the recorded monthly accruals, not a single monthly rate.
- The service's stream-based filtering and casting flow was identified as a
  possible cleanup, along with missing constructor null checks; that broader
  refactoring was deferred.

## Test checkpoints

- Tests cover aggregation by currency, minor-unit rounding, and idempotent
  retries with event publication only for new payouts.
- A scenario spanning a mid-month rate change verifies that both periods are
  included in the payout.
- Additional ArchUnit rules protect port boundaries.
- Final result: **35 tests passed**, and `./gradlew build` succeeded.

## Further reading

- Alistair Cockburn, [Hexagonal Architecture (Ports and Adapters)](https://alistair.cockburn.us/hexagonal-architecture/), the original article.
- [Java Language Specification: Sealed Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.6), the language specification for closed class hierarchies.
- Chris Richardson, [Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html), a reliable publication pattern relevant when the event port gains a persistent adapter.
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html), the official guide to executable architecture constraints.

## Remo's strip

| <img src="assets/remo.svg" width="120" alt="Remo building"> | <img src="assets/remo-red.svg" width="120" alt="Remo red"> | <img src="assets/remo-green.svg" width="120" alt="Remo green"> |
|---|---|---|
| **Built it:** a month-end use case to total recorded accruals and pay by currency | **Broke it:** the first exercise plan missed the additional ArchUnit rules, and the stream-based flow felt unsatisfying | **Fixed it:** added the architecture checks, used sealed outcomes for idempotency, and finished with all 35 tests passing |
