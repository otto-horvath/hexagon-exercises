package hexagon.interest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }

    public Money add(Money another) {
        Objects.requireNonNull(another, "another");

        if (!this.currency.equals(another.currency())) {
            throw new IllegalArgumentException("different currencies can't add");
        }

        return new Money(this.amount().add(another.amount()), this.currency());
    }

    public Money roundToMinorUnit() {
        if (currency.getDefaultFractionDigits() < 0) {
            throw new IllegalArgumentException("Currency does not define a minor unit: " + currency.getCurrencyCode());
        }

        return new Money(amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP), this.currency);
    }
}
