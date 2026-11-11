package com.gregisoft.explorer.cli.garmin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GarminActivityIdTest {

    @Test
    void acceptsAndCanonicalizesNumericId() {
        assertEquals("123456789", GarminActivityId.parse("123456789").value());
        assertEquals("123", GarminActivityId.parse("000123").value());
    }

    @Test
    void acceptsCanonicalGarminConnectActivityUrl() {
        assertEquals("123456789", GarminActivityId.parse(
                "https://connect.garmin.com/modern/activity/123456789").value());
        assertEquals("123456789", GarminActivityId.parse(
                "https://connect.garmin.com/modern/activity/123456789/").value());
    }

    @Test
    void rejectsInvalidIdsAndUnrelatedOrAmbiguousUrls() {
        for (String invalid : new String[]{
                "0", "-1", "abc", "http://connect.garmin.com/modern/activity/123",
                "https://evil.example/modern/activity/123",
                "https://connect.garmin.com/modern/activity/123?foo=bar",
                "https://connect.garmin.com/modern/activity/123/extra"
        }) {
            assertThrows(IllegalArgumentException.class, () -> GarminActivityId.parse(invalid));
        }
    }
}
