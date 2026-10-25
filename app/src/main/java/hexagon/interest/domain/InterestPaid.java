package hexagon.interest.domain;

import java.time.YearMonth;
import java.util.Objects;

public record InterestPaid(String accountId, YearMonth month, Money amount) {
    public InterestPaid {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(amount, "amount");
    }
}
