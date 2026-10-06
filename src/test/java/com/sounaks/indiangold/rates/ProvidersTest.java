package com.sounaks.indiangold.rates;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The built-in definitions, run against recorded responses served locally. */
class ProvidersTest
{
	private static final double GRAMS_PER_TROY_OUNCE = 31.1034768;
	private static final double GRAMS_PER_POUND = 453.59237;
	private LocalServer server;

	@BeforeEach
	void start() throws Exception
	{
		server = new LocalServer();
	}

	@AfterEach
	void stop()
	{
		server.close();
	}

	private void serveGoldApi()
	{
		for (String symbol : new String[] { "XAU", "XAG", "XPT", "XPD", "HG" })
			server.reply("/api.gold-api.com/price/" + symbol, 200, LocalServer.resource("gold-api-" + symbol + ".json"));
	}

	@Test
	void goldApiReadsFiveMetalsWithTheirOwnUnits() throws Exception
	{
		serveGoldApi();
		RateSnapshot snapshot = server.builtIn("gold-api").createProvider().fetch(LocalServer.context(null));

		assertEquals(Set.of(Metal.GOLD, Metal.SILVER, Metal.PLATINUM, Metal.PALLADIUM, Metal.COPPER), snapshot.quotes().keySet());
		Quote gold = snapshot.quotes().get(Metal.GOLD);
		assertEquals(4154.100098 / GRAMS_PER_TROY_OUNCE, gold.pricePerGram(), 1e-9);
		assertEquals("USD", gold.currency());
		assertEquals(Instant.parse("2026-10-06T14:13:40Z"), gold.asOf());
		assertEquals(6.563406 / GRAMS_PER_POUND, snapshot.quotes().get(Metal.COPPER).pricePerGram(), 1e-12, "copper (HG) is per pound");
		assertTrue(snapshot.fx().isEmpty());
	}

	@Test
	void requestsIdentifyTheAppHonestly() throws Exception
	{
		serveGoldApi();
		server.builtIn("gold-api").createProvider().fetch(LocalServer.context(null));

		assertFalse(server.requests.isEmpty());
		server.requests.forEach(r -> assertEquals("IndianGold/5.0 (+https://github.com/sounak3/indiangold)", r.userAgent()));
	}

	@Test
	void goldApiKeepsTheMetalsItGotWhenOneRequestFails() throws Exception
	{
		serveGoldApi();
		server.reply("/api.gold-api.com/price/XPT", 503, "busy");

		RateSnapshot snapshot = server.builtIn("gold-api").createProvider().fetch(LocalServer.context(null));

		assertEquals(4, snapshot.quotes().size());
		assertFalse(snapshot.quotes().containsKey(Metal.PLATINUM));
	}

	@Test
	void goldApiReportsMissingAddressesWhenEveryRequestFails()
	{
		RateException e = assertThrows(RateException.class, () -> server.builtIn("gold-api").createProvider().fetch(LocalServer.context(null)));
		assertEquals(RateException.Kind.CONFIGURATION, e.kind(), "every URL answered 404");
	}

	@Test
	void unreachableServerIsANetworkError()
	{
		ProviderDefinition definition = server.builtIn("gold-api");
		server.close();

		RateException e = assertThrows(RateException.class, () -> definition.createProvider().fetch(LocalServer.context(null)));
		assertEquals(RateException.Kind.NETWORK, e.kind());
		assertTrue(e.getMessage().startsWith("Cannot reach 127.0.0.1"), e.getMessage());
	}

	@Test
	void currencyApiInvertsOuncesPerDollarAndReadsExchangeRates() throws Exception
	{
		server.reply("/cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json", 200, LocalServer.resource("currency-api-usd.json"));

		RateSnapshot snapshot = server.builtIn("currency-api").createProvider().fetch(LocalServer.context(null));

		assertEquals(1 / 0.0002425592 / GRAMS_PER_TROY_OUNCE, snapshot.quotes().get(Metal.GOLD).pricePerGram(), 1e-9);
		FxRates fx = snapshot.fx().orElseThrow();
		assertEquals(96.41805659, fx.fromUsd(1, "INR"), 1e-9);
		assertEquals(Instant.parse("2026-10-06T00:00:00Z"), fx.asOf());
	}

	@Test
	void currencyApiUsesTheMirrorWhenTheCdnFails() throws Exception
	{
		server.reply("/cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json", 503, "down");
		server.reply("/latest.currency-api.pages.dev/v1/currencies/usd.min.json", 200, LocalServer.resource("currency-api-usd.json"));

		RateSnapshot snapshot = server.builtIn("currency-api").createProvider().fetch(LocalServer.context(null));

		assertEquals(4, snapshot.quotes().size());
		assertEquals(1, server.count("/latest.currency-api.pages.dev/"));
	}

	@Test
	void metalsDevNeedsAKey()
	{
		RateException e = assertThrows(RateException.class, () -> server.builtIn("metals-dev").createProvider().fetch(LocalServer.context(null)));
		assertEquals(RateException.Kind.API_KEY, e.kind());
		assertTrue(e.getMessage().contains("Settings"));
		assertEquals(0, server.requests.size(), "nothing is sent without a key");
	}

	@Test
	void metalsDevReadsAllMetalsAndExchangeRatesInOneRequest() throws Exception
	{
		server.reply("/api.metals.dev/v1/latest", 200, LocalServer.resource("metals-dev-latest.json"));

		RateSnapshot snapshot = server.builtIn("metals-dev").createProvider().fetch(LocalServer.context("my key&x"));

		assertEquals(9, snapshot.quotes().size());
		assertEquals(14430.0 / 1_000_000, snapshot.quotes().get(Metal.COPPER).pricePerGram(), 1e-12, "industrial metals are per tonne");
		assertEquals(1 / 0.0103715, snapshot.fx().orElseThrow().fromUsd(1, "INR"), 1e-6, "currencies are dollars per unit");
		assertEquals(1, server.requests.size());
		assertEquals("/api.metals.dev/v1/latest?api_key=my+key%26x&currency=USD", server.requests.get(0).pathAndQuery(), "the key is URL-encoded");
	}

	@Test
	void metalsDevErrorsBecomeClearMessages()
	{
		server.reply("/api.metals.dev/v1/latest", 401, LocalServer.resource("metals-dev-error-1101.json"));
		RateException invalidKey = assertThrows(RateException.class, () -> server.builtIn("metals-dev").createProvider().fetch(LocalServer.context("bad")));
		assertEquals(RateException.Kind.API_KEY, invalidKey.kind());
		assertTrue(invalidKey.getMessage().contains("API Key provided is invalid"), invalidKey.getMessage());

		server.reply("/api.metals.dev/v1/latest", 200, LocalServer.resource("metals-dev-error-1203.json"));
		RateException quota = assertThrows(RateException.class, () -> server.builtIn("metals-dev").createProvider().fetch(LocalServer.context("ok")));
		assertEquals(RateException.Kind.QUOTA, quota.kind());
	}

	@Test
	void metalsDevReportsUsage() throws Exception
	{
		server.reply("/api.metals.dev/usage", 200, LocalServer.resource("metals-dev-usage.json"));

		RateProvider.Usage usage = server.builtIn("metals-dev").createProvider().usage(LocalServer.context("k")).orElseThrow();

		assertEquals(new RateProvider.Usage("Free", 100, 12), usage);
		assertEquals(88, usage.remaining());
	}

	@Test
	void westmetallReadsTheLmePriceTableNotTheStocksTable() throws Exception
	{
		server.reply("/www.westmetall.com/en/markdaten.php", 200, LocalServer.resource("westmetall.html"));

		RateSnapshot snapshot = server.builtIn("westmetall").createProvider().fetch(LocalServer.context(null));

		assertEquals(Set.of(Metal.COPPER, Metal.ALUMINUM, Metal.NICKEL, Metal.ZINC, Metal.LEAD, Metal.TIN), snapshot.quotes().keySet());
		assertEquals(14430.0 / 1_000_000, snapshot.quotes().get(Metal.COPPER).pricePerGram(), 1e-12);
		assertEquals(54250.0 / 1_000_000, snapshot.quotes().get(Metal.TIN).pricePerGram(), 1e-12);
		assertEquals(Instant.parse("2026-10-05T00:00:00Z"), snapshot.quotes().get(Metal.TIN).asOf());
	}

	@Test
	void webPagesAreNotFetchedWhenRobotsTxtDisallowsThem()
	{
		server.reply("/robots.txt", 200, "User-agent: *\nDisallow: /www.westmetall.com/\n");
		server.reply("/www.westmetall.com/en/markdaten.php", 200, LocalServer.resource("westmetall.html"));

		RateException e = assertThrows(RateException.class, () -> server.builtIn("westmetall").createProvider().fetch(LocalServer.context(null)));

		assertEquals(RateException.Kind.NOT_ALLOWED, e.kind());
		assertEquals(0, server.count("/www.westmetall.com/"));
	}

	@Test
	void changedWebPageIsReportedAsUnexpected()
	{
		server.reply("/www.westmetall.com/en/markdaten.php", 200, "<html><body><p>Redesigned</p></body></html>");

		RateException e = assertThrows(RateException.class, () -> server.builtIn("westmetall").createProvider().fetch(LocalServer.context(null)));
		assertEquals(RateException.Kind.UNEXPECTED_RESPONSE, e.kind());
	}
}
