package hexagon.interest.application.port.out;

import hexagon.interest.domain.InterestPayout;
import hexagon.interest.domain.PayoutResult;

public interface PayoutRepository {
    PayoutResult save(InterestPayout payout);
}
