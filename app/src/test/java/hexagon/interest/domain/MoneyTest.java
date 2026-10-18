package hexagon.interest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @Test
    void storesAmountAndCurrency() {
        var amount = new BigDecimal("10000.00");
        var currency = Currency.getInstance("GBP");

        var money = new Money(amount, currency);

        assertThat(money.amount()).isEqualTo(amount);
        assertThat(money.currency()).isEqualTo(currency);
    }

    @Test
    void rejectsNullAmount() {
        var currency = Currency.getInstance("GBP");

        assertThatNullPointerException().isThrownBy(() -> new Money(null, currency));
    }

    @Test
    void rejectsNullCurrency() {
        var amount = new BigDecimal("10000.00");

        assertThatNullPointerException().isThrownBy(() -> new Money(amount, null));
    }

    @Test
    void addsAmountsWithTheSameCurrency() {
        var left = new Money(new BigDecimal("10.25"), Currency.getInstance("GBP"));
        var right = new Money(new BigDecimal("2.50"), Currency.getInstance("GBP"));

        var sum = left.add(right);

        assertThat(sum).isEqualTo(new Money(new BigDecimal("12.75"), Currency.getInstance("GBP")));
    }

    @Test
    void rejectsAddingDifferentCurrencies() {
        var pounds = new Money(new BigDecimal("10.00"), Currency.getInstance("GBP"));
        var euros = new Money(new BigDecimal("2.00"), Currency.getInstance("EUR"));

        assertThatIllegalArgumentException().isThrownBy(() -> pounds.add(euros));
    }

    @Test
    void rejectsNullMoneyToAdd() {
        var pounds = new Money(new BigDecimal("10.00"), Currency.getInstance("GBP"));

        assertThatNullPointerException().isThrownBy(() -> pounds.add(null));
    }

    @Test
    void addingDoesNotRoundToMinorUnit() {
        var left = new Money(new BigDecimal("1.234567"), Currency.getInstance("GBP"));
        var right = new Money(new BigDecimal("2.000000"), Currency.getInstance("GBP"));

        var sum = left.add(right);

        assertThat(sum.amount()).isEqualTo(new BigDecimal("3.234567"));
    }

    @ParameterizedTest
    @CsvSource({"GBP, 1.235, 1.24", "GBP, 1.234, 1.23", "JPY, 123.5, 124"})
    void roundsToCurrencyMinorUnitUsingHalfUp(String currencyCode, String amount, String expectedAmount) {
        var money = new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));

        var rounded = money.roundToMinorUnit();

        assertThat(rounded.amount()).isEqualTo(new BigDecimal(expectedAmount));
        assertThat(rounded.currency()).isEqualTo(money.currency());
    }

    @Test
    void rejectsRoundingCurrencyWithoutMinorUnit() {
        var money = new Money(new BigDecimal("1.25"), Currency.getInstance("XXX"));

        assertThatIllegalArgumentException().isThrownBy(money::roundToMinorUnit);
    }
}
