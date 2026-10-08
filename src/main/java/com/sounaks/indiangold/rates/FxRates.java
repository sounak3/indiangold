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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exchange rates against the US dollar, kept for all currencies so a currency change needs no new request.
 * @param perUsd Units of each currency (upper-case ISO code) that one US dollar buys.
 * @param asOf When the rates are valid.
 * @param providerId The provider that supplied them.
 * @author Sounak Choudhury
 */
public record FxRates(Map<String, Double> perUsd, Instant asOf, String providerId)
{
	public FxRates
	{
		Map<String, Double> clean = new TreeMap<>();
		perUsd.forEach((code, rate) -> {
			if(code != null && rate != null && rate > 0 && !rate.isInfinite()) clean.put(code.trim().toUpperCase(Locale.ROOT), rate);
		});
		clean.put("USD", 1.0);
		perUsd = Map.copyOf(clean);
	}

	/** Only US dollars, for when no exchange rates have been fetched yet. */
	public static FxRates usdOnly()
	{
		return new FxRates(Map.of(), Instant.EPOCH, "none");
	}

	public boolean has(String currency)
	{
		return currency != null && perUsd.containsKey(currency.toUpperCase(Locale.ROOT));
	}

	/**
	 * Converts an amount between two currencies through the US dollar.
	 * @param amount The amount.
	 * @param from The amount's currency.
	 * @param to The wanted currency.
	 * @return The converted amount, or NaN if either rate is unknown.
	 */
	public double convert(double amount, String from, String to)
	{
		if(from != null && from.equalsIgnoreCase(to)) return amount;
		return fromUsd(amount * usdPerUnit(from), to);
	}

	/**
	 * Converts an amount from US dollars.
	 * @param usd The amount in US dollars.
	 * @param currency The ISO code to convert to.
	 * @return The amount in that currency, or NaN if its rate is unknown.
	 */
	public double fromUsd(double usd, String currency)
	{
		Double rate = currency == null ? null : perUsd.get(currency.toUpperCase(Locale.ROOT));
		return rate == null ? Double.NaN : usd * rate;
	}

	/**
	 * Gets the value of one unit of a currency in US dollars.
	 * @param currency The ISO code.
	 * @return The value, or NaN if its rate is unknown.
	 */
	public double usdPerUnit(String currency)
	{
		Double rate = currency == null ? null : perUsd.get(currency.toUpperCase(Locale.ROOT));
		return rate == null ? Double.NaN : 1 / rate;
	}
}
