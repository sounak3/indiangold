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
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * What one provider returned from one fetch. It may hold only some metals; missing ones are filled by other sources.
 * @param providerId The provider.
 * @param quotes The prices it returned.
 * @param fx Exchange rates, if the provider supplies them.
 * @param fetchedAt When the fetch happened.
 * @author Sounak Choudhury
 */
public record RateSnapshot(String providerId, Map<Metal, Quote> quotes, Optional<FxRates> fx, Instant fetchedAt)
{
	public RateSnapshot
	{
		quotes = quotes.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(quotes));
	}

	public boolean isEmpty()
	{
		return quotes.isEmpty() && fx.isEmpty();
	}
}
