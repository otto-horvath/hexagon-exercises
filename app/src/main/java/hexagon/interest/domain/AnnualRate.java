package hexagon.interest.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record AnnualRate(BigDecimal value) {
    public AnnualRate {
        Objects.requireNonNull(value);
    }
}
