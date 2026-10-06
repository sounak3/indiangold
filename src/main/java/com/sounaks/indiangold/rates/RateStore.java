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
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The last prices, exchange rates, errors and quota counts of every source, kept in ~/.indiangold/rates.properties
 * so the rate bar shows the last known prices at start and offline.
 * @author Sounak Choudhury
 */
public final class RateStore
{
	public static final String FILE_NAME = "rates.properties";
	/** Provider id given to prices imported from versions before 5.0. */
	public static final String LEGACY_ID = "earlier-version";

	private final Path file;
	private final Properties values;

	private RateStore(Path file, Properties values)
	{
		this.file = file;
		this.values = values;
	}

	/**
	 * Opens the store in the data folder, or an empty one if it does not exist yet.
	 * @param dataDir The app's data folder.
	 * @return The store.
	 */
	public static RateStore open(Path dataDir)
	{
		Path file = dataDir.resolve(FILE_NAME);
		return new RateStore(file, SafeFiles.readProperties(file).orElseGet(Properties::new));
	}

	/** A store that is never saved, for tests and the definition editor's Test button. */
	public static RateStore inMemory()
	{
		return new RateStore(null, new Properties());
	}

	public synchronized boolean isEmpty()
	{
		return values.isEmpty();
	}

	public synchronized void save() throws IOException
	{
		if(file != null) SafeFiles.writeProperties(file, values, "IndianGold market rates");
	}

	/**
	 * Records a successful fetch. Metals missing from the snapshot keep their previous prices.
	 * @param snapshot What the provider returned.
	 */
	public synchronized void put(RateSnapshot snapshot)
	{
		String id = snapshot.providerId();
		snapshot.quotes().values().forEach(quote ->
				values.setProperty("quote." + id + "." + quote.metal().key(), quote.pricePerGram() + "|" + quote.currency() + "|" + quote.asOf().toEpochMilli()));
		snapshot.fx().ifPresent(fx -> {
			values.stringPropertyNames().stream().filter(key -> key.startsWith("fx." + id + ".")).forEach(values::remove);
			fx.perUsd().forEach((code, rate) -> values.setProperty("fx." + id + "." + code, String.valueOf(rate)));
			values.setProperty("fxAsOf." + id, String.valueOf(fx.asOf().toEpochMilli()));
		});
		values.setProperty("success." + id, String.valueOf(snapshot.fetchedAt().toEpochMilli()));
		values.remove("error." + id);
	}

	public synchronized void recordAttempt(String providerId, Instant at)
	{
		values.setProperty("attempt." + providerId, String.valueOf(at.toEpochMilli()));
	}

	public synchronized void recordError(String providerId, String message)
	{
		values.setProperty("error." + providerId, message);
	}

	public synchronized Optional<String> lastError(String providerId)
	{
		return Optional.ofNullable(values.getProperty("error." + providerId));
	}

	public synchronized Optional<Instant> lastAttempt(String providerId)
	{
		return instant("attempt." + providerId);
	}

	public synchronized Optional<Instant> lastSuccess(String providerId)
	{
		return instant("success." + providerId);
	}

	private Optional<Instant> instant(String key)
	{
		try
		{
			return Optional.ofNullable(values.getProperty(key)).map(Long::parseLong).map(Instant::ofEpochMilli);
		}
		catch(NumberFormatException e)
		{
			return Optional.empty();
		}
	}

	/** The last prices a provider returned. */
	public synchronized Map<Metal, Quote> quotes(String providerId)
	{
		Map<Metal, Quote> quotes = new EnumMap<>(Metal.class);
		for(Metal metal : Metal.values())
		{
			String value = values.getProperty("quote." + providerId + "." + metal.key());
			if(value == null) continue;
			String[] parts = value.split("\\|");
			try
			{
				quotes.put(metal, new Quote(metal, Double.parseDouble(parts[0]), parts[1], Instant.ofEpochMilli(Long.parseLong(parts[2])), providerId));
			}
			catch(RuntimeException e)
			{
				// a damaged entry is skipped; the next fetch replaces it
			}
		}
		return quotes;
	}

	/** The last exchange rates a provider returned. */
	public synchronized Optional<FxRates> fx(String providerId)
	{
		String prefix = "fx." + providerId + ".";
		Map<String, Double> perUsd = new HashMap<>();
		for(String key : values.stringPropertyNames())
		{
			if(!key.startsWith(prefix)) continue;
			try
			{
				perUsd.put(key.substring(prefix.length()), Double.parseDouble(values.getProperty(key)));
			}
			catch(NumberFormatException e)
			{
				// skipped
			}
		}
		if(perUsd.size() < 2) return Optional.empty();
		return Optional.of(new FxRates(perUsd, instant("fxAsOf." + providerId).orElse(Instant.EPOCH), providerId));
	}

	/**
	 * Imports the prices that versions before 5.0 kept in units.dat (@gold=USD per troy ounce, base metals USD per
	 * pound, $ratetime, $convfactor and $currency), so the rate bar is not empty after an update.
	 * @param unitsDat The old settings.
	 */
	public synchronized void importLegacy(Properties unitsDat)
	{
		Instant asOf;
		try
		{
			asOf = Instant.ofEpochMilli(Long.parseLong(unitsDat.getProperty("$ratetime", "0")));
		}
		catch(NumberFormatException e)
		{
			asOf = Instant.EPOCH;
		}
		Map<Metal, Quote> quotes = new EnumMap<>(Metal.class);
		for(Metal metal : Metal.values())
		{
			String value = unitsDat.getProperty("@" + metal.key());
			if(value == null) continue;
			try
			{
				quotes.put(metal, Quote.of(metal, Double.parseDouble(value), 1, metal.isPrecious() ? MassUnit.TROY_OUNCE : MassUnit.POUND, "USD", asOf, LEGACY_ID));
			}
			catch(IllegalArgumentException e)
			{
				// skipped
			}
		}
		Optional<FxRates> fx = Optional.empty();
		try
		{
			String currency = unitsDat.getProperty("$currency", "USD");
			double factor = Double.parseDouble(unitsDat.getProperty("$convfactor", "1"));
			if(!currency.equalsIgnoreCase("USD") && factor > 0) fx = Optional.of(new FxRates(Map.of(currency, factor), asOf, LEGACY_ID));
		}
		catch(NumberFormatException e)
		{
			// no exchange rate then
		}
		if(!quotes.isEmpty() || fx.isPresent()) put(new RateSnapshot(LEGACY_ID, quotes, fx, asOf));
	}

	/**
	 * Gets the quota budget of a provider.
	 * @param providerId The provider.
	 * @param monthlyQuota Requests allowed per month.
	 * @param clock The clock; its zone decides when a day and month start.
	 * @return The budget.
	 */
	public QuotaBudget budget(String providerId, int monthlyQuota, Clock clock)
	{
		return new QuotaBudget(providerId, monthlyQuota, clock);
	}

	/** How much of a provider's monthly request quota is left, and how much of it is today's share. */
	public final class QuotaBudget
	{
		private final String prefix;
		private final int quota;
		private final Clock clock;

		private QuotaBudget(String providerId, int quota, Clock clock)
		{
			this.prefix = "quota." + providerId + ".";
			this.quota = quota;
			this.clock = clock;
		}

		public int monthlyQuota()
		{
			return quota;
		}

		public int usedThisMonth()
		{
			synchronized(RateStore.this)
			{
				return YearMonth.now(clock).toString().equals(values.getProperty(prefix + "month")) ? count("used") : 0;
			}
		}

		public int usedToday()
		{
			synchronized(RateStore.this)
			{
				return LocalDate.now(clock).toString().equals(values.getProperty(prefix + "day")) ? count("usedToday") : 0;
			}
		}

		public int remainingThisMonth()
		{
			return Math.max(0, quota - usedThisMonth());
		}

		/** Today's share: what is left this month (not counting today) divided by the days left, today included. */
		public int allowanceToday()
		{
			LocalDate today = LocalDate.now(clock);
			int daysLeft = today.lengthOfMonth() - today.getDayOfMonth() + 1;
			int leftAtStartOfToday = quota - (usedThisMonth() - usedToday());
			return Math.max(0, leftAtStartOfToday / daysLeft);
		}

		public int remainingToday()
		{
			return Math.min(remainingThisMonth(), Math.max(0, allowanceToday() - usedToday()));
		}

		/** Counts requests just made. */
		public void record(int requests)
		{
			synchronized(RateStore.this)
			{
				int month = usedThisMonth() + requests;
				int today = usedToday() + requests;
				values.setProperty(prefix + "month", YearMonth.now(clock).toString());
				values.setProperty(prefix + "used", String.valueOf(month));
				values.setProperty(prefix + "day", LocalDate.now(clock).toString());
				values.setProperty(prefix + "usedToday", String.valueOf(today));
			}
		}

		/** Takes the provider's own count, which also includes requests made from other computers with the same key. */
		public void syncWith(RateProvider.Usage usage)
		{
			synchronized(RateStore.this)
			{
				int today = usedToday();
				values.setProperty(prefix + "month", YearMonth.now(clock).toString());
				values.setProperty(prefix + "used", String.valueOf(Math.max(usage.used(), today)));
				values.setProperty(prefix + "day", LocalDate.now(clock).toString());
				values.setProperty(prefix + "usedToday", String.valueOf(today));
			}
		}

		private int count(String name)
		{
			try
			{
				return Integer.parseInt(values.getProperty(prefix + name, "0"));
			}
			catch(NumberFormatException e)
			{
				return 0;
			}
		}
	}
}
