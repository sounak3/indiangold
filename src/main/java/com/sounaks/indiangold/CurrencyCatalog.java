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
package com.sounaks.indiangold;

import com.sounaks.indiangold.rates.FxRates;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns saved currency codes into usable currencies. Codes from older versions that are obsolete or not ISO 4217
 * are mapped to their successors (currency-migrations.properties), so a saved setting can never stop the app.
 * @author Sounak Choudhury
 */
final class CurrencyCatalog
{
	static final Currency FALLBACK = Currency.getInstance("USD");
	private static final Properties MIGRATIONS = loadMigrations();

	private CurrencyCatalog()
	{
	}

	private static Properties loadMigrations()
	{
		Properties migrations = new Properties();
		try(InputStream in = CurrencyCatalog.class.getResourceAsStream("/currency-migrations.properties"))
		{
			if(in != null) migrations.load(in);
		}
		catch(IOException e)
		{
			System.out.println("Cannot read currency migrations: " + e);
		}
		return migrations;
	}

	/**
	 * Gets the code to use instead of a saved one.
	 * @param code A saved currency code.
	 * @return The successor code for an obsolete or non-ISO code, otherwise the code itself.
	 */
	static String migrate(String code)
	{
		if(code == null) return null;
		return MIGRATIONS.getProperty(code.trim().toUpperCase(Locale.ROOT), code.trim());
	}

	/**
	 * Gets the currency for a saved code, never failing.
	 * @param code A saved currency code, possibly obsolete, invalid or null.
	 * @return Its successor if it has one, the currency itself if Java knows it, otherwise US dollars.
	 */
	static Currency resolve(String code)
	{
		if(code == null || code.isBlank()) return FALLBACK;
		String original = code.trim().toUpperCase(Locale.ROOT);
		// A newer successor (e.g. XCG) may be unknown to an older Java runtime; then the original code still works.
		for(String candidate : new String[] { migrate(original), original })
		{
			try
			{
				return Currency.getInstance(candidate);
			}
			catch(IllegalArgumentException e)
			{
				// try the next candidate
			}
		}
		System.out.println("Unknown currency " + code + "; using " + FALLBACK.getCurrencyCode() + ".");
		return FALLBACK;
	}

	/**
	 * A currency to choose from, shown as e.g. "Indian Rupee (INR) \u20b9".
	 * @param currency The currency.
	 */
	record Choice(Currency currency)
	{
		@Override
		public String toString()
		{
			String symbol = currency.getSymbol();
			return currency.getDisplayName() + " (" + currency.getCurrencyCode() + ")" + (symbol.equals(currency.getCurrencyCode()) ? "" : " " + symbol);
		}
	}

	/**
	 * Lists the currencies the user can choose: those the exchange rates cover (all current currencies Java knows
	 * if there are no rates yet), without obsolete codes, funds and metals, sorted by name.
	 * @param fx The exchange rates fetched so far.
	 * @param current The current currency, always included.
	 * @return The choices.
	 */
	static List<Choice> choices(FxRates fx, String current)
	{
		Set<String> codes = new TreeSet<>();
		if(fx.perUsd().size() > 2) codes.addAll(fx.perUsd().keySet());
		else
		{
			// No rates yet: the currencies countries use today (Java also knows long-gone ones such as DEM).
			for(String country : Locale.getISOCountries())
			{
				Currency currency = Currency.getInstance(Locale.of("", country));
				if(currency != null) codes.add(currency.getCurrencyCode());
			}
		}
		codes.add(resolve(current).getCurrencyCode());
		List<Choice> choices = new ArrayList<>();
		for(String code : codes)
		{
			if(MIGRATIONS.containsKey(code) && !code.equals(current)) continue; // obsolete or not ISO
			if(code.startsWith("X") && !Set.of("XAF", "XCD", "XCG", "XOF", "XPF").contains(code)) continue; // metals, funds, test codes
			try
			{
				Currency currency = Currency.getInstance(code);
				if(currency.getDefaultFractionDigits() >= 0) choices.add(new Choice(currency));
			}
			catch(IllegalArgumentException e)
			{
				// crypto and other codes Java does not know
			}
		}
		Collator collator = Collator.getInstance();
		choices.sort((a, b) -> collator.compare(a.toString(), b.toString()));
		return choices;
	}
}
