package hexagon.interest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InterestCalculatorTest {

    @Test
    void calculatesDailyInterestFromBalanceRateAndDayCountBasis() {
        var currency = Currency.getInstance("GBP");
        var balance = new Money(new BigDecimal("36500.00"), currency);
        var annualRate = new AnnualRate(new BigDecimal("0.01"));
        var interest = InterestCalculator.dailyInterest(balance, annualRate);

        assertThat(interest.currency()).isEqualTo(currency);
        assertThat(interest.amount()).isEqualByComparingTo("1.00");
    }

    @Test
    void calculatesRepeatingDailyInterestToTenDecimalPlaces() {
        var currency = Currency.getInstance("GBP");
        var balance = new Money(new BigDecimal("10000.00"), currency);
        var annualRate = new AnnualRate(new BigDecimal("0.04"));
        var interest = InterestCalculator.dailyInterest(balance, annualRate);

        assertThat(interest.amount()).isEqualTo(new BigDecimal("1.0958904110"));
    }

    @ParameterizedTest
    @CsvSource({"GBP, 0.9863013699", "EUR, 1.0000000000", "USD, 1.0000000000"})
    void calculatesDailyInterestUsingTheCurrencyDayCountBasis(String currencyCode, String expectedAmount) {
        var currency = Currency.getInstance(currencyCode);
        var balance = new Money(new BigDecimal("36000.00"), currency);
        var rate = new AnnualRate(new BigDecimal("0.01"));
        var interest = InterestCalculator.dailyInterest(balance, rate);

        assertThat(interest.amount()).isEqualTo(new BigDecimal(expectedAmount));
        assertThat(interest.currency()).isEqualTo(currency);
    }

    @Test
    void rejectsNullBalance() {
        var rate = new AnnualRate(new BigDecimal("0.04"));
        assertThatNullPointerException().isThrownBy(() -> InterestCalculator.dailyInterest(null, rate));
    }

    @Test
    void rejectsNullAnnualRate() {
        var balance = new Money(new BigDecimal("10000.00"), Currency.getInstance("GBP"));
        assertThatNullPointerException().isThrownBy(() -> InterestCalculator.dailyInterest(balance, null));
    }
}
