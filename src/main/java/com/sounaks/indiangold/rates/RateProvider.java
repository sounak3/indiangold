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

import java.net.URI;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A source of metal prices and, optionally, exchange rates.
 * <p>
 * Most sources need no Java code: a definition file (see {@link ProviderDefinition}) describes a JSON API or a web page.
 * For anything a definition cannot express, implement this interface with a public no-argument constructor and list the
 * class in {@code META-INF/services/com.sounaks.indiangold.rates.RateProvider}; jars placed in
 * {@code ~/.indiangold/plugins/} are loaded at start.
 * @author Sounak Choudhury
 */
public interface RateProvider
{
	/** A stable identifier, used in settings; lower case letters, digits and dashes. */
	String id();

	/** The name shown to the user. */
	String name();

	/** One line saying what the source offers and what it costs. */
	String description();

	/** The metals this source can supply. */
	Set<Metal> metals();

	/** Whether it also supplies exchange rates. */
	default boolean providesFx()
	{
		return false;
	}

	default boolean needsApiKey()
	{
		return false;
	}

	/** Where the user can get an API key. */
	default Optional<URI> signupPage()
	{
		return Optional.empty();
	}

	/** Advice shown next to "Get a free key...", e.g. which sign-in gives the bigger free plan. */
	default Optional<String> signupHint()
	{
		return Optional.empty();
	}

	/** The source's terms of use, shown next to it in the settings. */
	default Optional<URI> termsPage()
	{
		return Optional.empty();
	}

	/** Whether this source reads a web page rather than an API; such sources show a disclaimer. */
	default boolean isWebPage()
	{
		return false;
	}

	/** The shortest time between two fetches. */
	default Duration minInterval()
	{
		return Duration.ofMinutes(1);
	}

	/** The number of requests allowed per month, or 0 if unlimited. Limited sources are fetched on a schedule. */
	default int monthlyQuota()
	{
		return 0;
	}

	/** Requests one fetch uses, counted against the quota. */
	default int requestsPerFetch()
	{
		return 1;
	}

	/** The default times of day to fetch a source with a quota. */
	default List<LocalTime> defaultSchedule()
	{
		return List.of();
	}

	/** Whether the user enters the prices by hand, so the source is never fetched automatically. */
	default boolean isManual()
	{
		return false;
	}

	/**
	 * Fetches the current prices.
	 * @param context The settings, HTTP client and clock to use.
	 * @return The prices found; may hold only some of {@link #metals()}.
	 * @throws RateException With a message for the user if nothing usable could be fetched.
	 */
	RateSnapshot fetch(FetchContext context) throws RateException;

	/**
	 * Asks the source how much of its quota is used, if it can tell.
	 * @param context The settings, HTTP client and clock to use.
	 * @return The usage, or empty if the source cannot report it.
	 * @throws RateException If the source could not be asked.
	 */
	default Optional<Usage> usage(FetchContext context) throws RateException
	{
		return Optional.empty();
	}

	/**
	 * Quota usage as reported by the source.
	 * @param plan The plan name, e.g. "Free".
	 * @param total Requests allowed this month.
	 * @param used Requests used this month.
	 */
	record Usage(String plan, int total, int used)
	{
		public int remaining()
		{
			return Math.max(0, total - used);
		}
	}
}
