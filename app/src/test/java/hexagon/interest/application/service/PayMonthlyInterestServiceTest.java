package hexagon.interest.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import hexagon.interest.application.port.in.PayMonthlyInterestCommand;
import hexagon.interest.application.port.out.AccrualRepository;
import hexagon.interest.application.port.out.InterestEventPublisher;
import hexagon.interest.application.port.out.PayoutRepository;
import hexagon.interest.domain.Accrual;
import hexagon.interest.domain.InterestPaid;
import hexagon.interest.domain.InterestPayout;
import hexagon.interest.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class PayMonthlyInterestServiceTest {
    private static final String ACCOUNT_ID = "account-123";
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency USD = Currency.getInstance("USD");
    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    @Test
    void aggregatesByCurrencyRoundsTheMonthlyTotalAndPublishesPaidEvents() {
        var accruals = new RecordingAccrualRepository(List.of(
                new Accrual(ACCOUNT_ID, LocalDate.of(2026, 10, 1), new Money(new BigDecimal("0.005"), GBP)),
                new Accrual(ACCOUNT_ID, LocalDate.of(2026, 10, 2), new Money(new BigDecimal("0.005"), GBP)),
                new Accrual(ACCOUNT_ID, LocalDate.of(2026, 10, 1), new Money(new BigDecimal("0.014"), USD))));
        var payouts = new RecordingPayoutRepository();
        var events = new RecordingInterestEventPublisher();
        var service = new PayMonthlyInterestService(accruals, payouts, events);

        service.pay(new PayMonthlyInterestCommand(ACCOUNT_ID, MONTH));

        assertThat(accruals.accountId).isEqualTo(ACCOUNT_ID);
        assertThat(accruals.month).isEqualTo(MONTH);
        assertThat(payouts.saved)
                .containsExactlyInAnyOrder(
                        new InterestPayout(ACCOUNT_ID, MONTH, new Money(new BigDecimal("0.01"), GBP)),
                        new InterestPayout(ACCOUNT_ID, MONTH, new Money(new BigDecimal("0.01"), USD)));
        assertThat(events.published)
                .containsExactlyInAnyOrder(
                        new InterestPaid(ACCOUNT_ID, MONTH, new Money(new BigDecimal("0.01"), GBP)),
                        new InterestPaid(ACCOUNT_ID, MONTH, new Money(new BigDecimal("0.01"), USD)));
    }

    private static final class RecordingAccrualRepository implements AccrualRepository {
        private final List<Accrual> result;
        private String accountId;
        private YearMonth month;

        private RecordingAccrualRepository(List<Accrual> result) {
            this.result = result;
        }

        @Override
        public void save(String accountId, Currency currency, LocalDate date, Money accrual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Accrual> findForMonth(String accountId, YearMonth month) {
            this.accountId = accountId;
            this.month = month;
            return result;
        }
    }

    private static final class RecordingPayoutRepository implements PayoutRepository {
        private final List<InterestPayout> saved = new ArrayList<>();

        @Override
        public void save(InterestPayout payout) {
            saved.add(payout);
        }
    }

    private static final class RecordingInterestEventPublisher implements InterestEventPublisher {
        private final List<InterestPaid> published = new ArrayList<>();

        @Override
        public void publish(InterestPaid event) {
            published.add(event);
        }
    }
}
