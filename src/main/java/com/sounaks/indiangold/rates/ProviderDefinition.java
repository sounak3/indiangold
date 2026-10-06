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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * A rate source described by a properties file instead of Java code. Two types exist:
 * <ul>
 * <li>{@code json-api}: values are read from JSON responses with JSON pointers, e.g. {@code metal.gold.path=/metals/gold}.</li>
 * <li>{@code web-page}: values are read from an HTML page with CSS selectors, e.g. {@code metal.tin.select=...}.</li>
 * </ul>
 * Keys (only {@code id}, {@code name}, {@code type}, a URL and one metal are required):
 * <pre>
 * id, name, type, description, signup, terms
 * apikey=required            the URL contains {apikey}
 * url, url.fallback          tried in order when the first cannot be reached
 * currency=USD               currency of the prices (default USD)
 * minInterval=60             minutes between fetches (or an ISO duration such as PT1H)
 * quota.monthly=100          requests allowed per month; such sources are fetched on a schedule
 * schedule=10:30,16:30       default fetch times for sources with a quota
 * asOf.path / asOf.select, asOf.format, asOf.locale    where the price date is, and its format
 * error.path, error.code.path, error.kind.CODE=API_KEY|QUOTA|...   how a JSON API reports errors
 * number.decimal, number.grouping                      number format on a web page (default "." and ",")
 * metal.KEY.path | metal.KEY.select   where the price is   (KEY: gold, silver, platinum, palladium, copper, aluminum, nickel, zinc, lead, tin)
 * metal.KEY.url              a URL for this metal only (json-api)
 * metal.KEY.unit=toz         unit of the price (mg, g, kg, t, toz, lb, oz, tola, tael_hk, tael, baht)
 * metal.KEY.per=1            the price is for this many units
 * metal.KEY.invert=true      the value is units of metal per currency unit (e.g. ounces per dollar)
 * metal.KEY.currency         currency of this metal's price, if it differs
 * fx.path, fx.url            exchange rates: an object mapping currency codes to rates
 * fx.direction=perUsd|usdPerUnit   perUsd: units of the currency per US dollar; usdPerUnit: dollars per unit
 * usage.url, usage.plan.path, usage.total.path, usage.used.path   how to ask for quota usage
 * </pre>
 * @author Sounak Choudhury
 */
public final class ProviderDefinition
{
	public static final String TYPE_JSON_API = "json-api";
	public static final String TYPE_WEB_PAGE = "web-page";
	private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,40}");

	/**
	 * Where and how to read one metal's price.
	 * @param locator A JSON pointer (json-api) or CSS selector (web-page).
	 * @param url A URL for this metal only, or null to use the definition's URL.
	 * @param unit The unit of the price.
	 * @param per The quantity the price is for.
	 * @param invert Whether the value is units of metal per currency unit.
	 * @param currency The currency of the price.
	 */
	public record MetalSpec(String locator, String url, MassUnit unit, double per, boolean invert, String currency)
	{
	}

	private final Properties source;
	private final List<String> problems = new ArrayList<>();
	private final Map<Metal, MetalSpec> metals = new EnumMap<>(Metal.class);

	private ProviderDefinition(Properties properties)
	{
		this.source = new Properties();
		this.source.putAll(properties);
		validate();
	}

	/**
	 * Reads a definition. It is always returned; check {@link #problems()} before using it.
	 * @param properties The definition's keys.
	 * @return The definition.
	 */
	public static ProviderDefinition of(Properties properties)
	{
		return new ProviderDefinition(properties);
	}

	private void validate()
	{
		if(!ID.matcher(id()).matches()) problems.add("id must be lower-case letters, digits and dashes, e.g. \"my-source\".");
		if(name().isBlank()) problems.add("name is missing.");
		if(!type().equals(TYPE_JSON_API) && !type().equals(TYPE_WEB_PAGE)) problems.add("type must be " + TYPE_JSON_API + " or " + TYPE_WEB_PAGE + ".");
		for(String key : new TreeSet<>(source.stringPropertyNames()))
		{
			if(!key.startsWith("metal.") || !key.endsWith(isWebPage() ? ".select" : ".path")) continue;
			String metalKey = key.substring("metal.".length(), key.lastIndexOf('.'));
			Optional<Metal> metal = Metal.fromKey(metalKey);
			if(metal.isEmpty())
			{
				problems.add("Unknown metal \"" + metalKey + "\" in " + key + ".");
				continue;
			}
			String prefix = "metal." + metalKey + ".";
			String unitCode = get(prefix + "unit", "toz");
			Optional<MassUnit> unit = MassUnit.fromCode(unitCode);
			if(unit.isEmpty()) problems.add("Unknown unit \"" + unitCode + "\" in " + prefix + "unit.");
			double per = 1;
			try
			{
				per = Double.parseDouble(get(prefix + "per", "1"));
				if(!(per > 0)) problems.add(prefix + "per must be more than 0.");
			}
			catch(NumberFormatException e)
			{
				problems.add(prefix + "per must be a number.");
			}
			String url = source.getProperty(prefix + "url");
			if(url != null) checkUrl(prefix + "url", url);
			metals.put(metal.get(), new MetalSpec(source.getProperty(key).trim(), url, unit.orElse(MassUnit.TROY_OUNCE), per,
					Boolean.parseBoolean(get(prefix + "invert", "false")), get(prefix + "currency", currency())));
		}
		if(metals.isEmpty()) problems.add("Add at least one metal, e.g. metal.gold." + (isWebPage() ? "select" : "path") + "=...");
		boolean everyMetalHasUrl = !metals.isEmpty() && metals.values().stream().allMatch(spec -> spec.url() != null);
		if(url().isEmpty() && !everyMetalHasUrl) problems.add("url is missing.");
		url().ifPresent(value -> checkUrl("url", value));
		fallbackUrl().ifPresent(value -> checkUrl("url.fallback", value));
		if(minInterval().isZero()) problems.add("minInterval must be a number of minutes or a duration such as PT1H.");
		if(monthlyQuota() < 0) problems.add("quota.monthly must be a whole number.");
		if(schedule().size() != get("schedule", "").split(",").length && !get("schedule", "").isBlank())
			problems.add("schedule must be times like 10:30,16:30.");
		if(needsApiKey() && !get("url", "").contains("{apikey}") && metals.values().stream().noneMatch(m -> m.url() != null && m.url().contains("{apikey}")))
			problems.add("apikey=required but no URL contains {apikey}.");
		String direction = get("fx.direction", "perUsd");
		if(!direction.equals("perUsd") && !direction.equals("usdPerUnit")) problems.add("fx.direction must be perUsd or usdPerUnit.");
	}

	private void checkUrl(String key, String value)
	{
		try
		{
			URI uri = URI.create(value.replace("{apikey}", "KEY").replace("{currency}", "USD"));
			if(!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) problems.add(key + " must start with https://");
		}
		catch(IllegalArgumentException e)
		{
			problems.add(key + " is not a valid address: " + e.getMessage());
		}
	}

	private String get(String key, String fallback)
	{
		String value = source.getProperty(key);
		return value == null || value.isBlank() ? fallback : value.trim();
	}

	/** Problems that stop the definition from working; empty if it is usable. */
	public List<String> problems()
	{
		return Collections.unmodifiableList(problems);
	}

	public boolean isValid()
	{
		return problems.isEmpty();
	}

	public String id()
	{
		return get("id", "");
	}

	public String name()
	{
		return get("name", "");
	}

	public String type()
	{
		return get("type", "").toLowerCase(Locale.ROOT);
	}

	public boolean isWebPage()
	{
		return type().equals(TYPE_WEB_PAGE);
	}

	public String description()
	{
		return get("description", "");
	}

	public Optional<String> url()
	{
		return Optional.ofNullable(source.getProperty("url")).map(String::trim).filter(s -> !s.isEmpty());
	}

	public Optional<String> fallbackUrl()
	{
		return Optional.ofNullable(source.getProperty("url.fallback")).map(String::trim).filter(s -> !s.isEmpty());
	}

	public Optional<URI> signupPage()
	{
		return optionalUri("signup");
	}

	public Optional<URI> termsPage()
	{
		return optionalUri("terms");
	}

	private Optional<URI> optionalUri(String key)
	{
		try
		{
			return Optional.ofNullable(source.getProperty(key)).map(String::trim).filter(s -> !s.isEmpty()).map(URI::create);
		}
		catch(IllegalArgumentException e)
		{
			return Optional.empty();
		}
	}

	public boolean needsApiKey()
	{
		return get("apikey", "none").equalsIgnoreCase("required");
	}

	public String currency()
	{
		return get("currency", "USD").toUpperCase(Locale.ROOT);
	}

	/** The shortest time between fetches; web pages are never fetched more than once an hour. */
	public Duration minInterval()
	{
		String value = get("minInterval", isWebPage() ? "60" : "1");
		Duration interval;
		try
		{
			interval = value.startsWith("P") || value.startsWith("p") ? Duration.parse(value.toUpperCase(Locale.ROOT)) : Duration.ofMinutes(Long.parseLong(value));
		}
		catch(DateTimeParseException | NumberFormatException e)
		{
			return Duration.ZERO;
		}
		if(isWebPage() && interval.compareTo(Duration.ofHours(1)) < 0) return Duration.ofHours(1);
		return interval.isNegative() || interval.isZero() ? Duration.ofMinutes(1) : interval;
	}

	public int monthlyQuota()
	{
		try
		{
			return Integer.parseInt(get("quota.monthly", "0"));
		}
		catch(NumberFormatException e)
		{
			return -1;
		}
	}

	public List<LocalTime> schedule()
	{
		List<LocalTime> times = new ArrayList<>();
		for(String time : get("schedule", "").split(","))
		{
			try
			{
				if(!time.isBlank()) times.add(LocalTime.parse(time.trim()));
			}
			catch(DateTimeParseException e)
			{
				// reported by validate()
			}
		}
		return times;
	}

	public Map<Metal, MetalSpec> metals()
	{
		return Collections.unmodifiableMap(metals);
	}

	/**
	 * Gets any key, for the parts each provider type reads itself (asOf.*, error.*, fx.*, usage.*, number.*).
	 * @param key The key.
	 * @return Its trimmed value, or empty if missing or blank.
	 */
	public Optional<String> property(String key)
	{
		return Optional.ofNullable(source.getProperty(key)).map(String::trim).filter(s -> !s.isEmpty());
	}

	/** A copy of all keys, for saving or editing. */
	public Properties toProperties()
	{
		Properties copy = new Properties();
		copy.putAll(source);
		return copy;
	}

	/**
	 * Creates the provider this definition describes.
	 * @return The provider.
	 * @throws IllegalStateException If the definition has problems.
	 */
	public RateProvider createProvider()
	{
		if(!isValid()) throw new IllegalStateException("Definition " + id() + " has problems: " + problems);
		return isWebPage() ? new WebPageProvider(this) : new JsonApiProvider(this);
	}
}
