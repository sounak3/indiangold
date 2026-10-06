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

import java.time.Clock;

/**
 * What a provider gets for a fetch: its settings, the shared HTTP client and the clock.
 * @param settings The user's settings for this provider, e.g. its API key.
 * @param http The HTTP client; it sends the app's user agent and obeys robots.txt for web pages.
 * @param clock The clock to use for timestamps.
 * @author Sounak Choudhury
 */
public record FetchContext(ProviderSettings settings, HttpFetcher http, Clock clock)
{
}
