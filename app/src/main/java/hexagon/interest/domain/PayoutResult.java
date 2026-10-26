package hexagon.interest.domain;

import java.util.Objects;

public sealed interface PayoutResult permits PayoutResult.NewlyPaid, PayoutResult.AlreadyPaid {
    record NewlyPaid(InterestPayout payout) implements PayoutResult {
        public NewlyPaid {
            Objects.requireNonNull(payout, "payout");
        }
    }

    record AlreadyPaid() implements PayoutResult {}
}
