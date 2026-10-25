package hexagon.interest.application.port.in;

import java.time.YearMonth;
import java.util.Objects;

public record PayMonthlyInterestCommand(String accountId, YearMonth month) {
    public PayMonthlyInterestCommand {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(month, "month");
    }
}
