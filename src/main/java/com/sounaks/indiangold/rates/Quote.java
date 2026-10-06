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
import java.util.Objects;

/**
 * The price of a metal per gram, in the currency the source quoted it in. Prices are converted to the user's
 * currency and unit only when shown, so a currency change needs no new request.
 * @param metal The metal.
 * @param pricePerGram Price of one gram of the (pure) metal.
 * @param currency ISO code of the price's currency.
 * @param asOf When the source says the price is valid.
 * @param providerId The provider that supplied it.
 * @author Sounak Choudhury
 */
public record Quote(Metal metal, double pricePerGram, String currency, Instant asOf, String providerId)
{
	public Quote
	{
		Objects.requireNonNull(metal, "metal");
		Objects.requireNonNull(asOf, "asOf");
		Objects.requireNonNull(providerId, "providerId");
		currency = Objects.requireNonNull(currency, "currency").trim().toUpperCase(Locale.ROOT);
		if(!(pricePerGram > 0) || Double.isInfinite(pricePerGram))
			throw new IllegalArgumentException("Price of " + metal.key() + " must be a positive number, not " + pricePerGram);
	}

	/**
	 * Creates a quote from a price for any quantity and unit.
	 * @param metal The metal.
	 * @param price The quoted price.
	 * @param perQuantity How many units the price is for, e.g. 10 for "per 10 g".
	 * @param unit The unit of the quantity.
	 * @param currency ISO code of the price's currency.
	 * @param asOf When the price is valid.
	 * @param providerId The provider that supplied it.
	 * @return The quote.
	 */
	public static Quote of(Metal metal, double price, double perQuantity, MassUnit unit, String currency, Instant asOf, String providerId)
	{
		return new Quote(metal, price / (perQuantity * unit.grams()), currency, asOf, providerId);
	}

	/**
	 * Converts this price to a currency and quantity.
	 * @param fx Exchange rates.
	 * @param targetCurrency ISO code of the wanted currency.
	 * @param grams The quantity in grams, e.g. 10 for a price per 10 g.
	 * @param fineness Purity to apply, 1 for the metal itself (e.g. 0.916 for 22K gold).
	 * @return The price, or NaN if an exchange rate is missing.
	 */
	public double priceIn(FxRates fx, String targetCurrency, double grams, double fineness)
	{
		return fx.convert(pricePerGram * grams * fineness, currency, targetCurrency);
	}
}
