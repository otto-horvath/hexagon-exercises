package hexagon.interest.adapter.in.scheduler;

import hexagon.interest.application.port.in.AccrueInterest;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class InterestScheduler {
    private final Clock clock;
    private final AccrueInterest accrueInterest;
    private final List<AccountCurrency> accounts;

    public InterestScheduler(Clock clock, AccrueInterest accrueInterest, List<AccountCurrency> accounts) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.accrueInterest = Objects.requireNonNull(accrueInterest, "accrueInterest");
        this.accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
    }

    public void runDaily() {
        throw new UnsupportedOperationException("TODO: accrue interest for every configured account and currency");
    }
}
