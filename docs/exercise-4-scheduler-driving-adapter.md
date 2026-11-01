# Exercise 4 — Scheduler as a driving adapter

Exercise 4 added the scheduler as a second driving adapter. The daily and
monthly jobs call the existing driving ports, take time from an injected
`Clock`, catch up on missed days, and skip overlapping runs. Executor wiring
was deliberately deferred to exercise 7, so this guide covers the job logic and
the adapter boundary, not `ScheduledExecutorService` configuration.

## What we learned

### A scheduler is just another driving adapter

`hexagon.interest.adapter.in.scheduler` depends on `AccrueInterest` and
`PayMonthlyInterest`, the same driving ports the HTTP adapter will use later.
It never touches a service, a repository, or a domain rule, and nothing in
`application` or `domain` imports it. The dependency arrow still points inward.

The adapter owns only technical concerns: the configured list of account and
currency pairs, the clock, the daily and monthly triggers, and the overlap
guard. The business decision about what a day of interest is remains in the
domain and application layers.

### Time is an input, not a static call

The scheduler reads today with `LocalDate.now(clock)` instead of calling
`LocalDate.now()` directly. Tests therefore replace time with a `Clock.fixed`
or a small mutable clock, and a run can be moved across a month end or a leap
day without waiting.

Catch-up uses `LocalDate.datesUntil`, which is an exclusive range:

```java
firstDate.datesUntil(today.plusDays(1)); // firstDate .. today, inclusive
```

### Catch-up state belongs to the last successful run

The adapter keeps `lastAccruedDate`. The first run accrues today; later runs
accrue from the day after the last success through today. The state is assigned
only after every accrual in the run has completed, so a failed run is retried
instead of being silently marked done.

Two tests made this concrete:

- `accruesEachDayOnlyOnceAcrossConsecutiveRuns` exposed an implementation that
  updated the state only on the first run, replaying the same days on every
  later tick.
- `retriesTheFirstDayWhenTheInitialRunFails` exposed an implementation that
  advanced the state before the first accrual succeeded.

A multi-day retry can still replay days that were stored before a later day
failed. That is acceptable only if accrual storage is idempotent per account,
currency, and date, which belongs to exercise 6.

### Month-end is a date decision, not domain logic

On the first day of a month, the monthly job pays the previous month for every
distinct account. `YearMonth.from(currentDate)` keeps the month derived from
the same date the job already read, and `distinct()` collapses multiple
currencies because `PayMonthlyInterest` pays all currencies for an account.

The leap-day case is covered by the existing date types: the catch-up range
walks 28, 29 February, and 1 March in 2028 without special handling.

A known limitation remains: if the process is down on the first day, that
month is not paid later. Payout idempotency would allow paying the last
completed month on later ticks, but that behavior was left for a later
decision.

### Overlap: skip the tick, do not queue it

Each job has its own `AtomicBoolean`. A run takes the flag with
`compareAndSet(false, true)`, does its work, and resets the flag in `finally`.
A second tick that arrives while the first is still running returns
immediately.

This is deliberately not a blocking lock: a scheduler tick that cannot start
has no value if it later runs with stale inputs. It is also not one shared
flag, so a slow daily run does not suppress the month-end payout.

### The boundary is executable

A new ArchUnit rule keeps driving adapters on driving ports:

```java
noClasses()
        .that()
        .resideInAPackage("hexagon.interest.adapter.in..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("hexagon.interest.application.service..");
```

Adapters may still reference domain types when translating data, but they must
not orchestrate use cases or bypass a driving port.

## Discussions and decisions

- Executor wiring moved to exercise 7. The README now lists only `Clock` as new
  in exercise 4 and `ScheduledExecutorService` as deferred to exercise 7.
- The monthly job runs on the first day for the previous month. This keeps the
  final day's accrual closed before payout, at the cost of missing the month
  when the process is down on that first day.
- Catch-up state stays in memory for now. Restart-safe state and idempotent
  accrual storage are exercise 6 topics.
- Overlap prevention is in-process. Multiple application instances would each
  run their own scheduler and would need a distributed lock or idempotent
  storage.
- Concurrency and multi-day tests were judged sufficient. They are scheduler
  concerns; the hexagonal lesson is the adapter-to-port boundary, not an
  exhaustive scheduler test suite.
- Refactors were kept behavior-preserving: extract the overlap guard, name
  command construction, rename the account list, share the mutable clock, add
  a test constructor helper, and cover constructor validation.

## Mistakes and how they were fixed

- The first daily overlap test passed without a guard because the same-day
  dedupe made the second tick a no-op. The test now advances the clock while
  the first run is blocked, so only a real overlap guard can make it pass.
- Catch-up state advanced only on the first run, so a third consecutive run
  replayed earlier days. A consecutive-run test exposed it; the state now
  advances after every successful run.
- The first-run path still advanced the state before the accrual succeeded. A
  retry test exposed it; the range helper became side-effect free and the state
  is written after the work.
- An extra `plusDays(1)` made the range start one day late. It skipped today
  and made same-day runs produce a negative range; the existing tests caught
  it immediately.
- The agent implemented refactors without the requested explanation of why and
  what improved. The user called it out, and the remaining refactors were
  explained one by one before being applied.

## Test checkpoints

- Daily accrual for every configured account and currency.
- Catch-up across a month end and 29 February.
- No duplicate accrual on repeated or consecutive runs.
- Retry after a failed first run.
- Month-end payout on the first day, deduplicated by account, and no payout on
  other days.
- Overlapping daily and monthly runs are skipped.
- Constructor validation.
- Final result: **46 tests passed**, and `./gradlew build` succeeded.

## A question to carry forward

The overlap guard is an in-process `AtomicBoolean`, and `lastAccruedDate` lives
in memory. What changes when two application instances run the scheduler at the
same time, or when one restarts in the middle of a missed-day range? A
distributed lock, an idempotent repository, or both? Keep the payout
idempotency design in mind when persistence arrives in exercise 6.

## Further reading

- [Java SE 25 `Clock`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/Clock.html):
  injectable time and the `fixed` clock used in tests.
- [`LocalDate.datesUntil`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/LocalDate.html#datesUntil(java.time.LocalDate)):
  the exclusive date range behind catch-up.
- [`ScheduledExecutorService`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html):
  the API that will run the jobs in exercise 7.
- [`AtomicBoolean`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/atomic/AtomicBoolean.html):
  the compare-and-set primitive behind the overlap guard.
- [`CountDownLatch`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/CountDownLatch.html):
  the coordination primitive used by the concurrency tests.
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491):
  why `synchronized` no longer pins virtual threads from JDK 24 onward.
- Alistair Cockburn,
  [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/):
  the original ports-and-adapters article.

## Appendices

### Appendix 1 — Testing overlap with `CountDownLatch`

The concurrency tests use three pieces of state:

- `calls`: an `AtomicInteger` incremented by the fake use case. It records
  whether the second tick reached the port; the expected value is `1`.
- `entered`: a one-shot latch counted down by the first run after it enters the
  fake. The test awaits it, so the second tick is guaranteed to start while the
  first run is genuinely in progress.
- `release`: a one-shot latch the first run waits on. The test counts it down
  only after checking the second tick, letting the first run finish.

The sequence is:

1. Start the first run in a daemon thread.
2. `entered.await(1, SECONDS)` returns `true`; the first run is inside the fake.
3. Start the second run and `join(1_000)`. If the guard works, it returns
   immediately and `isAlive()` is false. If the implementation blocks or
   queues, the thread is still alive and the assertion fails.
4. `release.countDown()`, join the first thread, and assert `calls == 1`.

A latch never goes negative: counting down an already-zero latch is a no-op.
The `join` timeout distinguishes “skipped” from “blocked waiting”, and daemon
threads prevent a failed assertion from hanging the test JVM.

### Appendix 2 — The overlap guard template

```java
private final AtomicBoolean dailyRunning = new AtomicBoolean();
private final AtomicBoolean monthlyRunning = new AtomicBoolean();

public void runDaily() {
    runIfIdle(dailyRunning, this::accrueDueDays);
}

public void runMonthly() {
    runIfIdle(monthlyRunning, this::payCompletedMonth);
}

private void runIfIdle(AtomicBoolean running, Runnable job) {
    if (!running.compareAndSet(false, true)) {
        return;
    }

    try {
        job.run();
    } finally {
        running.set(false);
    }
}
```

- `AtomicBoolean`, not a `volatile boolean`: the check and the set must be one
  atomic operation.
- Separate flags per job: a slow daily run must not suppress the monthly payout.
- Instance fields, not `static`: the flag belongs to the scheduler instance,
  and static state breaks tests and multiple instances.
- Reset in `finally`: one exception must not leave a job permanently
  “running”.
- `synchronized` would make the second tick wait; the requirement is to skip
  it.

`Runnable` is the standard no-argument, void-returning functional interface;
it does not imply threads. It only names a unit of work, so the guard works for
any job without a custom interface.

### Appendix 3 — A note on `synchronized` and virtual threads

Before JDK 24, a virtual thread blocked inside `synchronized` or `Object.wait`
pinned its carrier platform thread. JEP 491, delivered in JDK 24, removed that
pinning. On the Java 25 target of this project, `synchronized` is no longer a
reason to prefer `ReentrantLock`.

Pinning can still occur when a virtual thread blocks in a native or foreign
call, during class initialization, or while waiting for class initialization.
None of that changes the choice here: the guard uses `AtomicBoolean` to skip a
tick, not to avoid pinning.

## Remo's strip

| <img src="assets/remo.svg" width="120" alt="Remo building"> | <img src="assets/remo-red.svg" width="120" alt="Remo red"> | <img src="assets/remo-green.svg" width="120" alt="Remo green"> |
|---|---|---|
| **Built it:** a driving adapter that accrues daily, catches up across month ends and leap days, pays the previous month, and skips overlapping runs | **Broke it:** the overlap test first passed for the wrong reason, catch-up replayed days, and the refactors ran ahead of their explanations | **Fixed it:** added the clock-rollover test, advanced state only after success, retried the failed first day, and explained each refactor before applying it; 46 tests green |
