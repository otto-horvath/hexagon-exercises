package hexagon.interest.domain;

import java.time.YearMonth;
import java.util.Objects;

public record InterestPayout(String accountId, YearMonth month, Money amount) {
    public InterestPayout {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(amount, "amount");
    }
}
