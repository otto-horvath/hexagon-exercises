package hexagon.interest.application.service;

import hexagon.interest.application.port.in.PayMonthlyInterest;
import hexagon.interest.application.port.in.PayMonthlyInterestCommand;
import hexagon.interest.application.port.out.AccrualRepository;
import hexagon.interest.application.port.out.InterestEventPublisher;
import hexagon.interest.application.port.out.PayoutRepository;

public final class PayMonthlyInterestService implements PayMonthlyInterest {
    public PayMonthlyInterestService(
            AccrualRepository accrualRepository,
            PayoutRepository payoutRepository,
            InterestEventPublisher eventPublisher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pay(PayMonthlyInterestCommand command) {
        throw new UnsupportedOperationException();
    }
}
