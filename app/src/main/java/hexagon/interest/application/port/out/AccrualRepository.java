package hexagon.interest.application.port.out;

import hexagon.interest.domain.Money;
import java.time.LocalDate;
import java.util.Currency;

public interface AccrualRepository {
    void save(String accountId, Currency currency, LocalDate date, Money accrual);
}
