package hexagon.interest.application.service;

import hexagon.interest.application.port.in.AccrueInterest;
import hexagon.interest.application.port.in.AccrueInterestCommand;
import hexagon.interest.application.port.out.AccrualRepository;
import hexagon.interest.application.port.out.BalanceProvider;
import hexagon.interest.application.port.out.InterestRateProvider;
import hexagon.interest.domain.InterestCalculator;
import java.util.Objects;

public final class AccrueInterestService implements AccrueInterest {
    private final BalanceProvider balanceProvider;
    private final InterestRateProvider interestRateProvider;
    private final AccrualRepository accrualRepository;

    public AccrueInterestService(
            BalanceProvider balanceProvider,
            InterestRateProvider interestRateProvider,
            AccrualRepository accrualRepository) {
        Objects.requireNonNull(balanceProvider, "balanceProvider");
        Objects.requireNonNull(interestRateProvider, "interestRateProvider");
        Objects.requireNonNull(accrualRepository, "accrualRepository");

        this.balanceProvider = balanceProvider;
        this.interestRateProvider = interestRateProvider;
        this.accrualRepository = accrualRepository;
    }

    @Override
    public void accrue(AccrueInterestCommand command) {
        Objects.requireNonNull(command, "command");

        var balanceAmount = balanceProvider.balanceFor(command.accountId(), command.currency(), command.date());
        var rate = interestRateProvider.rateFor(command.currency(), command.date());

        var dailyInterest = InterestCalculator.dailyInterest(balanceAmount, rate);

        accrualRepository.save(command.accountId(), command.currency(), command.date(), dailyInterest);
    }
}
