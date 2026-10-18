package hexagon.interest.domain;

import java.util.Currency;
import java.util.Map;
import java.util.Objects;

public record DayCountConvention(String name, int daysPerYear) {
    private static final Map<Currency, DayCountConvention> BY_CURRENCY = Map.of(
            Currency.getInstance("GBP"), new DayCountConvention("ACT/365 Fixed", 365),
            Currency.getInstance("EUR"), new DayCountConvention("ACT/360", 360),
            Currency.getInstance("USD"), new DayCountConvention("ACT/360", 360));

    public DayCountConvention {
        Objects.requireNonNull(name);

        if (daysPerYear < 1) {
            throw new IllegalArgumentException("daysPerYear must be positive");
        }
    }

    public static DayCountConvention forCurrency(Currency currency) {
        Objects.requireNonNull(currency, "currency");

        var convention = BY_CURRENCY.get(currency);
        if (convention == null) {
            throw new IllegalArgumentException("Unsupported currency: " + currency.getCurrencyCode());
        }

        return convention;
    }
}
