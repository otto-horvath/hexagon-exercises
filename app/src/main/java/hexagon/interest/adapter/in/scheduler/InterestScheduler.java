package hexagon.interest.adapter.in.scheduler;

import hexagon.interest.application.port.in.AccrueInterest;
import hexagon.interest.application.port.in.AccrueInterestCommand;
import hexagon.interest.application.port.in.PayMonthlyInterest;
import hexagon.interest.application.port.in.PayMonthlyInterestCommand;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class InterestScheduler {
    private final Clock clock;
    private final AccrueInterest accrueInterest;
    private final PayMonthlyInterest payMonthlyInterest;
    private final List<AccountCurrency> accounts;

    private LocalDate lastExecution;

    public InterestScheduler(
            Clock clock,
            AccrueInterest accrueInterest,
            PayMonthlyInterest payMonthlyInterest,
            List<AccountCurrency> accounts) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.accrueInterest = Objects.requireNonNull(accrueInterest, "accrueInterest");
        this.payMonthlyInterest = Objects.requireNonNull(payMonthlyInterest, "payMonthlyInterest");
        this.accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
    }

    public void runDaily() {
        var currentDate = LocalDate.now(clock);

        accrualForPeriod(currentDate)
                .forEach(accrualDate -> accounts.stream()
                        .map(account -> new AccrueInterestCommand(account.accountId(), account.currency(), accrualDate))
                        .forEach(accrueInterest::accrue));
    }

    public void runMonthly() {
        var currentDate = LocalDate.now(clock);

        if (currentDate.getDayOfMonth() > 1) {
            return;
        }

        var payoutYearMont = YearMonth.now(clock).minusMonths(1);
        accounts.stream()
                .map(AccountCurrency::accountId)
                .distinct()
                .map(accountId -> new PayMonthlyInterestCommand(accountId, payoutYearMont))
                .forEach(payMonthlyInterest::pay);
    }

    private Stream<LocalDate> accrualForPeriod(LocalDate currentDate) {
        if (lastExecution == null) {
            lastExecution = LocalDate.now(clock);
            return lastExecution.datesUntil(currentDate.plusDays(1));
        }

        if (lastExecution.equals(currentDate)) {
            return Stream.empty();
        }

        return lastExecution.plusDays(1).datesUntil(currentDate.plusDays(1));
    }
}
