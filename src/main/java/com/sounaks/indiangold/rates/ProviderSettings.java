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

import java.util.Optional;

/**
 * The settings the user made for one provider, such as its API key. Saved with the app's other settings.
 * @author Sounak Choudhury
 */
public interface ProviderSettings
{
	/** Setting name of the API key. */
	String API_KEY = "apikey";

	Optional<String> get(String name);

	void put(String name, String value);

	default Optional<String> apiKey()
	{
		return get(API_KEY).map(String::trim).filter(key -> !key.isEmpty());
	}
}
