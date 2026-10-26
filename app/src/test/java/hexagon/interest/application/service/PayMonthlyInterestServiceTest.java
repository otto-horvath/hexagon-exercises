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
import hexagon.interest.domain.PayoutResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Test
    void repeatedPayoutRequestSavesAndPublishesOnlyOnce() {
        var accruals = new RecordingAccrualRepository(
                List.of(new Accrual(ACCOUNT_ID, LocalDate.of(2026, 10, 1), new Money(new BigDecimal("1.25"), GBP))));
        var payouts = new RecordingPayoutRepository();
        var events = new RecordingInterestEventPublisher();
        var service = new PayMonthlyInterestService(accruals, payouts, events);
        var command = new PayMonthlyInterestCommand(ACCOUNT_ID, MONTH);

        service.pay(command);
        service.pay(command);

        assertThat(payouts.saved)
                .containsExactly(new InterestPayout(ACCOUNT_ID, MONTH, new Money(new BigDecimal("1.25"), GBP)));
        assertThat(events.published)
                .containsExactly(new InterestPaid(ACCOUNT_ID, MONTH, new Money(new BigDecimal("1.25"), GBP)));
    }

    @Test
    void includesAccrualsFromBothSidesOfAMidMonthRateChange() {
        var accruals = new RecordingAccrualRepository(List.of(
                new Accrual(ACCOUNT_ID, LocalDate.of(2026, 10, 14), new Money(new BigDecimal("0.05"), GBP)),
                new Accrual(ACCOUNT_ID, LocalDate.of(2026, 10, 15), new Money(new BigDecimal("0.10"), GBP))));
        var payouts = new RecordingPayoutRepository();
        var events = new RecordingInterestEventPublisher();
        var service = new PayMonthlyInterestService(accruals, payouts, events);

        service.pay(new PayMonthlyInterestCommand(ACCOUNT_ID, MONTH));

        assertThat(payouts.saved)
                .containsExactly(new InterestPayout(ACCOUNT_ID, MONTH, new Money(new BigDecimal("0.15"), GBP)));
        assertThat(events.published)
                .containsExactly(new InterestPaid(ACCOUNT_ID, MONTH, new Money(new BigDecimal("0.15"), GBP)));
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
        private final Set<PayoutKey> keys = new HashSet<>();

        @Override
        public PayoutResult save(InterestPayout payout) {
            var key = new PayoutKey(payout.accountId(), payout.amount().currency(), payout.month());
            if (!keys.add(key)) {
                return new PayoutResult.AlreadyPaid();
            }
            saved.add(payout);
            return new PayoutResult.NewlyPaid(payout);
        }

        private record PayoutKey(String accountId, Currency currency, YearMonth month) {}
    }

    private static final class RecordingInterestEventPublisher implements InterestEventPublisher {
        private final List<InterestPaid> published = new ArrayList<>();

        @Override
        public void publish(InterestPaid event) {
            published.add(event);
        }
    }
}
