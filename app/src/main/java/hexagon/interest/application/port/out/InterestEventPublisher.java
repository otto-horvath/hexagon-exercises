package hexagon.interest.application.port.out;

import hexagon.interest.domain.InterestPaid;

public interface InterestEventPublisher {
    void publish(InterestPaid event);
}
