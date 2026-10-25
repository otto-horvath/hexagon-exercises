package hexagon.interest.application.port.out;

import hexagon.interest.domain.Accrual;
import hexagon.interest.domain.Money;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

public interface AccrualRepository {
    void save(String accountId, Currency currency, LocalDate date, Money accrual);

    List<Accrual> findForMonth(String accountId, YearMonth month);
}
