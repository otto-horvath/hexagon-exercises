package hexagon.interest.application.port.out;

import hexagon.interest.domain.InterestPayout;

public interface PayoutRepository {
    boolean save(InterestPayout payout);
}
