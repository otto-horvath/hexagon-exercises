package hexagon.interest.application.service;

import hexagon.interest.application.port.in.AccrueInterest;
import hexagon.interest.application.port.in.AccrueInterestCommand;
import hexagon.interest.application.port.out.AccrualRepository;
import hexagon.interest.application.port.out.BalanceProvider;
import hexagon.interest.application.port.out.InterestRateProvider;
import java.util.Objects;

public final class AccrueInterestService implements AccrueInterest {
    public AccrueInterestService(
            BalanceProvider balanceProvider,
            InterestRateProvider interestRateProvider,
            AccrualRepository accrualRepository) {
        Objects.requireNonNull(balanceProvider, "balanceProvider");
        Objects.requireNonNull(interestRateProvider, "interestRateProvider");
        Objects.requireNonNull(accrualRepository, "accrualRepository");
    }

    @Override
    public void accrue(AccrueInterestCommand command) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
