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
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Selector;

/**
 * A provider that reads prices from a web page with CSS selectors, as described by a {@link ProviderDefinition} of
 * type web-page. The page is fetched at most once an hour, only if robots.txt allows it, and the app identifies itself
 * honestly; whether the site's terms allow this is up to the user, who is shown a disclaimer.
 * @author Sounak Choudhury
 */
final class WebPageProvider extends DefinitionProvider
{
	WebPageProvider(ProviderDefinition definition)
	{
		super(definition);
	}

	@Override
	public boolean isWebPage()
	{
		return true;
	}

	@Override
	public RateSnapshot fetch(FetchContext context) throws RateException
	{
		URI page = resolve(definition.url().orElseThrow(), context);
		Document document = Jsoup.parse(context.http().getPage(page), page.toString());
		return read(document, context.clock().instant());
	}

	/**
	 * Reads the prices from a parsed page; used by fetch and by the definition editor's Test button.
	 * @param document The page.
	 * @param now The time to use when the page has no date.
	 * @return The prices found.
	 * @throws RateException If none of the metals could be found or a selector is invalid.
	 */
	RateSnapshot read(Document document, Instant now) throws RateException
	{
		char decimal = definition.property("number.decimal").map(s -> s.charAt(0)).orElse('.');
		char grouping = definition.property("number.grouping").map(s -> s.charAt(0)).orElse(decimal == '.' ? ',' : '.');
		Instant asOf = now;
		Optional<String> asOfSelector = definition.property("asOf.select");
		if(asOfSelector.isPresent())
		{
			asOf = text(document, asOfSelector.get()).flatMap(text -> parseInstant(text, definition.property("asOf.format").orElse(null),
					Locale.forLanguageTag(definition.property("asOf.locale").orElse("en")))).orElse(now);
		}
		Map<Metal, Quote> quotes = new EnumMap<>(Metal.class);
		for(Map.Entry<Metal, ProviderDefinition.MetalSpec> entry : definition.metals().entrySet())
		{
			Instant validAt = asOf;
			text(document, entry.getValue().locator()).flatMap(text -> parseNumber(text, decimal, grouping))
					.flatMap(value -> quote(entry.getKey(), value, validAt)).ifPresent(quote -> quotes.put(entry.getKey(), quote));
		}
		if(quotes.isEmpty())
			throw new RateException(RateException.Kind.UNEXPECTED_RESPONSE, "None of the prices were found on " + name() + "'s page; the page may have changed.");
		return new RateSnapshot(id(), quotes, Optional.empty(), now);
	}

	private Optional<String> text(Document document, String selector) throws RateException
	{
		try
		{
			Element element = document.selectFirst(selector);
			return element == null ? Optional.empty() : Optional.of(element.text());
		}
		catch(Selector.SelectorParseException e)
		{
			throw new RateException(RateException.Kind.CONFIGURATION, "The selector \"" + selector + "\" of " + name() + " is not valid: " + e.getMessage(), e);
		}
	}
}
