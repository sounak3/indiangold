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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A provider that reads prices from JSON APIs, as described by a {@link ProviderDefinition} of type json-api.
 * @author Sounak Choudhury
 */
final class JsonApiProvider extends DefinitionProvider
{
	JsonApiProvider(ProviderDefinition definition)
	{
		super(definition);
	}

	@Override
	public boolean providesFx()
	{
		return definition.property("fx.path").isPresent();
	}

	@Override
	public RateSnapshot fetch(FetchContext context) throws RateException
	{
		// Fetch every distinct URL once; metals may share one URL (Metals.Dev) or have one each (Gold-API).
		Map<String, List<Metal>> metalsByUrl = new LinkedHashMap<>();
		definition.metals().forEach((metal, spec) ->
				metalsByUrl.computeIfAbsent(spec.url() == null ? definition.url().orElseThrow() : spec.url(), k -> new java.util.ArrayList<>()).add(metal));
		Map<String, JsonElement> responses = new HashMap<>();
		Map<Metal, Quote> quotes = new EnumMap<>(Metal.class);
		RateException firstFailure = null;
		for(Map.Entry<String, List<Metal>> entry : metalsByUrl.entrySet())
		{
			JsonElement json;
			try
			{
				json = load(entry.getKey(), context, responses);
			}
			catch(RateException e)
			{
				// A wrong key or used-up quota affects every URL; anything else may affect just this one.
				if(e.kind() == RateException.Kind.API_KEY || e.kind() == RateException.Kind.QUOTA) throw e;
				if(firstFailure == null) firstFailure = e;
				continue;
			}
			Instant asOf = asOf(json, context);
			for(Metal metal : entry.getValue())
			{
				numberAt(json, definition.metals().get(metal).locator()).flatMap(value -> quote(metal, value, asOf))
						.ifPresent(quote -> quotes.put(metal, quote));
			}
		}
		Optional<FxRates> fx = Optional.empty();
		if(definition.property("fx.path").isPresent())
		{
			try
			{
				fx = fx(load(definition.property("fx.url").orElse(definition.url().orElseThrow()), context, responses), context);
			}
			catch(RateException e)
			{
				if(firstFailure == null) firstFailure = e;
			}
		}
		if(quotes.isEmpty() && fx.isEmpty())
		{
			if(firstFailure != null) throw firstFailure;
			throw new RateException(RateException.Kind.UNEXPECTED_RESPONSE, name() + " answered without any of the expected prices; its API may have changed.");
		}
		return new RateSnapshot(id(), quotes, fx, context.clock().instant());
	}

	@Override
	public Optional<Usage> usage(FetchContext context) throws RateException
	{
		Optional<String> url = definition.property("usage.url");
		if(url.isEmpty()) return Optional.empty();
		JsonElement json = load(url.get(), context, new HashMap<>());
		Optional<Double> total = numberAt(json, definition.property("usage.total.path").orElse("/total"));
		Optional<Double> used = numberAt(json, definition.property("usage.used.path").orElse("/used"));
		if(total.isEmpty() || used.isEmpty()) throw new RateException(RateException.Kind.UNEXPECTED_RESPONSE, name() + " did not report its usage.");
		String plan = textAt(json, definition.property("usage.plan.path").orElse("/plan")).orElse("");
		return Optional.of(new Usage(plan, total.get().intValue(), used.get().intValue()));
	}

	/** Fetches and parses a URL once per fetch, trying url.fallback if the main URL cannot be reached. */
	private JsonElement load(String template, FetchContext context, Map<String, JsonElement> cache) throws RateException
	{
		JsonElement cached = cache.get(template);
		if(cached != null) return cached;
		Optional<String> fallback = template.equals(definition.url().orElse(null)) ? definition.fallbackUrl() : Optional.empty();
		HttpFetcher.Response response;
		try
		{
			response = context.http().get(resolve(template, context));
			// A server error on the main URL (e.g. a CDN outage) is a reason to try the fallback too.
			if(response.status() >= 500 && fallback.isPresent()) response = context.http().get(resolve(fallback.get(), context));
		}
		catch(RateException e)
		{
			if(e.kind() != RateException.Kind.NETWORK || fallback.isEmpty()) throw e;
			response = context.http().get(resolve(fallback.get(), context));
		}
		JsonElement json = null;
		try
		{
			json = JsonParser.parseString(response.body());
		}
		catch(JsonParseException e)
		{
			// handled below: an error status explains more than a parse error
		}
		if(json != null) checkReportedError(json);
		if(!response.isOk()) throw HttpFetcher.statusError(resolve(template, context), response.status());
		if(json == null) throw new RateException(RateException.Kind.UNEXPECTED_RESPONSE, name() + " did not answer with JSON; its API may have changed.");
		cache.put(template, json);
		return json;
	}

	/** Throws the API's own error message, e.g. Metals.Dev's {"status":"failure","error_code":1101,...}. */
	private void checkReportedError(JsonElement json) throws RateException
	{
		Optional<String> message = definition.property("error.path").flatMap(path -> textAt(json, path));
		if(message.isEmpty()) return;
		String code = definition.property("error.code.path").flatMap(path -> textAt(json, path)).orElse("");
		RateException.Kind kind;
		try
		{
			kind = RateException.Kind.valueOf(definition.property("error.kind." + code.replaceAll("\\.0$", "")).orElse("UNEXPECTED_RESPONSE").toUpperCase(Locale.ROOT));
		}
		catch(IllegalArgumentException e)
		{
			kind = RateException.Kind.UNEXPECTED_RESPONSE;
		}
		throw new RateException(kind, name() + ": " + message.get());
	}

	private Instant asOf(JsonElement json, FetchContext context)
	{
		return definition.property("asOf.path").flatMap(path -> textAt(json, path))
				.flatMap(text -> parseInstant(text, definition.property("asOf.format").orElse(null),
						Locale.forLanguageTag(definition.property("asOf.locale").orElse("en"))))
				.orElse(context.clock().instant());
	}

	private Optional<FxRates> fx(JsonElement json, FetchContext context)
	{
		Optional<JsonElement> node = JsonPointer.find(json, definition.property("fx.path").orElseThrow());
		if(node.isEmpty() || !node.get().isJsonObject()) return Optional.empty();
		boolean usdPerUnit = definition.property("fx.direction").orElse("perUsd").equals("usdPerUnit");
		Map<String, Double> perUsd = new HashMap<>();
		for(Map.Entry<String, JsonElement> entry : ((JsonObject)node.get()).entrySet())
		{
			numberOf(entry.getValue()).filter(rate -> rate > 0)
					.ifPresent(rate -> perUsd.put(entry.getKey().toUpperCase(Locale.ROOT), usdPerUnit ? 1 / rate : rate));
		}
		// Rates are against the provider's base currency; it must be USD (Metals.Dev, Currency-API with usd.json).
		return perUsd.size() < 2 ? Optional.empty() : Optional.of(new FxRates(perUsd, asOf(json, context), id()));
	}

	static Optional<Double> numberAt(JsonElement json, String pointer)
	{
		return JsonPointer.find(json, pointer).flatMap(JsonApiProvider::numberOf);
	}

	private static Optional<Double> numberOf(JsonElement element)
	{
		if(element == null || !element.isJsonPrimitive()) return Optional.empty();
		try
		{
			double value = element.getAsJsonPrimitive().isNumber() ? element.getAsDouble() : Double.parseDouble(element.getAsString().trim());
			return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
		}
		catch(NumberFormatException e)
		{
			return Optional.empty();
		}
	}

	static Optional<String> textAt(JsonElement json, String pointer)
	{
		return JsonPointer.find(json, pointer).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString).filter(s -> !s.isBlank());
	}
}
