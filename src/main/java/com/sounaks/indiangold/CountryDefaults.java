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

import com.sounaks.indiangold.rates.MassUnit;
import com.sounaks.indiangold.rates.Metal;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The usual currency, price units, gold purities and taxes of each country, from country-defaults.csv.
 * Countries not in the file get the "default" row and the currency Java knows for them.
 * @author Sounak Choudhury
 */
final class CountryDefaults
{
	static final String DEFAULT_ROW = "default";

	/**
	 * The unit a group of metals is priced in.
	 * @param unit The unit.
	 * @param quantity The price is for this many units, e.g. 10 for "per 10 g".
	 */
	record GroupUnit(MassUnit unit, double quantity)
	{
	}

	/**
	 * A gold row in the rate bar, derived from the price of pure gold.
	 * @param label The row's name, e.g. "22K".
	 * @param fineness The share of gold, e.g. 0.916.
	 */
	record Purity(String label, double fineness)
	{
		/** Parses "24K:0.999;22K:0.916"; invalid entries are skipped. */
		static List<Purity> parseList(String text)
		{
			List<Purity> purities = new ArrayList<>();
			if(text == null) return purities;
			for(String entry : text.split(";"))
			{
				int colon = entry.lastIndexOf(':');
				if(colon <= 0) continue;
				try
				{
					double fineness = Double.parseDouble(entry.substring(colon + 1).trim());
					String label = entry.substring(0, colon).trim();
					if(fineness > 0 && fineness <= 1 && !label.isEmpty()) purities.add(new Purity(label, fineness));
				}
				catch(NumberFormatException e)
				{
					// skipped
				}
			}
			return purities;
		}

		static String formatList(List<Purity> purities)
		{
			List<String> parts = new ArrayList<>();
			purities.forEach(p -> parts.add(p.label().replace(':', ' ').replace(';', ' ') + ":" + p.fineness()));
			return String.join(";", parts);
		}
	}

	/**
	 * The defaults of one country.
	 * @param country ISO 3166 code.
	 * @param currency ISO 4217 code.
	 * @param units The price unit of each metal group.
	 * @param purities The gold rows.
	 * @param taxes Taxes to set, or empty to keep the user's own.
	 */
	record Defaults(String country, String currency, Map<Metal.Group, GroupUnit> units, List<Purity> purities, List<TaxSettings.Tax> taxes)
	{
	}

	/**
	 * A country to choose from.
	 * @param code ISO 3166 code.
	 * @param name The name in the user's language.
	 */
	record Country(String code, String name)
	{
		@Override
		public String toString()
		{
			return name;
		}
	}

	private final Map<String, String[]> rows = new HashMap<>();

	private CountryDefaults()
	{
	}

	/** Loads country-defaults.csv from the jar. */
	static CountryDefaults load()
	{
		CountryDefaults defaults = new CountryDefaults();
		try(InputStream in = CountryDefaults.class.getResourceAsStream("/country-defaults.csv"))
		{
			if(in == null) throw new IOException("country-defaults.csv is missing");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			boolean header = true;
			for(String line; (line = reader.readLine()) != null;)
			{
				if(line.isBlank() || line.startsWith("#")) continue;
				if(header) { header = false; continue; }
				String[] cells = line.split(",", -1);
				if(cells.length >= 12) defaults.rows.put(cells[0].trim(), cells);
			}
		}
		catch(IOException e)
		{
			System.out.println("Cannot read the country defaults: " + e.getMessage());
		}
		return defaults;
	}

	/** All countries, sorted by name in the user's language. */
	List<Country> countries()
	{
		List<Country> countries = new ArrayList<>();
		for(String code : Locale.getISOCountries()) countries.add(new Country(code, Locale.of("", code).getDisplayCountry()));
		Collator collator = Collator.getInstance();
		countries.sort((a, b) -> collator.compare(a.name(), b.name()));
		return Collections.unmodifiableList(countries);
	}

	/** The user's country according to the system, or the United States if the system does not say. */
	static String systemCountry()
	{
		String country = Locale.getDefault().getCountry();
		return country == null || country.length() != 2 ? "US" : country;
	}

	boolean hasRow(String country)
	{
		return rows.containsKey(country);
	}

	/**
	 * Gets the defaults of a country.
	 * @param country ISO 3166 code.
	 * @return Its row, or the default row with the country's currency.
	 */
	Defaults forCountry(String country)
	{
		String[] cells = rows.getOrDefault(country, rows.get(DEFAULT_ROW));
		if(cells == null) cells = new String[] { DEFAULT_ROW, "", "g", "1", "kg", "1", "g", "1", "kg", "1", "24K:0.999;22K:0.916;18K:0.750", "" };
		String currency = cells[1].trim();
		if(currency.isEmpty())
		{
			try
			{
				Currency javaCurrency = Currency.getInstance(Locale.of("", country));
				currency = javaCurrency == null ? "USD" : javaCurrency.getCurrencyCode();
			}
			catch(IllegalArgumentException e)
			{
				currency = "USD";
			}
		}
		Map<Metal.Group, GroupUnit> units = new EnumMap<>(Metal.Group.class);
		units.put(Metal.Group.GOLD, unit(cells[2], cells[3]));
		units.put(Metal.Group.SILVER, unit(cells[4], cells[5]));
		units.put(Metal.Group.PLATINUM_GROUP, unit(cells[6], cells[7]));
		units.put(Metal.Group.BASE, unit(cells[8], cells[9]));
		List<Purity> purities = Purity.parseList(cells[10]);
		if(purities.isEmpty()) purities = List.of(new Purity("24K", 0.999));
		List<TaxSettings.Tax> taxes = new ArrayList<>();
		for(String entry : cells[11].split(";"))
		{
			int colon = entry.lastIndexOf(':');
			if(colon > 0) taxes.add(new TaxSettings.Tax(entry.substring(0, colon).trim(), entry.substring(colon + 1).trim()));
		}
		return new Defaults(country, CurrencyCatalog.resolve(currency).getCurrencyCode(), units, purities, taxes);
	}

	private static GroupUnit unit(String code, String quantity)
	{
		double qty;
		try
		{
			qty = Double.parseDouble(quantity.trim());
		}
		catch(NumberFormatException e)
		{
			qty = 1;
		}
		return new GroupUnit(MassUnit.fromCode(code).orElse(MassUnit.GRAM), qty > 0 ? qty : 1);
	}
}
