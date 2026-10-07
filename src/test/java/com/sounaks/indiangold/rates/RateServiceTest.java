package com.sounaks.indiangold.rates;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Scheduling, quota budget, choosing between sources, and loading providers. */
class RateServiceTest
{
	private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

	/** A clock the test moves forward. */
	static final class TestClock extends Clock
	{
		private Instant now;

		TestClock(String localDateTime)
		{
			now = ZonedDateTime.of(java.time.LocalDateTime.parse(localDateTime), ZONE).toInstant();
		}

		void advance(Duration d)
		{
			now = now.plus(d);
		}

		void set(String localDateTime)
		{
			now = ZonedDateTime.of(java.time.LocalDateTime.parse(localDateTime), ZONE).toInstant();
		}

		@Override
		public ZoneId getZone()
		{
			return ZONE;
		}

		@Override
		public Clock withZone(ZoneId zone)
		{
			return this;
		}

		@Override
		public Instant instant()
		{
			return now;
		}
	}

	/** A provider that returns fixed prices and counts its fetches. */
	static final class FakeProvider implements RateProvider
	{
		final String id;
		final int quota;
		final Duration minInterval;
		final AtomicInteger fetches = new AtomicInteger();
		Map<Metal, Double> usdPerGram = new HashMap<>();
		Instant asOf;
		RuntimeException crash;
		RateException failure;

		FakeProvider(String id, int quota, Duration minInterval)
		{
			this.id = id;
			this.quota = quota;
			this.minInterval = minInterval;
		}

		@Override public String id() { return id; }
		@Override public String name() { return id; }
		@Override public String description() { return ""; }
		@Override public Set<Metal> metals() { return usdPerGram.isEmpty() ? EnumSet.of(Metal.GOLD) : EnumSet.copyOf(usdPerGram.keySet()); }
		@Override public int monthlyQuota() { return quota; }
		@Override public Duration minInterval() { return minInterval; }
		@Override public List<LocalTime> defaultSchedule() { return List.of(LocalTime.of(10, 30), LocalTime.of(16, 30)); }

		@Override
		public RateSnapshot fetch(FetchContext context) throws RateException
		{
			fetches.incrementAndGet();
			if(crash != null) throw crash;
			if(failure != null) throw failure;
			Map<Metal, Quote> quotes = new HashMap<>();
			Instant validAt = asOf == null ? context.clock().instant() : asOf;
			usdPerGram.forEach((metal, price) -> quotes.put(metal, new Quote(metal, price, "USD", validAt, id)));
			return new RateSnapshot(id, quotes, Optional.empty(), context.clock().instant());
		}
	}

	/** Settings held in memory. */
	static final class TestSettings implements RateService.Settings
	{
		final List<RateService.SourceChoice> sources = new ArrayList<>();
		final Map<String, Map<String, String>> perProvider = new HashMap<>();
		int autoRefreshMinutes = 10;

		@Override public List<RateService.SourceChoice> sources() { return sources; }
		@Override public int autoRefreshMinutes() { return autoRefreshMinutes; }
		final Map<String, List<LocalTime>> schedules = new HashMap<>();
		@Override public Optional<List<LocalTime>> schedule(String providerId) { return Optional.ofNullable(schedules.get(providerId)); }

		@Override
		public ProviderSettings providerSettings(String providerId)
		{
			Map<String, String> values = perProvider.computeIfAbsent(providerId, k -> new HashMap<>());
			return new ProviderSettings()
			{
				@Override public Optional<String> get(String name) { return Optional.ofNullable(values.get(name)); }
				@Override public void put(String name, String value) { values.put(name, value); }
			};
		}
	}

	private static RateService service(TestClock clock, TestSettings settings, RateStore store, RateProvider... providers)
	{
		ProviderRegistry registry = ProviderRegistry.load(null);
		for (RateProvider provider : providers) {
			registry.register(provider);
			settings.sources.add(new RateService.SourceChoice(provider.id(), true));
		}
		return new RateService(registry, store, settings, LocalServer.context(null).http(), clock);
	}

	@Test
	void dailyAllowanceSpreadsTheMonthlyQuota()
	{
		TestClock clock = new TestClock("2026-10-01T09:00:00");
		RateStore.QuotaBudget budget = RateStore.inMemory().budget("p", 100, clock);

		assertEquals(3, budget.allowanceToday(), "100 requests over 31 days");
		budget.record(2);
		assertEquals(1, budget.remainingToday());
		assertEquals(98, budget.remainingThisMonth());

		clock.set("2026-10-02T09:00:00");
		assertEquals(0, budget.usedToday());
		assertEquals(3, budget.allowanceToday(), "98 over 30 days");

		clock.set("2026-10-31T09:00:00");
		budget.record(88); // heavy use on the last day
		assertEquals(10, budget.remainingToday(), "on the last day, everything left this month is today's share");
		budget.record(10);
		assertEquals(0, budget.remainingToday());
		assertEquals(0, budget.remainingThisMonth());

		clock.set("2026-11-01T09:00:00");
		assertEquals(0, budget.usedThisMonth(), "a new month starts fresh");
		assertEquals(3, budget.allowanceToday(), "100 over 30 days");
	}

	@Test
	void providerUsageReplacesTheLocalCount()
	{
		TestClock clock = new TestClock("2026-10-07T09:00:00");
		RateStore.QuotaBudget budget = RateStore.inMemory().budget("p", 100, clock);
		budget.record(1);

		budget.syncWith(new RateProvider.Usage("Free", 100, 40)); // another computer used the same key

		assertEquals(40, budget.usedThisMonth());
		assertEquals(60, budget.remainingThisMonth());
	}

	@Test
	void sourcesWithoutQuotaAreFetchedAtStartAndThenEveryFewMinutes()
	{
		TestClock clock = new TestClock("2026-10-07T09:00:00");
		FakeProvider api = new FakeProvider("api", 0, Duration.ofMinutes(1));
		FakeProvider page = new FakeProvider("page", 0, Duration.ofHours(1));
		TestSettings settings = new TestSettings();
		RateService service = service(clock, settings, RateStore.inMemory(), api, page);

		service.fetchDue();
		assertEquals(1, api.fetches.get());
		assertEquals(1, page.fetches.get());

		clock.advance(Duration.ofMinutes(10));
		service.fetchDue();
		assertEquals(2, api.fetches.get());
		assertEquals(1, page.fetches.get(), "web pages wait an hour whatever the setting");

		clock.advance(Duration.ofMinutes(50));
		service.fetchDue();
		assertEquals(3, api.fetches.get());
		assertEquals(2, page.fetches.get());

		settings.autoRefreshMinutes = 0;
		clock.advance(Duration.ofHours(5));
		service.fetchDue();
		assertEquals(3, api.fetches.get(), "manual refresh only: no more automatic fetches");
		assertEquals(2, page.fetches.get());
	}

	@Test
	void sourcesWithQuotaAreFetchedOnlyAtScheduledTimes()
	{
		TestClock clock = new TestClock("2026-10-07T11:00:00");
		FakeProvider limited = new FakeProvider("limited", 100, Duration.ofMinutes(1));
		RateService service = service(clock, new TestSettings(), RateStore.inMemory(), limited);

		service.fetchDue();
		assertEquals(1, limited.fetches.get(), "the 10:30 update was missed while closed: one catch-up");
		service.fetchDue();
		clock.advance(Duration.ofHours(2));
		service.fetchDue();
		assertEquals(1, limited.fetches.get(), "nothing until 16:30");

		clock.set("2026-10-07T16:31:00");
		service.fetchDue();
		assertEquals(2, limited.fetches.get());
		assertEquals(2, service.budget(limited).usedToday());
	}

	@Test
	void theUserCanChooseOtherTimesOrNone()
	{
		TestClock clock = new TestClock("2026-10-07T08:00:00");
		FakeProvider limited = new FakeProvider("limited", 100, Duration.ofMinutes(1));
		TestSettings settings = new TestSettings();
		settings.schedules.put("limited", List.of(LocalTime.of(7, 45)));
		RateService service = service(clock, settings, RateStore.inMemory(), limited);

		service.fetchDue();
		assertEquals(1, limited.fetches.get(), "07:45 has passed");

		settings.schedules.put("limited", List.of());
		clock.set("2026-10-08T23:00:00");
		service.fetchDue();
		assertEquals(1, limited.fetches.get(), "no automatic updates at all");
	}

	@Test
	void sourcesNeedingAKeyWaitForIt()
	{
		TestClock clock = new TestClock("2026-10-07T11:00:00");
		FakeProvider keyed = new FakeProvider("keyed", 100, Duration.ofMinutes(1));
		RateProvider needsKey = new DelegatingKeyProvider(keyed);
		TestSettings settings = new TestSettings();
		RateService service = service(clock, settings, RateStore.inMemory(), needsKey);

		service.fetchDue();
		assertEquals(0, keyed.fetches.get());

		settings.providerSettings("keyed").put(ProviderSettings.API_KEY, "abc");
		service.fetchDue();
		assertEquals(1, keyed.fetches.get());
	}

	/** The fake provider, but needing an API key. */
	private static final class DelegatingKeyProvider implements RateProvider
	{
		private final FakeProvider inner;

		DelegatingKeyProvider(FakeProvider inner) { this.inner = inner; }
		@Override public String id() { return inner.id(); }
		@Override public String name() { return inner.name(); }
		@Override public String description() { return ""; }
		@Override public Set<Metal> metals() { return inner.metals(); }
		@Override public boolean needsApiKey() { return true; }
		@Override public int monthlyQuota() { return inner.monthlyQuota(); }
		@Override public List<LocalTime> defaultSchedule() { return inner.defaultSchedule(); }
		@Override public RateSnapshot fetch(FetchContext context) throws RateException { return inner.fetch(context); }
	}

	@Test
	void firstEnabledSourceWinsUnlessItsPriceIsStale()
	{
		TestClock clock = new TestClock("2026-10-07T09:00:00");
		FakeProvider first = new FakeProvider("first", 0, Duration.ofMinutes(1));
		FakeProvider second = new FakeProvider("second", 0, Duration.ofMinutes(1));
		first.usdPerGram.put(Metal.GOLD, 130.0);
		second.usdPerGram.put(Metal.GOLD, 131.0);
		second.usdPerGram.put(Metal.TIN, 0.054);
		TestSettings settings = new TestSettings();
		RateService service = service(clock, settings, RateStore.inMemory(), first, second);

		service.fetchDue();
		RateService.Rates rates = service.current();
		assertEquals(130.0, rates.quotes().get(Metal.GOLD).pricePerGram(), "priority order");
		assertEquals("second", rates.quotes().get(Metal.TIN).providerId(), "missing metals come from the next source");

		first.failure = new RateException(RateException.Kind.NETWORK, "offline");
		clock.advance(Duration.ofDays(3));
		service.fetchDue();
		assertEquals(131.0, service.current().quotes().get(Metal.GOLD).pricePerGram(), "a 3-day-old price gives way to a fresh one");
		assertEquals(Optional.of("offline"), service.status(first).lastError());

		settings.sources.set(1, new RateService.SourceChoice("second", false));
		assertEquals(130.0, service.current().quotes().get(Metal.GOLD).pricePerGram(), "disabled sources are ignored; stale beats nothing");
	}

	@Test
	void aCrashingPlugInDoesNotStopTheOthers()
	{
		TestClock clock = new TestClock("2026-10-07T09:00:00");
		FakeProvider broken = new FakeProvider("broken", 0, Duration.ofMinutes(1));
		broken.crash = new IllegalStateException("bug");
		FakeProvider fine = new FakeProvider("fine", 0, Duration.ofMinutes(1));
		fine.usdPerGram.put(Metal.SILVER, 2.0);
		RateService service = service(clock, new TestSettings(), RateStore.inMemory(), broken, fine);

		service.fetchDue();

		assertEquals(2.0, service.current().quotes().get(Metal.SILVER).pricePerGram());
		assertTrue(service.status(broken).lastError().orElseThrow().contains("bug"));
	}

	@Test
	void pricesFromEarlierVersionsAreShownUntilASourceHasItsOwn()
	{
		Properties old = new Properties();
		old.setProperty("@gold", "1779.00");
		old.setProperty("@copper", "4.2455");
		old.setProperty("@rhodium", "17800.00");
		old.setProperty("$ratetime", "1624903927201");
		old.setProperty("$currency", "INR");
		old.setProperty("$convfactor", "74.283006");
		RateStore store = RateStore.inMemory();
		store.importLegacy(old);
		TestClock clock = new TestClock("2026-10-07T09:00:00");
		RateService service = service(clock, new TestSettings(), store);

		RateService.Rates rates = service.current();

		assertEquals(1779.00 / 31.1034768, rates.quotes().get(Metal.GOLD).pricePerGram(), 1e-9);
		assertEquals(4.2455 / 453.59237, rates.quotes().get(Metal.COPPER).pricePerGram(), 1e-12, "base metals were per pound");
		assertEquals(74.283006, rates.fx().fromUsd(1, "INR"), 1e-9);
		assertEquals(2, rates.quotes().size(), "rhodium is gone");
	}

	@Test
	void registryLoadsUserDefinitionsAndExplainsBadOnes(@TempDir Path dataDir) throws Exception
	{
		Path folder = Files.createDirectories(dataDir.resolve("providers"));
		Files.writeString(folder.resolve("my-shop.properties"), "id=my-shop\nname=My shop\ntype=web-page\nurl=https://shop.test/rates\nmetal.gold.select=#gold\nmetal.gold.unit=g\nmetal.gold.per=10\ncurrency=INR\n");
		Files.writeString(folder.resolve("broken.properties"), "id=broken\ntype=json-api\n");
		Files.writeString(folder.resolve("clash.properties"), "id=gold-api\nname=Mine\ntype=json-api\nurl=https://x.test/\nmetal.gold.path=/g\n");

		ProviderRegistry registry = ProviderRegistry.load(dataDir);

		assertEquals(ProviderRegistry.Origin.USER_DEFINITION, registry.find("my-shop").orElseThrow().origin());
		assertTrue(registry.find("my-shop").orElseThrow().provider().isWebPage());
		assertEquals(ProviderRegistry.Origin.BUILT_IN, registry.find("manual").orElseThrow().origin(), "Java providers come from ServiceLoader");
		assertEquals(List.of("gold-api", "westmetall", "currency-api", "metals-dev", "manual"),
				registry.entries().stream().filter(e -> e.origin() == ProviderRegistry.Origin.BUILT_IN).map(e -> e.provider().id()).toList());
		String problems = String.join("\n", registry.problems());
		assertTrue(problems.contains("broken.properties: name is missing"), problems);
		assertTrue(problems.contains("\"gold-api\" is already used"), problems);
	}

	@Test
	void userDefinitionsCanBeSavedAndDeletedButBuiltInsCannot(@TempDir Path dataDir) throws Exception
	{
		ProviderRegistry registry = ProviderRegistry.load(dataDir);
		Properties copy = registry.find("westmetall").orElseThrow().definition().orElseThrow().toProperties();
		copy.setProperty("id", "westmetall-copy");

		Path file = registry.save(ProviderDefinition.of(copy));
		assertTrue(Files.isRegularFile(file));
		ProviderRegistry reloaded = ProviderRegistry.load(dataDir);
		assertTrue(reloaded.find("westmetall-copy").isPresent());

		assertThrows(IllegalArgumentException.class, () -> reloaded.save(registry.find("westmetall").orElseThrow().definition().orElseThrow()));
		assertThrows(IllegalArgumentException.class, () -> reloaded.delete("westmetall"));
		reloaded.delete("westmetall-copy");
		assertTrue(ProviderRegistry.load(dataDir).find("westmetall-copy").isEmpty());
	}

	@Test
	void manualPricesUseTheirDateAndCurrency()
	{
		TestSettings settings = new TestSettings();
		ProviderSettings manual = settings.providerSettings(ManualProvider.ID);
		manual.put(ManualProvider.DATE, "2026-10-06");
		manual.put(ManualProvider.CURRENCY, "INR");
		manual.put(ManualProvider.priceKey(Metal.GOLD), "121500");
		manual.put(ManualProvider.unitKey(Metal.GOLD), "g");
		manual.put(ManualProvider.perKey(Metal.GOLD), "10");
		manual.put(ManualProvider.priceKey(Metal.SILVER), "");
		TestClock clock = new TestClock("2026-10-07T09:00:00");

		RateSnapshot snapshot = assertDoesNotThrow(() -> new ManualProvider().fetch(new FetchContext(manual, LocalServer.context(null).http(), clock)));

		Quote gold = snapshot.quotes().get(Metal.GOLD);
		assertEquals(12150.0, gold.pricePerGram(), 1e-9);
		assertEquals("INR", gold.currency());
		assertEquals(ZonedDateTime.of(2026, 10, 6, 0, 0, 0, 0, ZONE).toInstant(), gold.asOf());
		assertEquals(Set.of(Metal.GOLD), snapshot.quotes().keySet());
	}
}
