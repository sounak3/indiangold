package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import com.sounaks.indiangold.rates.FxRates;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;

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
		for (String code : OldCurrencyCodes.list()) {
			Currency resolved = CurrencyCatalog.resolve(code);
			assertNotNull(resolved, code);
			assertFalse(resolved.getCurrencyCode().equals("USD") && !code.equals("USD") && !code.equals("SVC"),
					code + " fell back to USD instead of a real successor");
		}
	}

	@Test
	void pickerOffersOnlyCurrentCurrenciesTheRatesCover()
	{
		FxRates fx = new FxRates(Map.of("INR", 96.4, "AED", 3.67, "GGP", 0.76, "HRK", 6.6, "XAU", 0.00024, "BTC", 0.00001, "EUR", 0.88), Instant.EPOCH, "t");

		List<String> codes = CurrencyCatalog.choices(fx, "INR").stream().map(c -> c.currency().getCurrencyCode()).toList();

		assertTrue(codes.containsAll(List.of("INR", "AED", "EUR", "USD")), codes.toString());
		assertFalse(codes.contains("GGP") || codes.contains("HRK") || codes.contains("XAU"), codes.toString());
		assertTrue(CurrencyCatalog.choices(fx, "INR").get(0).toString().matches(".+ \\([A-Z]{3}\\).*"));
	}

	@Test
	void withoutRatesThePickerOffersTodaysCurrenciesOnly()
	{
		List<String> codes = CurrencyCatalog.choices(FxRates.usdOnly(), "USD").stream().map(c -> c.currency().getCurrencyCode()).toList();

		assertTrue(codes.size() > 140, "currencies of all countries: " + codes.size());
		assertTrue(codes.contains("KES") && codes.contains("AED"));
		assertFalse(codes.contains("DEM") || codes.contains("ATS"), "long-gone currencies are not offered");
	}
}
