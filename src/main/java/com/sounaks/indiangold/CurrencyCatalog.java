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

import java.io.IOException;
import java.io.InputStream;
import java.util.Currency;
import java.util.Locale;
import java.util.Properties;

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
}
