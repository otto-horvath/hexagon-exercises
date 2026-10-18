package hexagon.interest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DayCountConventionTest {

    @ParameterizedTest
    @CsvSource({"GBP, 'ACT/365 Fixed', 365", "EUR, ACT/360, 360", "USD, ACT/360, 360"})
    void selectsConventionForCurrency(String currencyCode, String name, int daysPerYear) {
        var convention = DayCountConvention.forCurrency(Currency.getInstance(currencyCode));

        assertThat(convention.name()).isEqualTo(name);
        assertThat(convention.daysPerYear()).isEqualTo(daysPerYear);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatNullPointerException().isThrownBy(() -> DayCountConvention.forCurrency(null));
    }

    @Test
    void rejectsUnsupportedCurrency() {
        var currency = Currency.getInstance("CAD");

        assertThatIllegalArgumentException().isThrownBy(() -> DayCountConvention.forCurrency(currency));
    }

    @Test
    void rejectsNullName() {
        assertThatNullPointerException().isThrownBy(() -> new DayCountConvention(null, 365));
    }

    @Test
    void rejectsNonPositiveDaysPerYear() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DayCountConvention("ACT/365", 0));
    }
}
