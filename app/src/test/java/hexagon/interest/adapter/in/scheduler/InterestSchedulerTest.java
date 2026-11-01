package hexagon.interest.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import hexagon.interest.application.port.in.AccrueInterest;
import hexagon.interest.application.port.in.AccrueInterestCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
                List.of(new AccountCurrency(ACCOUNT_ID, GBP), new AccountCurrency(ACCOUNT_ID, USD)));

        scheduler.runDaily();

        assertThat(accruals.commands)
                .containsExactly(
                        new AccrueInterestCommand(ACCOUNT_ID, GBP, TODAY),
                        new AccrueInterestCommand(ACCOUNT_ID, USD, TODAY));
    }

    private static final class RecordingAccrueInterest implements AccrueInterest {
        private final List<AccrueInterestCommand> commands = new ArrayList<>();

        @Override
        public void accrue(AccrueInterestCommand command) {
            commands.add(command);
        }
    }
}
