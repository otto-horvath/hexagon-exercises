package hexagon.interest.application.port.out;

import hexagon.interest.domain.AnnualRate;
import java.time.LocalDate;
import java.util.Currency;

public interface InterestRateProvider {
    AnnualRate rateFor(Currency currency, LocalDate date);
}
