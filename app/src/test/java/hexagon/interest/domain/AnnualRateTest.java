package hexagon.interest.domain;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class AnnualRateTest {

    @Test
    void rejectsNullValue() {
        assertThatNullPointerException().isThrownBy(() -> new AnnualRate(null));
    }
}
