package com.sounaks.indiangold.rates;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/** Definition validation, robots.txt rules, number and date parsing, and price conversion. */
class DefinitionsTest
{
	private static Properties props(String... keyValues)
	{
		Properties p = new Properties();
		for (int i = 0; i < keyValues.length; i += 2) p.setProperty(keyValues[i], keyValues[i + 1]);
		return p;
	}

	@Test
	void builtInDefinitionsAreValid() throws Exception
	{
		for (String id : new String[] { "gold-api", "currency-api", "metals-dev", "westmetall" }) {
			Properties p = new Properties();
			try (InputStream in = getClass().getResourceAsStream("/providers/" + id + ".properties")) {
				p.load(in);
			}
			ProviderDefinition definition = ProviderDefinition.of(p);
			assertTrue(definition.isValid(), id + ": " + definition.problems());
			assertEquals(id, definition.id());
		}
	}

	@Test
	void problemsAreExplained()
	{
		ProviderDefinition bad = ProviderDefinition.of(props("id", "Bad Id", "type", "json-api", "apikey", "required",
				"url", "ftp://x", "metal.rhodium.path", "/r", "metal.gold.path", "/g", "metal.gold.unit", "stone"));

		String all = String.join("\n", bad.problems());
		assertTrue(all.contains("id must be"), all);
		assertTrue(all.contains("name is missing"), all);
		assertTrue(all.contains("Unknown metal \"rhodium\""), all);
		assertTrue(all.contains("Unknown unit \"stone\""), all);
		assertTrue(all.contains("must start with https://"), all);
		assertTrue(all.contains("{apikey}"), all);
		assertThrows(IllegalStateException.class, bad::createProvider);
	}

	@Test
	void webPagesAreNeverFetchedMoreThanOnceAnHour()
	{
		ProviderDefinition page = ProviderDefinition.of(props("id", "p", "name", "P", "type", "web-page", "url", "https://x.test/",
				"minInterval", "5", "metal.tin.select", "td"));

		assertTrue(page.isValid(), page.problems().toString());
		assertEquals(Duration.ofHours(1), page.minInterval());
		assertEquals(Duration.ofHours(1), ProviderDefinition.of(props("id", "p", "name", "P", "type", "web-page", "url", "https://x.test/",
				"metal.tin.select", "td")).minInterval());
	}

	@Test
	void robotsTxtPicksOurGroupAndTheLongestRule()
	{
		String text = "User-agent: *\nDisallow: /\n\nUser-agent: IndianGold\nUser-agent: other\nDisallow: /private/\nAllow: /private/prices$\n";
		RobotsTxt ours = RobotsTxt.parse(text, "IndianGold/5.0 (+https://example.test)");
		assertTrue(ours.allows("/en/markdaten.php"));
		assertFalse(ours.allows("/private/x"));
		assertTrue(ours.allows("/private/prices"));
		assertFalse(ours.allows("/private/prices/old"));

		RobotsTxt anyone = RobotsTxt.parse(text, "SomeBot/1.0");
		assertFalse(anyone.allows("/en/markdaten.php"));

		RobotsTxt wildcard = RobotsTxt.parse("User-agent: *\nDisallow: /*?\nAllow: /\n", "IndianGold/5.0");
		assertTrue(wildcard.allows("/gold"));
		assertFalse(wildcard.allows("/gold?ref=1"));
		assertTrue(RobotsTxt.parse("User-agent: *\nDisallow:\n", "IndianGold/5.0").allows("/anything"));
	}

	@Test
	void numbersAreReadAsWrittenOnThePage()
	{
		assertEquals(Optional.of(14430.0), DefinitionProvider.parseNumber(" 14,430.00 ", '.', ','));
		assertEquals(Optional.of(1234.5), DefinitionProvider.parseNumber("1.234,50 \u20ac", ',', '.'));
		assertEquals(Optional.of(1234.5), DefinitionProvider.parseNumber("CHF 1'234.50", '.', ','));
		assertEquals(Optional.of(4156.9), DefinitionProvider.parseNumber("4,156.90 4,163.45", '.', ','), "only the first number");
		assertEquals(Optional.empty(), DefinitionProvider.parseNumber(" - ", '.', ','));
	}

	@Test
	void datesAreReadWithOrWithoutAPattern()
	{
		assertEquals(Optional.of(Instant.parse("2026-10-05T00:00:00Z")), DefinitionProvider.parseInstant("05. October 2026", "dd. MMMM yyyy", Locale.ENGLISH));
		assertEquals(Optional.of(Instant.parse("2026-10-06T14:13:40Z")), DefinitionProvider.parseInstant("2026-10-06T14:13:40Z", null, Locale.ENGLISH));
		assertEquals(Optional.of(Instant.parse("2026-10-06T00:00:00Z")), DefinitionProvider.parseInstant("2026-10-06", null, Locale.ENGLISH));
		assertEquals(Optional.of(Instant.ofEpochSecond(1_791_000_000L)), DefinitionProvider.parseInstant("1791000000", null, Locale.ENGLISH));
		assertEquals(Optional.empty(), DefinitionProvider.parseInstant("yesterday", null, Locale.ENGLISH));
	}

	@Test
	void pricesConvertToAnyCurrencyUnitAndPurity()
	{
		Quote gold = Quote.of(Metal.GOLD, 3110.34768, 1, MassUnit.TROY_OUNCE, "USD", Instant.EPOCH, "test");
		assertEquals(100.0, gold.pricePerGram(), 1e-9);

		FxRates fx = new FxRates(java.util.Map.of("INR", 90.0, "EUR", 0.9), Instant.EPOCH, "test");
		assertEquals(100 * 10 * 0.916 * 90, gold.priceIn(fx, "INR", 10, 0.916), 1e-6, "22K per 10 g in rupees");
		assertTrue(Double.isNaN(gold.priceIn(fx, "JPY", 1, 1)), "missing rate");

		Quote rupeeQuote = Quote.of(Metal.SILVER, 90_000, 1, MassUnit.KILOGRAM, "inr", Instant.EPOCH, "test");
		assertEquals(90.0 / 90 * 0.9, rupeeQuote.priceIn(fx, "EUR", 1, 1), 1e-9, "INR per kg to EUR per g");
		assertThrows(IllegalArgumentException.class, () -> Quote.of(Metal.TIN, 0, 1, MassUnit.TONNE, "USD", Instant.EPOCH, "test"));
	}
}
