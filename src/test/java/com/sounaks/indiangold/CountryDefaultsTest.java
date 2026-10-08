package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import com.sounaks.indiangold.rates.MassUnit;
import com.sounaks.indiangold.rates.Metal;
import com.sounaks.indiangold.rates.RateService;
import java.io.File;
import java.nio.file.Path;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryDefaultsTest
{
	@TempDir Path home;
	@TempDir Path jarDir;
	private String originalHome;
	private final CountryDefaults countries = CountryDefaults.load();

	@BeforeEach
	void isolateFromRealUser()
	{
		originalHome = System.getProperty("user.home");
		System.setProperty("user.home", home.toString());
		FileOperations.jarDir = jarDir.toFile();
	}

	@AfterEach
	void restore()
	{
		System.setProperty("user.home", originalHome);
		FileOperations.jarDir = null;
	}

	@Test
	void everyCountryHasARowWithUsableValues()
	{
		for (String code : Locale.getISOCountries()) {
			assertTrue(countries.hasRow(code), code);
			CountryDefaults.Defaults d = countries.forCountry(code);
			assertDoesNotThrow(() -> Currency.getInstance(d.currency()), code);
			assertEquals(4, d.units().size(), code);
			assertFalse(d.purities().isEmpty(), code);
			d.purities().forEach(p -> assertTrue(p.fineness() > 0.3 && p.fineness() <= 1, code + " " + p));
		}
		assertTrue(countries.countries().size() >= 249);
	}

	@Test
	void everyCountryOfTheOldCurrencyListIsOffered()
	{
		for (String oldCode : OldCurrencyCodes.list()) {
			String current = CurrencyCatalog.resolve(oldCode).getCurrencyCode();
			boolean offered = countries.countries().stream().anyMatch(c -> countries.forCountry(c.code()).currency().equals(current));
			assertTrue(offered, oldCode + " (" + current + ") has no country");
		}
	}

	@Test
	void indiaQuotesGoldPerTenGramsAndSilverPerKilogram()
	{
		CountryDefaults.Defaults india = countries.forCountry("IN");

		assertEquals("INR", india.currency());
		assertEquals(new CountryDefaults.GroupUnit(MassUnit.GRAM, 10), india.units().get(Metal.Group.GOLD));
		assertEquals(new CountryDefaults.GroupUnit(MassUnit.KILOGRAM, 1), india.units().get(Metal.Group.SILVER));
		assertEquals(List.of("24K", "22K", "18K"), india.purities().stream().map(CountryDefaults.Purity::label).toList());
		assertEquals(0.916, india.purities().get(1).fineness(), 1e-9);
		assertEquals(3.0, TaxSettings.totalPercent(india.taxes()), 1e-9);
	}

	@Test
	void countriesWithoutSpecificConventionsGetTheMetricDefaultAndTheirCurrency()
	{
		CountryDefaults.Defaults kenya = countries.forCountry("KE");
		assertEquals("KES", kenya.currency());
		assertEquals(MassUnit.GRAM, kenya.units().get(Metal.Group.GOLD).unit());
		assertTrue(kenya.taxes().isEmpty());

		assertEquals("USD", countries.forCountry("ZZ").currency(), "unknown code");
	}

	@Test
	void applyingACountryAddsMissingUnitsAndSetsTheDisplay()
	{
		FileOperations ops = new FileOperations(new File("units.dat"), "test");
		MarketSettings settings = new MarketSettings(ops);

		settings.applyCountry(countries.forCountry("TH"));

		assertEquals("THB", settings.currency());
		MarketSettings.DisplayUnit gold = settings.unit(Metal.Group.GOLD);
		assertEquals("baht (15.244 g)", gold.name());
		assertEquals(15.244, gold.grams(), 1e-9);
		assertTrue(ops.getCheckedUnitNames().contains("baht (15.244 g)"), "the new unit can be used in the calculator");
		assertEquals("96.5%", settings.purities().get(0).label());

		settings.applyCountry(countries.forCountry("IN"));
		assertEquals("10 g", settings.unit(Metal.Group.GOLD).label());
		assertEquals(10, settings.unit(Metal.Group.GOLD).totalGrams(), 1e-9);
		assertEquals("kg", settings.unit(Metal.Group.SILVER).label());
		assertEquals(3.0, TaxSettings.totalPercent(TaxSettings.parse(ops.getValue("$taxes", ""))), 1e-9);
	}

	@Test
	void unitsUsedForRatesAreRecognized()
	{
		FileOperations ops = new FileOperations(new File("units.dat"), "test");
		MarketSettings settings = new MarketSettings(ops);
		settings.applyCountry(countries.forCountry("IN"));

		assertEquals("gold", settings.groupUsing("gram (g)"));
		assertEquals("silver", settings.groupUsing("Kilogram (kg)"));
		assertNull(settings.groupUsing("ratti (rt)"));

		settings.setUnit(Metal.Group.GOLD, "tola (bhori / standard tola)", 1);
		assertEquals("platinum and palladium", settings.groupUsing("gram (g)"), "gold moved to tola; platinum still uses grams");
		assertEquals(11.6638, settings.unit(Metal.Group.GOLD).totalGrams(), 1e-3);
	}

	@Test
	void oldUnitSettingsCarryOverToEveryMetalGroup()
	{
		FileOperations ops = new FileOperations(new File("units.dat"), "test");
		ops.setValue("$punit", "tola (bhori / standard tola)");
		ops.setValue("$punitspercurrency", "1");
		ops.setValue("$bunit", "kilogram (kg)");
		ops.setValue("$bunitspercurrency", "100");

		MarketSettings settings = new MarketSettings(ops);

		assertEquals("tola (bhori / standard tola)", settings.unit(Metal.Group.SILVER).name());
		assertEquals(11.6638, settings.unit(Metal.Group.GOLD).grams(), 1e-3);
		assertEquals(100_000, settings.unit(Metal.Group.BASE).totalGrams(), 1e-6);
	}

	@Test
	void sourcesAndProviderSettingsAreKept()
	{
		FileOperations ops = new FileOperations(new File("units.dat"), "test");
		MarketSettings settings = new MarketSettings(ops);

		assertEquals(List.of(new RateService.SourceChoice("gold-api", true), new RateService.SourceChoice("westmetall", true),
				new RateService.SourceChoice("currency-api", true), new RateService.SourceChoice("metals-dev", false),
				new RateService.SourceChoice("manual", false)), settings.sources());

		settings.setSources(List.of(new RateService.SourceChoice("metals-dev", true), new RateService.SourceChoice("gold-api", false)));
		settings.providerSettings("metals-dev").put("apikey", "AbC-123");
		ops.saveToFile();

		MarketSettings reloaded = new MarketSettings(new FileOperations(new File("units.dat"), "test"));
		assertEquals(List.of(new RateService.SourceChoice("metals-dev", true), new RateService.SourceChoice("gold-api", false)), reloaded.sources());
		assertEquals("AbC-123", reloaded.providerSettings("metals-dev").apiKey().orElseThrow(), "the key keeps its case");
	}
}
