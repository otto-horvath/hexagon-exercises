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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public final class InterestScheduler {
    private final Clock clock;
    private final AccrueInterest accrueInterest;
    private final PayMonthlyInterest payMonthlyInterest;
    private final List<AccountCurrency> accounts;
    private final AtomicBoolean dailyRunning = new AtomicBoolean();
    private final AtomicBoolean monthlyRunning = new AtomicBoolean();

    private LocalDate lastAccruedDate;

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

    private void accrueDueDays() {
        var currentDate = LocalDate.now(clock);

        accrualForPeriod(currentDate)
                .forEach(accrualDate -> accounts.stream()
                        .map(account -> new AccrueInterestCommand(account.accountId(), account.currency(), accrualDate))
                        .forEach(accrueInterest::accrue));

        lastAccruedDate = currentDate;
    }

    private void payCompletedMonth() {
        var currentDate = LocalDate.now(clock);

        if (currentDate.getDayOfMonth() != 1) {
            return;
        }

        var payoutYearMonth = YearMonth.from(currentDate).minusMonths(1);
        accounts.stream()
                .map(AccountCurrency::accountId)
                .distinct()
                .map(accountId -> new PayMonthlyInterestCommand(accountId, payoutYearMonth))
                .forEach(payMonthlyInterest::pay);
    }

    private Stream<LocalDate> accrualForPeriod(LocalDate currentDate) {
        var firstDate = lastAccruedDate == null ? currentDate : lastAccruedDate.plusDays(1);

        return firstDate.datesUntil(currentDate.plusDays(1));
    }
}
