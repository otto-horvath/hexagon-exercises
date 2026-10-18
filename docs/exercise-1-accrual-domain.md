# Exercise 1 — Accrual domain

Exercise 1 built the inside of the hexagon first: immutable money and rate
values, currency-specific day-count conventions, and a pure daily-interest
calculation. There are no ports, adapters, I/O, or framework dependencies in
this exercise.

## What we learned

### Model domain values explicitly

`Money`, `AnnualRate`, and `DayCountConvention` are records. `Money` keeps an
amount and its `Currency` together, rejects null components, permits addition
only within one currency, and preserves sub-minor-unit precision during that
addition. A 4% annual rate is represented as the decimal fraction `0.04`, not
`4.00`.

`Money` owns rounding to a currency's minor unit. It asks
`Currency.getDefaultFractionDigits()` for the scale, then applies the selected
`HALF_UP` rounding mode. This operation is explicit: constructing or adding
`Money` values does not round them. The payout use case in Exercise 3 can call
it when money is actually paid.

The currency supplies a default scale, but the rounding mode is still domain
policy. For example, JPY has zero default fraction digits, so rounding `123.5`
JPY to its minor unit with `HALF_UP` gives `124` JPY. A pseudo-currency such as
`XXX` has no defined minor unit, so `Money` rejects that rounding request.

### Apply the currency's day-count convention

The domain maps supported currencies to the conventions in the exercise:

| Currency | Convention | Basis |
|---|---|---:|
| GBP | ACT/365 Fixed | 365 |
| EUR | ACT/360 | 360 |
| USD | ACT/360 | 360 |

`Currency` is a class, not an enum; values come from `Currency.getInstance`.
Rather than switch on currency-code strings or create a second enum for the
same currencies, `DayCountConvention` holds an immutable map keyed by
`Currency` values. `InterestCalculator` derives the convention from the
balance's currency, so callers cannot accidentally supply a convention for a
different currency.

### Make decimal precision a deliberate policy

Daily interest is calculated as:

```text
balance × annual rate ÷ days per year
```

For example, £36,500 at 1% with ACT/365 accrues £1 for one day. The README's
£10,000 at 4% example instead produces `1.095890410958…` GBP. The no-argument
`BigDecimal.divide` asks for an exact result and throws `ArithmeticException`
when the decimal expansion does not terminate.

This exercise chose a scale of **10 decimal places with `HALF_UP`** for daily
accrual. The example therefore becomes `1.0958904110` GBP. This is an explicit
intermediate-precision policy, not a market-wide standard. It remains distinct
from payout rounding to a currency's minor unit. `BigDecimal` is immutable, so
operations such as `divide` and `setScale` return a new value.

## Architecture boundary

`InterestCalculator` is domain logic: it combines domain values and rules but
does not know about accounts, API clients, schedulers, databases, or event
publishers. Ports and adapters begin in later exercises when an application
use case needs those external capabilities. This keeps the first exercise
focused on the core of the hexagon.

## Gotchas and decisions

- The first `Money` test only checked the record's generated accessors. That
  was a useful shape check, but not a meaningful domain rule by itself; the
  stronger contracts became same-currency addition, rejecting mixed
  currencies, preserving precision, and explicit rounding.
- `Currency.GBP` does not exist. `Currency` is not an enum; use `getInstance`
  and compare or map `Currency` values.
- Matching on a currency-code string is easy to mistype (a leading space in
  the USD case caused a failed test). The typed map makes currency values the
  actual lookup keys.
- Exact `BigDecimal` division works for terminating quotients, but a repeating
  quotient requires an explicit scale and rounding mode.
- The day-count convention is selected from the balance's currency instead
  of being an independent calculator argument. This closes a mismatch the
  earlier API shape allowed.
- `getDefaultFractionDigits()` can return `-1` for currencies without a minor
  unit; passing that through to `setScale` would not mean “no rounding
  possible.” The domain rejects the request explicitly.

## Appendix: choosing AI models for this series

The model discussion led to a task-based plan rather than switching models
often: keep the current model for the normal test-first tutoring loop; use a
stronger reasoning model for broad architecture reviews or difficult
cross-cutting failures; use web-enabled research when API or product details
may have changed; reserve faster, lighter models for narrow mechanical tasks.
Compare models on the same real tasks and keep the least costly one that meets
the quality bar. Model names, capabilities, and availability change, so these
are workflow guidelines rather than a permanent ranking.

For this project, preserving the learning goal matters more than raw code
output: the assistant writes tests and scaffold, explains and reviews, while
the learner writes production code. The user chose to stay with the current
model for the series.

## Further reading

- Alistair Cockburn, [Hexagonal Architecture (Ports and Adapters), original
  2005 article](https://alistair.cockburn.us/hexagonal-architecture/).
- Java SE 25 [`BigDecimal`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html), including the
  [`divide`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html#divide(java.math.BigDecimal,int,java.math.RoundingMode))
  and [`setScale`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html#setScale(int,java.math.RoundingMode)) methods.
- Java SE 25 [`Currency.getDefaultFractionDigits`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Currency.html#getDefaultFractionDigits), which reports the currency's default minor-unit scale.
- Java SE 25 [`Map`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Map.html), including the unmodifiable `Map.of` factory.
- OpenAI, [Model selection](https://platform.openai.com/docs/guides/model-selection), for task-based model and reasoning-effort guidance.

## Remo's strip

| <img src="assets/remo.svg" width="120" alt="Remo building"> | <img src="assets/remo-red.svg" width="120" alt="Remo red"> | <img src="assets/remo-green.svg" width="120" alt="Remo green"> |
|---|---|---|
| **Built it:** a `Money` record and the first daily-interest example | **Broke it:** a string-switch typo missed USD, and exact division failed on a repeating decimal | **Fixed it:** currency-keyed conventions and an explicit precision policy kept the domain rules visible |
