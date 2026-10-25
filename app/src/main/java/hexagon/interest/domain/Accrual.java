package hexagon.interest.domain;

import java.time.LocalDate;
import java.util.Objects;

public record Accrual(String accountId, LocalDate date, Money amount) {
    public Accrual {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(amount, "amount");
    }
}
