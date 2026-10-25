package hexagon.interest.application.port.out;

import hexagon.interest.domain.InterestPayout;

public interface PayoutRepository {
    void save(InterestPayout payout);
}
