package hexagon.interest.application.port.out;

import hexagon.interest.domain.Money;
import java.time.LocalDate;
import java.util.Currency;

public interface BalanceProvider {
    Money balanceFor(String accountId, Currency currency, LocalDate date);
}
