package hexagon.interest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class InterestCalculator {
    private InterestCalculator() {}

    public static Money dailyInterest(Money balance, AnnualRate annualRate) {

        Objects.requireNonNull(balance);
        Objects.requireNonNull(annualRate);

        var convention = DayCountConvention.forCurrency(balance.currency());

        var interest = balance.amount()
                .multiply(annualRate.value())
                .divide(BigDecimal.valueOf(convention.daysPerYear()), 10, RoundingMode.HALF_UP);

        return new Money(interest, balance.currency());
    }
}
