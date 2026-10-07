package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UnitDisplayTest
{
	@Test
	void dateOnlyPricesAreShownWithoutATime()
	{
		assertEquals("6 Oct 2026", UnitDisplay.asOf(Instant.parse("2026-10-06T00:00:00Z")));
		assertTrue(UnitDisplay.asOf(Instant.parse("2026-10-06T14:13:40Z")).matches("\\d+ Oct 2026 \\d\\d:\\d\\d"));
	}
}
