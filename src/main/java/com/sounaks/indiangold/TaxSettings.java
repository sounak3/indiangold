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

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the $taxes setting ("name|percent|name|percent|name|percent").
 * Empty, missing or invalid entries become zero instead of breaking the price calculation.
 * @author Sounak Choudhury
 */
final class TaxSettings
{
	static final int COUNT = 3;
	static final String DEFAULT = "Tax-1|0.0|Tax-2|0.0|Tax-3|0.0";
	private static final String ZERO = "0.0";

	/**
	 * One tax as shown in the price panel.
	 * @param name The label the user gave it.
	 * @param percent The rate as typed, for example "1.5" or "1.5%".
	 */
	record Tax(String name, String percent)
	{
	}

	private TaxSettings()
	{
	}

	/**
	 * Parses a stored $taxes value.
	 * @param stored The stored value; may be null, short, or contain empty or invalid percentages.
	 * @return Exactly COUNT taxes, with default names and zero percent where an entry is missing or unusable.
	 */
	static List<Tax> parse(String stored)
	{
		String[] parts = stored == null ? new String[0] : stored.split("\\|", -1);
		List<Tax> taxes = new ArrayList<>(COUNT);
		for(int i = 0; i < COUNT; i++)
		{
			String name = 2 * i < parts.length ? parts[2 * i] : "";
			String percent = 2 * i + 1 < parts.length ? parts[2 * i + 1].trim() : "";
			taxes.add(new Tax(name.isBlank() ? "Tax-" + (i + 1) : name, isNumber(percent) ? percent : ZERO));
		}
		return taxes;
	}

	/**
	 * Formats taxes for saving as $taxes. A "|" in a name would split it, so it is replaced with "/".
	 * @param taxes The taxes to save; only the first COUNT are kept.
	 * @return The value to store.
	 */
	static String format(List<Tax> taxes)
	{
		StringBuilder stored = new StringBuilder();
		for(int i = 0; i < COUNT; i++)
		{
			Tax tax = i < taxes.size() ? taxes.get(i) : new Tax("", "");
			String name = tax.name() == null || tax.name().isBlank() ? "Tax-" + (i + 1) : tax.name().replace('|', '/');
			String percent = tax.percent() != null && isNumber(tax.percent().trim()) ? tax.percent().trim() : ZERO;
			if(i > 0) stored.append('|');
			stored.append(name).append('|').append(percent);
		}
		return stored.toString();
	}

	/**
	 * Adds up the tax percentages.
	 * @param taxes The taxes, as returned by parse.
	 * @return The total percentage; unusable entries count as zero.
	 */
	static double totalPercent(List<Tax> taxes)
	{
		double total = 0;
		for(Tax tax : taxes)
		{
			if(isNumber(tax.percent())) total += value(tax.percent());
		}
		return total;
	}

	private static boolean isNumber(String percent)
	{
		if(percent == null || percent.isEmpty()) return false;
		try
		{
			return Double.isFinite(value(percent));
		}
		catch(NumberFormatException e)
		{
			return false;
		}
	}

	private static double value(String percent)
	{
		String number = percent.endsWith("%") ? percent.substring(0, percent.length() - 1) : percent;
		return Double.parseDouble(number.trim());
	}
}
