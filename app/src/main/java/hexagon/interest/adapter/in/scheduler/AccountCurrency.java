package hexagon.interest.adapter.in.scheduler;

import java.util.Currency;
import java.util.Objects;

public record AccountCurrency(String accountId, Currency currency) {
    public AccountCurrency {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(currency, "currency");
    }
}
