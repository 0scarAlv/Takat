package com.takat.finanzas.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class TrendWindowTest {
    private val now = YearMonth.of(2026, 11)

    @Test fun currentMonthEndsAtNow() = assertEquals(now, trendWindowEnd(now, now))

    @Test fun recentPastMonthKeepsNewerMonthsVisible() =
        assertEquals(now, trendWindowEnd(YearMonth.of(2026, 8), now))

    @Test fun oldestMonthStillInsideWindow() =
        assertEquals(now, trendWindowEnd(YearMonth.of(2026, 6), now))

    @Test fun olderMonthSlidesWindowKeepingSelectionFirst() =
        assertEquals(YearMonth.of(2026, 6), trendWindowEnd(YearMonth.of(2026, 1), now))

    @Test fun futureMonthEndsAtSelection() =
        assertEquals(YearMonth.of(2027, 1), trendWindowEnd(YearMonth.of(2027, 1), now))
}
