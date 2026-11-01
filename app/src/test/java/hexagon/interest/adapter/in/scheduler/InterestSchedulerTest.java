package hexagon.interest.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import hexagon.interest.application.port.in.AccrueInterest;
import hexagon.interest.application.port.in.AccrueInterestCommand;
import hexagon.interest.application.port.in.PayMonthlyInterest;
import hexagon.interest.application.port.in.PayMonthlyInterestCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterestSchedulerTest {
    private static final String ACCOUNT_ID = "account-123";
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
