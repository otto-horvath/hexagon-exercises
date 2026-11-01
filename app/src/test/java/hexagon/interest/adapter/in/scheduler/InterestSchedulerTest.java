package hexagon.interest.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import hexagon.interest.application.port.in.AccrueInterest;
import hexagon.interest.application.port.in.AccrueInterestCommand;
import hexagon.interest.application.port.in.PayMonthlyInterest;
import hexagon.interest.application.port.in.PayMonthlyInterestCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InterestSchedulerTest {
    private static final String ACCOUNT_ID = "account-123";
    private static final String SECOND_ACCOUNT_ID = "account-456";
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency USD = Currency.getInstance("USD");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Test
    void accruesTodayForEveryConfiguredAccountAndCurrency() {
        var accruals = new RecordingAccrueInterest();
        var scheduler = new InterestScheduler(
                Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC),
                accruals,
                new RecordingPayMonthlyInterest(),
                List.of(new AccountCurrency(ACCOUNT_ID, GBP), new AccountCurrency(ACCOUNT_ID, USD)));

        scheduler.runDaily();

        assertThat(accruals.commands)
                .containsExactly(
                        new AccrueInterestCommand(ACCOUNT_ID, GBP, TODAY),
                        new AccrueInterestCommand(ACCOUNT_ID, USD, TODAY));
    }

    @Test
    void accruesEveryMissedDayAcrossMonthEndAndLeapDay() {
        var clock = new MutableClock(LocalDate.of(2028, 2, 27));
        var accruals = new RecordingAccrueInterest();
        var scheduler = new InterestScheduler(
                clock, accruals, new RecordingPayMonthlyInterest(), List.of(new AccountCurrency(ACCOUNT_ID, GBP)));

        scheduler.runDaily();
        clock.setToday(LocalDate.of(2028, 3, 2));
        scheduler.runDaily();

        assertThat(accruals.commands)
                .containsExactly(
                        accrual(LocalDate.of(2028, 2, 27)),
                        accrual(LocalDate.of(2028, 2, 28)),
                        accrual(LocalDate.of(2028, 2, 29)),
                        accrual(LocalDate.of(2028, 3, 1)),
                        accrual(LocalDate.of(2028, 3, 2)));
    }

    @Test
    void doesNotAccrueTheSameDayTwice() {
        var clock = new MutableClock(TODAY);
        var accruals = new RecordingAccrueInterest();
        var scheduler = new InterestScheduler(
                clock, accruals, new RecordingPayMonthlyInterest(), List.of(new AccountCurrency(ACCOUNT_ID, GBP)));

        scheduler.runDaily();
        scheduler.runDaily();

        assertThat(accruals.commands).containsExactly(accrual(TODAY));
    }

    @Test
    void paysThePreviousMonthOnTheFirstDayForEachAccountOnce() {
        var clock = new MutableClock(LocalDate.of(2028, 3, 1));
        var payouts = new RecordingPayMonthlyInterest();
        var scheduler = new InterestScheduler(
                clock,
                new RecordingAccrueInterest(),
                payouts,
                List.of(
                        new AccountCurrency(ACCOUNT_ID, GBP),
                        new AccountCurrency(ACCOUNT_ID, USD),
                        new AccountCurrency(SECOND_ACCOUNT_ID, GBP)));

        scheduler.runMonthly();

        assertThat(payouts.commands)
                .containsExactlyInAnyOrder(
                        new PayMonthlyInterestCommand(ACCOUNT_ID, YearMonth.of(2028, 2)),
                        new PayMonthlyInterestCommand(SECOND_ACCOUNT_ID, YearMonth.of(2028, 2)));
    }

    @Test
    void doesNotPayOnAnyOtherDay() {
        var clock = new MutableClock(LocalDate.of(2028, 2, 29));
        var payouts = new RecordingPayMonthlyInterest();
        var scheduler = new InterestScheduler(
                clock, new RecordingAccrueInterest(), payouts, List.of(new AccountCurrency(ACCOUNT_ID, GBP)));

        scheduler.runMonthly();

        assertThat(payouts.commands).isEmpty();
    }

    @Test
    void skipsADailyRunWhenTheClockRollsOverWhileThePreviousOneIsStillRunning() throws InterruptedException {
        var clock = new MutableClock(TODAY);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        AccrueInterest blockingAccrual = command -> {
            calls.incrementAndGet();
            clock.setToday(TODAY.plusDays(1));
            entered.countDown();
            await(release);
        };
        var scheduler = new InterestScheduler(
                clock,
                blockingAccrual,
                new RecordingPayMonthlyInterest(),
                List.of(new AccountCurrency(ACCOUNT_ID, GBP)));

        var first = runInBackground(scheduler::runDaily);
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

        var second = runInBackground(scheduler::runDaily);
        second.join(1_000);

        assertThat(second.isAlive()).isFalse();
        release.countDown();
        first.join();

        assertThat(calls).hasValue(1);
    }

    @Test
    void skipsAMonthlyRunWhileThePreviousOneIsStillRunning() throws InterruptedException {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        PayMonthlyInterest blockingPayout = command -> {
            calls.incrementAndGet();
            entered.countDown();
            await(release);
        };
        var scheduler = new InterestScheduler(
                new MutableClock(LocalDate.of(2028, 3, 1)),
                new RecordingAccrueInterest(),
                blockingPayout,
                List.of(new AccountCurrency(ACCOUNT_ID, GBP)));

        var first = runInBackground(scheduler::runMonthly);
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

        var second = runInBackground(scheduler::runMonthly);
        second.join(1_000);

        assertThat(second.isAlive()).isFalse();
        release.countDown();
        first.join();

        assertThat(calls).hasValue(1);
    }

    private static Thread runInBackground(Runnable runnable) {
        var thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static AccrueInterestCommand accrual(LocalDate date) {
        return new AccrueInterestCommand(ACCOUNT_ID, GBP, date);
    }

    private static final class MutableClock extends Clock {
        private final ZoneId zone = ZoneOffset.UTC;
        private Instant instant;

        private MutableClock(LocalDate today) {
            setToday(today);
        }

        private void setToday(LocalDate today) {
            this.instant = today.atStartOfDay(zone).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static final class RecordingAccrueInterest implements AccrueInterest {
        private final List<AccrueInterestCommand> commands = new ArrayList<>();

        @Override
        public void accrue(AccrueInterestCommand command) {
            commands.add(command);
        }
    }

    private static final class RecordingPayMonthlyInterest implements PayMonthlyInterest {
        private final List<PayMonthlyInterestCommand> commands = new ArrayList<>();

        @Override
        public void pay(PayMonthlyInterestCommand command) {
            commands.add(command);
        }
    }
}
