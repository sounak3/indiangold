package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VersionTest
{
	@Test
	void versionComesFromPomWithoutSnapshotSuffix()
	{
		String version = IndianGold.readVersion();

		assertTrue(version.matches("\\d+(\\.\\d+){0,2}"), "not a numeric version: " + version);
		assertEquals("Indian Gold v" + version, IndianGold.NAME_STRING_FULL);
		assertEquals("IGv" + version.split("\\.")[0], IndianGold.NAME_STRING_SHORT);
	}
}
