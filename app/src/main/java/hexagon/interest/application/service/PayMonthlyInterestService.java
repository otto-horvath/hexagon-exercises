package hexagon.interest.application.service;

import hexagon.interest.application.port.in.PayMonthlyInterest;
import hexagon.interest.application.port.in.PayMonthlyInterestCommand;
import hexagon.interest.application.port.out.AccrualRepository;
import hexagon.interest.application.port.out.InterestEventPublisher;
import hexagon.interest.application.port.out.PayoutRepository;
import hexagon.interest.domain.Accrual;
import hexagon.interest.domain.InterestPaid;
import hexagon.interest.domain.InterestPayout;
import hexagon.interest.domain.Money;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PayMonthlyInterestService implements PayMonthlyInterest {
    private final AccrualRepository accrualRepository;
    private final PayoutRepository payoutRepository;
    private final InterestEventPublisher eventPublisher;

    public PayMonthlyInterestService(
            AccrualRepository accrualRepository,
            PayoutRepository payoutRepository,
            InterestEventPublisher eventPublisher) {
        this.accrualRepository = accrualRepository;
        this.payoutRepository = payoutRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void pay(PayMonthlyInterestCommand command) {
        Objects.requireNonNull(command, "command");

        List<Accrual> accruals = accrualRepository.findForMonth(command.accountId(), command.month());

        var payouts = accruals.stream()
                .collect(Collectors.toMap(accrual -> accrual.amount().currency(), Accrual::amount, Money::add));

        payouts.values().stream()
                .map(money -> new InterestPayout(command.accountId(), command.month(), money.roundToMinorUnit()))
                .filter(interest -> payoutRepository.save(interest))
                .map(payout -> new InterestPaid(command.accountId(), command.month(), payout.amount()))
                .forEach(interest -> eventPublisher.publish(interest));
    }
}
