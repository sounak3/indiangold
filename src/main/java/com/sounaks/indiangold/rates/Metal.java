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
 * The metals the rate bar can show. Gold rows for other purities are derived from GOLD (pure gold).
 * @author Sounak Choudhury
 */
public enum Metal
{
	GOLD(Group.GOLD, "Gold"),
	SILVER(Group.SILVER, "Silver"),
	PLATINUM(Group.PLATINUM_GROUP, "Platinum"),
	PALLADIUM(Group.PLATINUM_GROUP, "Palladium"),
	COPPER(Group.BASE, "Copper"),
	ALUMINUM(Group.BASE, "Aluminium"),
	NICKEL(Group.BASE, "Nickel"),
	ZINC(Group.BASE, "Zinc"),
	LEAD(Group.BASE, "Lead"),
	TIN(Group.BASE, "Tin");

	/** Metals quoted in the same unit within a country, e.g. India quotes gold per 10 g but silver per kg. */
	public enum Group
	{
		GOLD, SILVER, PLATINUM_GROUP, BASE
	}

	private final Group group;
	private final String displayName;

	Metal(Group group, String displayName)
	{
		this.group = group;
		this.displayName = displayName;
	}

	public Group group()
	{
		return group;
	}

	public boolean isPrecious()
	{
		return group != Group.BASE;
	}

	public String displayName()
	{
		return displayName;
	}

	/**
	 * Gets the key used in definition files and settings.
	 * @return The lower-case name, e.g. "gold".
	 */
	public String key()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Finds a metal by its key; "aluminium" is accepted as well as "aluminum".
	 * @param key The key, in any case.
	 * @return The metal, or empty if the key is unknown.
	 */
	public static Optional<Metal> fromKey(String key)
	{
		if(key == null) return Optional.empty();
		String normalized = key.trim().toLowerCase(Locale.ROOT);
		if(normalized.equals("aluminium")) normalized = "aluminum";
		for(Metal metal : values())
		{
			if(metal.key().equals(normalized)) return Optional.of(metal);
		}
		return Optional.empty();
	}
}
