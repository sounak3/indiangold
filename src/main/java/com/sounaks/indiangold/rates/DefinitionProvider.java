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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Behaviour shared by providers built from a {@link ProviderDefinition}.
 * @author Sounak Choudhury
 */
abstract class DefinitionProvider implements RateProvider
{
	private static final Pattern NUMBER = Pattern.compile("[-+]?[0-9][0-9.,'\u00a0\u202f]*");

	protected final ProviderDefinition definition;

	DefinitionProvider(ProviderDefinition definition)
	{
		this.definition = definition;
	}

	public ProviderDefinition definition()
	{
		return definition;
	}

	@Override
	public String id()
	{
		return definition.id();
	}

	@Override
	public String name()
	{
		return definition.name();
	}

	@Override
	public String description()
	{
		return definition.description();
	}

	@Override
	public Set<Metal> metals()
	{
		return definition.metals().keySet();
	}

	@Override
	public boolean needsApiKey()
	{
		return definition.needsApiKey();
	}

	@Override
	public Optional<URI> signupPage()
	{
		return definition.signupPage();
	}

	@Override
	public Optional<String> signupHint()
	{
		return definition.property("signup.hint");
	}

	@Override
	public Optional<URI> termsPage()
	{
		return definition.termsPage();
	}

	@Override
	public Duration minInterval()
	{
		return definition.minInterval();
	}

	@Override
	public int monthlyQuota()
	{
		return definition.monthlyQuota();
	}

	@Override
	public List<LocalTime> defaultSchedule()
	{
		return definition.schedule();
	}

	/** Distinct URLs fetched per refresh; each counts against a quota. */
	@Override
	public int requestsPerFetch()
	{
		Set<String> urls = new LinkedHashSet<>();
		definition.metals().values().forEach(spec -> urls.add(spec.url() == null ? definition.url().orElse("") : spec.url()));
		definition.property("fx.url").ifPresent(urls::add);
		return Math.max(1, urls.size());
	}

	/**
	 * Fills {apikey} into a URL template.
	 * @param template The URL from the definition.
	 * @param context The fetch context holding the key.
	 * @return The URL.
	 * @throws RateException API_KEY if the template needs a key and none is set.
	 */
	protected URI resolve(String template, FetchContext context) throws RateException
	{
		String url = template;
		if(url.contains("{apikey}"))
		{
			String key = context.settings().apiKey().orElseThrow(() -> new RateException(RateException.Kind.API_KEY,
					"Enter your API key for " + name() + " in Settings \u2192 Market rates."));
			url = url.replace("{apikey}", URLEncoder.encode(key, StandardCharsets.UTF_8));
		}
		try
		{
			return URI.create(url);
		}
		catch(IllegalArgumentException e)
		{
			throw new RateException(RateException.Kind.CONFIGURATION, "The address of " + name() + " is not valid: " + e.getMessage(), e);
		}
	}

	/**
	 * Builds a quote from a raw value.
	 * @param metal The metal.
	 * @param value The value read from the response.
	 * @param asOf When the value is valid.
	 * @return The quote, or empty if the value is not a usable price.
	 */
	protected Optional<Quote> quote(Metal metal, double value, Instant asOf)
	{
		ProviderDefinition.MetalSpec spec = definition.metals().get(metal);
		if(!(value > 0) || Double.isInfinite(value)) return Optional.empty();
		double price = spec.invert() ? 1 / value : value;
		return Optional.of(Quote.of(metal, price, spec.per(), spec.unit(), spec.currency(), asOf, id()));
	}

	/**
	 * Parses a number as written on a web page, e.g. "14,430.00" or "1.234,50".
	 * @param text Text containing the number.
	 * @param decimal The decimal separator used by the page.
	 * @param grouping The digit grouping separator used by the page.
	 * @return The number, or empty if the text holds none.
	 */
	static Optional<Double> parseNumber(String text, char decimal, char grouping)
	{
		if(text == null) return Optional.empty();
		Matcher matcher = NUMBER.matcher(text.replace('\u2212', '-'));
		if(!matcher.find()) return Optional.empty();
		StringBuilder plain = new StringBuilder();
		for(char c : matcher.group().trim().toCharArray())
		{
			if(c == decimal) plain.append('.');
			else if(c == grouping || c == '\u00a0' || c == '\u202f' || c == '\'') continue;
			else if(Character.isDigit(c) || c == '-' || c == '+') plain.append(c);
			else return Optional.empty(); // e.g. a "." on a page that uses "," for decimals and "." is not the grouping
		}
		try
		{
			return Optional.of(Double.parseDouble(plain.toString()));
		}
		catch(NumberFormatException e)
		{
			return Optional.empty();
		}
	}

	/**
	 * Parses a date or time from a response. Without a pattern it accepts ISO instants and dates and epoch seconds or
	 * milliseconds; a date without a time means the start of that day in UTC.
	 * @param text The text.
	 * @param pattern A DateTimeFormatter pattern, or null.
	 * @param locale The locale for month names in the pattern.
	 * @return The instant, or empty if it cannot be read.
	 */
	static Optional<Instant> parseInstant(String text, String pattern, Locale locale)
	{
		if(text == null || text.isBlank()) return Optional.empty();
		String value = text.trim();
		try
		{
			if(pattern != null)
			{
				TemporalAccessor parsed = DateTimeFormatter.ofPattern(pattern, locale).parseBest(value, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
				if(parsed instanceof OffsetDateTime odt) return Optional.of(odt.toInstant());
				if(parsed instanceof LocalDateTime ldt) return Optional.of(ldt.toInstant(ZoneOffset.UTC));
				return Optional.of(((LocalDate)parsed).atStartOfDay().toInstant(ZoneOffset.UTC));
			}
			if(value.matches("\\d{9,10}")) return Optional.of(Instant.ofEpochSecond(Long.parseLong(value)));
			if(value.matches("\\d{12,13}")) return Optional.of(Instant.ofEpochMilli(Long.parseLong(value)));
			if(value.matches("\\d{4}-\\d{2}-\\d{2}")) return Optional.of(LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC));
			return Optional.of(OffsetDateTime.parse(value).toInstant());
		}
		catch(DateTimeParseException | ClassCastException | NumberFormatException e)
		{
			return Optional.empty();
		}
	}
}
