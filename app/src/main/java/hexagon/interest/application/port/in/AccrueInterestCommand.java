package hexagon.interest.application.port.in;

import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

public record AccrueInterestCommand(String accountId, Currency currency, LocalDate date) {
    public AccrueInterestCommand {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(date, "date");
    }
}
