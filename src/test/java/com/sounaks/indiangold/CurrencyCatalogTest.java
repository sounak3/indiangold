package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Currency;

import org.junit.jupiter.api.Test;

class CurrencyCatalogTest
{
	@Test
	void codesJavaRejectsMapToTheCurrencyActuallyUsedThere()
	{
		assertEquals("GBP", CurrencyCatalog.resolve("GGP").getCurrencyCode());
		assertEquals("GBP", CurrencyCatalog.resolve("JEP").getCurrencyCode());
		assertEquals("GBP", CurrencyCatalog.resolve("IMP").getCurrencyCode());
		assertEquals("AUD", CurrencyCatalog.resolve("TVD").getCurrencyCode());
	}

	@Test
	void legacyCodesMapToTheirSuccessors()
	{
		assertEquals("TRY", CurrencyCatalog.resolve("TRL").getCurrencyCode());
		assertEquals("BYN", CurrencyCatalog.resolve("BYR").getCurrencyCode());
		assertEquals("EUR", CurrencyCatalog.resolve("HRK").getCurrencyCode());
		assertEquals("VES", CurrencyCatalog.resolve("VEF").getCurrencyCode());
	}

	@Test
	void unknownOrMissingCodesFallBackToUsDollar()
	{
		assertEquals("USD", CurrencyCatalog.resolve("XYZ").getCurrencyCode());
		assertEquals("USD", CurrencyCatalog.resolve("").getCurrencyCode());
		assertEquals("USD", CurrencyCatalog.resolve(null).getCurrencyCode());
	}

	@Test
	void currentCodesAreKept()
	{
		assertEquals("INR", CurrencyCatalog.resolve("INR").getCurrencyCode());
		assertEquals("inr", CurrencyCatalog.migrate("inr").toLowerCase());
	}

	@Test
	void everyCodeTheOldSettingsDialogOfferedStillResolves()
	{
		for (String code : new CurrencyCode().getCodeList()) {
			Currency resolved = CurrencyCatalog.resolve(code);
			assertNotNull(resolved, code);
			assertFalse(resolved.getCurrencyCode().equals("USD") && !code.equals("USD") && !code.equals("SVC"),
					code + " fell back to USD instead of a real successor");
		}
	}
}
