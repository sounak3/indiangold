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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Prices the user types in, for example from a local jeweller's board, with the date they are for. Stored in the
 * provider's settings: date (yyyy-MM-dd), currency, and price.METAL, unit.METAL, per.METAL for each metal entered.
 * @author Sounak Choudhury
 */
public final class ManualProvider implements RateProvider
{
	public static final String ID = "manual";
	public static final String DATE = "date";
	public static final String CURRENCY = "currency";

	@Override
	public String id()
	{
		return ID;
	}

	@Override
	public String name()
	{
		return "Manual entry";
	}

	@Override
	public String description()
	{
		return "Prices you type in yourself, with the date they are for. Works offline.";
	}

	@Override
	public Set<Metal> metals()
	{
		return EnumSet.allOf(Metal.class);
	}

	@Override
	public boolean isManual()
	{
		return true;
	}

	public static String priceKey(Metal metal)
	{
		return "price." + metal.key();
	}

	public static String unitKey(Metal metal)
	{
		return "unit." + metal.key();
	}

	public static String perKey(Metal metal)
	{
		return "per." + metal.key();
	}

	@Override
	public RateSnapshot fetch(FetchContext context) throws RateException
	{
		ProviderSettings settings = context.settings();
		String currency = settings.get(CURRENCY).orElse("USD");
		Instant asOf;
		try
		{
			asOf = settings.get(DATE).map(LocalDate::parse).orElse(LocalDate.now(context.clock()))
					.atStartOfDay(context.clock().getZone() == null ? ZoneId.systemDefault() : context.clock().getZone()).toInstant();
		}
		catch(DateTimeParseException e)
		{
			throw new RateException(RateException.Kind.CONFIGURATION, "The date of the manual prices is not valid.", e);
		}
		Map<Metal, Quote> quotes = new EnumMap<>(Metal.class);
		for(Metal metal : Metal.values())
		{
			Optional<Double> price = number(settings.get(priceKey(metal)));
			if(price.isEmpty() || !(price.get() > 0)) continue;
			MassUnit unit = settings.get(unitKey(metal)).flatMap(MassUnit::fromCode).orElse(MassUnit.GRAM);
			double per = number(settings.get(perKey(metal))).filter(value -> value > 0).orElse(1.0);
			quotes.put(metal, Quote.of(metal, price.get(), per, unit, currency, asOf, ID));
		}
		if(quotes.isEmpty())
			throw new RateException(RateException.Kind.CONFIGURATION, "No manual prices yet; use \"Enter prices\" in Settings \u2192 Market rates.");
		return new RateSnapshot(ID, quotes, Optional.empty(), context.clock().instant());
	}

	private static Optional<Double> number(Optional<String> text)
	{
		try
		{
			return text.map(String::trim).filter(s -> !s.isEmpty()).map(Double::parseDouble).filter(Double::isFinite);
		}
		catch(NumberFormatException e)
		{
			return Optional.empty();
		}
	}
}
