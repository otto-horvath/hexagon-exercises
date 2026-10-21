package hexagon.interest.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import hexagon.interest.application.port.in.AccrueInterestCommand;
import hexagon.interest.application.port.out.AccrualRepository;
import hexagon.interest.application.port.out.BalanceProvider;
import hexagon.interest.application.port.out.InterestRateProvider;
import hexagon.interest.domain.AnnualRate;
import hexagon.interest.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class AccrueInterestServiceTest {
    private static final String ACCOUNT_ID = "account-123";
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);

    @Test
    void fetchesInputsCalculatesDailyInterestAndStoresTheAccrual() {
        var balance = new Money(new BigDecimal("36500.00"), GBP);
        var rate = new AnnualRate(new BigDecimal("0.01"));
        var balances = new RecordingBalanceProvider(balance);
        var rates = new RecordingInterestRateProvider(rate);
        var accruals = new RecordingAccrualRepository();
        var service = new AccrueInterestService(balances, rates, accruals);

        service.accrue(new AccrueInterestCommand(ACCOUNT_ID, GBP, DATE));

        assertThat(balances.accountId).isEqualTo(ACCOUNT_ID);
        assertThat(balances.currency).isEqualTo(GBP);
        assertThat(balances.date).isEqualTo(DATE);
        assertThat(rates.currency).isEqualTo(GBP);
        assertThat(rates.date).isEqualTo(DATE);
        assertThat(accruals.accountId).isEqualTo(ACCOUNT_ID);
        assertThat(accruals.currency).isEqualTo(GBP);
        assertThat(accruals.date).isEqualTo(DATE);
        assertThat(accruals.accrual).isEqualTo(new Money(new BigDecimal("1.0000000000"), GBP));
    }

    private static final class RecordingBalanceProvider implements BalanceProvider {
        private final Money balance;
        private String accountId;
        private Currency currency;
        private LocalDate date;

        private RecordingBalanceProvider(Money balance) {
            this.balance = balance;
        }

        @Override
        public Money balanceFor(String accountId, Currency currency, LocalDate date) {
            this.accountId = accountId;
            this.currency = currency;
            this.date = date;
            return balance;
        }
    }

    private static final class RecordingInterestRateProvider implements InterestRateProvider {
        private final AnnualRate rate;
        private Currency currency;
        private LocalDate date;

        private RecordingInterestRateProvider(AnnualRate rate) {
            this.rate = rate;
        }

        @Override
        public AnnualRate rateFor(Currency currency, LocalDate date) {
            this.currency = currency;
            this.date = date;
            return rate;
        }
    }

    private static final class RecordingAccrualRepository implements AccrualRepository {
        private String accountId;
        private Currency currency;
        private LocalDate date;
        private Money accrual;

        @Override
        public void save(String accountId, Currency currency, LocalDate date, Money accrual) {
            this.accountId = accountId;
            this.currency = currency;
            this.date = date;
            this.accrual = accrual;
        }
    }
}
