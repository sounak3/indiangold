/*
 * Copyright (C) 2026 Sounak Choudhury
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sounaks.indiangold.rates;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Keeps prices up to date. All fetching happens on one background thread, never on the Swing thread:
 * <ul>
 * <li>sources without a quota are fetched every few minutes (the user's choice, but never more often than the
 * source's minimum interval, which is at least an hour for web pages);</li>
 * <li>sources with a monthly quota are fetched at their scheduled times, with one catch-up fetch at start if a
 * scheduled time was missed while the app was closed;</li>
 * <li>manual prices change only when the user saves them.</li>
 * </ul>
 * For each metal the first enabled source (in the user's order) with a recent price wins.
 * @author Sounak Choudhury
 */
public final class RateService implements AutoCloseable
{
	/** A price older than this gives way to a fresher one from a lower-priority source. */
	static final Duration STALE_AFTER = Duration.ofHours(48);

	/** What the service needs from the app's settings. */
	public interface Settings
	{
		/** The sources in priority order, with whether each is enabled. */
		List<SourceChoice> sources();

		/** The user's settings for one provider, e.g. its API key. */
		ProviderSettings providerSettings(String providerId);

		/** Minutes between automatic fetches of sources without a quota; 0 means only at start and on request. */
		int autoRefreshMinutes();

		/** The fetch times for a source with a quota: empty if the user has not chosen any (then its default schedule is
		 * used), an empty list for no automatic updates. */
		Optional<List<LocalTime>> schedule(String providerId);
	}

	public record SourceChoice(String id, boolean enabled)
	{
	}

	/**
	 * How one source is doing, for the settings window and rate bar tooltips.
	 * @param providerId The source.
	 * @param lastSuccess When it last returned prices.
	 * @param lastError The last error, if the last fetch failed.
	 * @param nextAllowed The earliest time it may be fetched again.
	 */
	public record Status(String providerId, Optional<Instant> lastSuccess, Optional<String> lastError, Instant nextAllowed)
	{
	}

	/**
	 * The prices to show: the best quote for each metal and the best exchange rates.
	 * @param quotes The chosen price of each metal that any enabled source has.
	 * @param fx The exchange rates to convert them with.
	 */
	public record Rates(Map<Metal, Quote> quotes, FxRates fx)
	{
	}

	/** Called on the service's thread after every fetch; Swing code must hand over to the event thread. */
	public interface Listener
	{
		void ratesChanged(Rates rates);
	}

	private final ProviderRegistry registry;
	private final RateStore store;
	private final Settings settings;
	private final HttpFetcher http;
	private final Clock clock;
	private final ScheduledExecutorService executor;
	private final List<Listener> listeners = new CopyOnWriteArrayList<>();
	private boolean startedPublishing; // only touched on the service thread

	public RateService(ProviderRegistry registry, RateStore store, Settings settings, HttpFetcher http, Clock clock)
	{
		this.registry = registry;
		this.store = store;
		this.settings = settings;
		this.http = http;
		this.clock = clock;
		this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "rate-updates");
			thread.setDaemon(true);
			return thread;
		});
	}

	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	/** Fetches what is due now and then checks again every minute. */
	public void start()
	{
		executor.scheduleWithFixedDelay(() -> runSafely(this::fetchDue), 0, 1, TimeUnit.MINUTES);
	}

	@Override
	public void close()
	{
		executor.shutdownNow();
	}

	public ProviderRegistry registry()
	{
		return registry;
	}

	public RateStore store()
	{
		return store;
	}

	/**
	 * Fetches every enabled source now, as far as each source's minimum interval allows.
	 * @param includeLimited Whether to fetch sources with a quota too; the UI asks the user first.
	 * @return Completes when done.
	 */
	public CompletableFuture<Void> refreshNow(boolean includeLimited)
	{
		return CompletableFuture.runAsync(() -> runSafely(() -> {
			Instant now = clock.instant();
			for(RateProvider provider : enabledProviders())
			{
				if(provider.isManual() || (provider.monthlyQuota() > 0 && !includeLimited)) continue;
				if(!nextAllowed(provider).isAfter(now)) fetch(provider);
			}
			publish();
		}), executor);
	}

	/**
	 * Fetches one source now, e.g. after the user saved manual prices or entered an API key.
	 * @param providerId The source.
	 * @return Completes with the fetch's result.
	 */
	public CompletableFuture<Optional<RateException>> refresh(String providerId)
	{
		return CompletableFuture.supplyAsync(() -> {
			Optional<RateProvider> provider = registry.find(providerId).map(ProviderRegistry.Entry::provider);
			if(provider.isEmpty()) return Optional.of(new RateException(RateException.Kind.CONFIGURATION, "Unknown source " + providerId));
			Optional<RateException> result = fetch(provider.get());
			publish();
			return result;
		}, executor);
	}

	/**
	 * Fetches a source without storing anything, for the Test buttons. Counts against the source's quota.
	 * @param provider The source, possibly not yet saved.
	 * @param providerSettings Its settings, e.g. the API key being tested.
	 * @return Completes with what it returned, or fails with a RateException.
	 */
	public CompletableFuture<RateSnapshot> test(RateProvider provider, ProviderSettings providerSettings)
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				if(provider.monthlyQuota() > 0) budget(provider).record(provider.requestsPerFetch());
				return provider.fetch(new FetchContext(providerSettings, http, clock));
			}
			catch(RateException e)
			{
				throw new java.util.concurrent.CompletionException(e);
			}
		}, executor);
	}

	/**
	 * Asks a source for its quota usage and, if it answers, takes over its count.
	 * @param providerId The source.
	 * @return Completes with the usage, or empty if the source cannot tell.
	 */
	public CompletableFuture<Optional<RateProvider.Usage>> usage(String providerId)
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				RateProvider provider = registry.find(providerId).orElseThrow().provider();
				Optional<RateProvider.Usage> usage = provider.usage(new FetchContext(settings.providerSettings(providerId), http, clock));
				usage.ifPresent(u -> budget(provider).syncWith(u));
				saveStore();
				return usage;
			}
			catch(RateException e)
			{
				throw new java.util.concurrent.CompletionException(e);
			}
		}, executor);
	}

	public RateStore.QuotaBudget budget(RateProvider provider)
	{
		return store.budget(provider.id(), provider.monthlyQuota(), clock);
	}

	public Status status(RateProvider provider)
	{
		return new Status(provider.id(), store.lastSuccess(provider.id()), store.lastError(provider.id()), nextAllowed(provider));
	}

	/** The prices to show now, from what the sources returned last. */
	public Rates current()
	{
		Map<Metal, Quote> chosen = new EnumMap<>(Metal.class);
		Instant staleBefore = clock.instant().minus(STALE_AFTER);
		List<String> order = new ArrayList<>();
		for(RateProvider provider : enabledProviders()) order.add(provider.id());
		order.add(RateStore.LEGACY_ID); // prices from before 5.0, until a source has its own
		for(Metal metal : Metal.values())
		{
			Quote best = null;
			for(String id : order)
			{
				Quote quote = store.quotes(id).get(metal);
				if(quote == null) continue;
				if(best == null) best = quote;
				else if(best.asOf().isBefore(staleBefore) && quote.asOf().isAfter(best.asOf())) best = quote;
				if(!best.asOf().isBefore(staleBefore)) break;
			}
			if(best != null) chosen.put(metal, best);
		}
		FxRates fx = null;
		for(String id : order)
		{
			Optional<FxRates> candidate = store.fx(id);
			if(candidate.isPresent() && (fx == null || candidate.get().asOf().isAfter(fx.asOf()))) fx = candidate.get();
		}
		if(fx == null)
		{
			// Exchange rates from a source the user switched off are still better than none.
			for(ProviderRegistry.Entry entry : registry.entries())
			{
				Optional<FxRates> candidate = store.fx(entry.provider().id());
				if(candidate.isPresent() && (fx == null || candidate.get().asOf().isAfter(fx.asOf()))) fx = candidate.get();
			}
		}
		return new Rates(chosen, fx == null ? FxRates.usdOnly() : fx);
	}

	/** Enabled sources that exist, in the user's order. */
	List<RateProvider> enabledProviders()
	{
		List<RateProvider> providers = new ArrayList<>();
		for(SourceChoice choice : settings.sources())
		{
			if(choice.enabled()) registry.find(choice.id()).ifPresent(entry -> providers.add(entry.provider()));
		}
		return providers;
	}

	/** Fetches every enabled source that is due; runs on the service thread. */
	void fetchDue()
	{
		boolean fetched = false;
		for(RateProvider provider : enabledProviders())
		{
			if(isDue(provider, clock.instant()))
			{
				fetch(provider);
				fetched = true;
			}
		}
		if(fetched || !startedPublishing)
		{
			startedPublishing = true;
			publish();
		}
	}


	boolean isDue(RateProvider provider, Instant now)
	{
		if(provider.isManual()) return store.lastAttempt(provider.id()).isEmpty();
		if(nextAllowed(provider).isAfter(now)) return false;
		Optional<Instant> last = store.lastAttempt(provider.id());
		if(provider.monthlyQuota() > 0)
		{
			if(budget(provider).remainingThisMonth() < provider.requestsPerFetch()) return false;
			if(provider.needsApiKey() && settings.providerSettings(provider.id()).apiKey().isEmpty()) return false;
			Optional<Instant> slot = lastScheduledTime(provider, now);
			return slot.isPresent() && (last.isEmpty() || last.get().isBefore(slot.get()));
		}
		if(last.isEmpty()) return true;
		int minutes = settings.autoRefreshMinutes();
		if(minutes <= 0) return false;
		Duration interval = Duration.ofMinutes(minutes);
		if(interval.compareTo(provider.minInterval()) < 0) interval = provider.minInterval();
		return !last.get().plus(interval).isAfter(now);
	}

	/** The most recent scheduled time not after now, within the last day. */
	Optional<Instant> lastScheduledTime(RateProvider provider, Instant now)
	{
		List<LocalTime> times = settings.schedule(provider.id()).orElse(provider.defaultSchedule());
		ZonedDateTime local = now.atZone(clock.getZone());
		Instant best = null;
		for(LocalDate day : new LocalDate[] { local.toLocalDate(), local.toLocalDate().minusDays(1) })
		{
			for(LocalTime time : times)
			{
				Instant slot = day.atTime(time).atZone(clock.getZone()).toInstant();
				if(!slot.isAfter(now) && (best == null || slot.isAfter(best))) best = slot;
			}
		}
		return Optional.ofNullable(best);
	}

	Instant nextAllowed(RateProvider provider)
	{
		return store.lastAttempt(provider.id()).map(last -> last.plus(provider.minInterval())).orElse(Instant.MIN);
	}

	/** Fetches one source and stores the result; runs on the service thread. */
	private Optional<RateException> fetch(RateProvider provider)
	{
		Instant now = clock.instant();
		store.recordAttempt(provider.id(), now);
		if(provider.monthlyQuota() > 0) budget(provider).record(provider.requestsPerFetch());
		Optional<RateException> failure = Optional.empty();
		try
		{
			RateSnapshot snapshot = provider.fetch(new FetchContext(settings.providerSettings(provider.id()), http, clock));
			store.put(snapshot);
		}
		catch(RateException e)
		{
			store.recordError(provider.id(), e.getMessage());
			failure = Optional.of(e);
		}
		catch(RuntimeException e)
		{
			// a plug-in's bug must not stop the other sources
			store.recordError(provider.id(), provider.name() + " failed: " + e);
			failure = Optional.of(new RateException(RateException.Kind.UNEXPECTED_RESPONSE, provider.name() + " failed: " + e, e));
		}
		saveStore();
		return failure;
	}

	private void saveStore()
	{
		try
		{
			store.save();
		}
		catch(IOException e)
		{
			System.out.println("Cannot save the market rates: " + e);
		}
	}

	private void publish()
	{
		Rates rates = current();
		for(Listener listener : listeners)
		{
			try
			{
				listener.ratesChanged(rates);
			}
			catch(RuntimeException e)
			{
				System.out.println("A rate listener failed: " + e);
			}
		}
	}

	private static void runSafely(Runnable task)
	{
		try
		{
			task.run();
		}
		catch(RuntimeException e)
		{
			// An exception would cancel the scheduled task for good, which is what broke the old rate bar.
			System.out.println("Updating market rates failed: " + e);
		}
	}
}
