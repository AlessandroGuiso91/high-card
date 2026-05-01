package it.sara.demo.service.user.criteria;

import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests on {@link OrderType} display strings. The {@code BY_LASTNAME_DESC}
 * value originally carried the wrong display name ({@code "by lastName"}); the fix
 * is locked in here so a future copy-paste cannot reintroduce the bug.
 */
class OrderTypeTest {

    @Test
    void displayNamesAreDistinctAcrossDirections() {
        assertThat(OrderType.BY_FIRSTNAME.getDisplayName())
                .isNotEqualTo(OrderType.BY_FIRSTNAME_DESC.getDisplayName());
        assertThat(OrderType.BY_LASTNAME.getDisplayName())
                .isNotEqualTo(OrderType.BY_LASTNAME_DESC.getDisplayName());
    }

    @Test
    void byLastNameDescDisplayNameMentionsDesc() {
        assertThat(OrderType.BY_LASTNAME_DESC.getDisplayName()).contains("desc");
    }
}
