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

import java.util.Locale;
import java.util.Optional;

/**
 * Units that rate sources quote prices in. Definition files refer to them by code, e.g. "toz" or "t".
 * @author Sounak Choudhury
 */
public enum MassUnit
{
	MILLIGRAM("mg", 0.001),
	GRAM("g", 1),
	KILOGRAM("kg", 1_000),
	TONNE("t", 1_000_000),
	TROY_OUNCE("toz", 31.1034768),
	POUND("lb", 453.59237),
	OUNCE("oz", 28.349523125),
	TOLA("tola", 11.6638038),
	TAEL_HONG_KONG("tael_hk", 37.429),
	TAEL("tael", 37.5),
	BAHT("baht", 15.244);

	private final String code;
	private final double grams;

	MassUnit(String code, double grams)
	{
		this.code = code;
		this.grams = grams;
	}

	public String code()
	{
		return code;
	}

	/**
	 * Gets the weight of one unit.
	 * @return The weight in grams.
	 */
	public double grams()
	{
		return grams;
	}

	/**
	 * Finds a unit by code. Also accepts "mt" (metric tonne, used by Metals.Dev) and "ozt".
	 * @param code The unit code, in any case.
	 * @return The unit, or empty if the code is unknown.
	 */
	public static Optional<MassUnit> fromCode(String code)
	{
		if(code == null) return Optional.empty();
		String normalized = code.trim().toLowerCase(Locale.ROOT);
		if(normalized.equals("mt")) normalized = "t";
		if(normalized.equals("ozt") || normalized.equals("oz t")) normalized = "toz";
		for(MassUnit unit : values())
		{
			if(unit.code.equals(normalized)) return Optional.of(unit);
		}
		return Optional.empty();
	}
}
