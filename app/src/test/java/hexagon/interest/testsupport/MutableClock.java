package hexagon.interest.testsupport;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class MutableClock extends Clock {
    private final ZoneId zone = ZoneOffset.UTC;
    private Instant instant;

    public MutableClock(LocalDate today) {
        setToday(today);
    }

    public void setToday(LocalDate today) {
        this.instant = today.atStartOfDay(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
