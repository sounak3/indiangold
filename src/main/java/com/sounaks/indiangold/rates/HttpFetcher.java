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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches URLs for rate providers. It always identifies the app honestly in the User-Agent header (it never poses as a
 * browser), and for web pages it first checks the site's robots.txt.
 * @author Sounak Choudhury
 */
public class HttpFetcher
{
	private static final Duration ROBOTS_CACHE_TIME = Duration.ofHours(24);

	private final HttpClient client;
	private final String userAgent;
	private final Duration timeout;
	private final Clock clock;
	private final Map<String, CachedRobots> robotsByHost = new ConcurrentHashMap<>();

	/**
	 * A response with its status code; JSON APIs often explain errors in the body.
	 * @param status The HTTP status code.
	 * @param body The response body.
	 */
	public record Response(int status, String body)
	{
		public boolean isOk()
		{
			return status >= 200 && status < 300;
		}
	}

	private record CachedRobots(RobotsTxt rules, Instant fetchedAt)
	{
	}

	/**
	 * @param userAgent The User-Agent header, e.g. "IndianGold/5.0 (+https://github.com/sounak3/indiangold)".
	 * @param timeout How long to wait for a connection and for each response.
	 * @param clock The clock used to expire cached robots.txt files.
	 */
	public HttpFetcher(String userAgent, Duration timeout, Clock clock)
	{
		this.userAgent = userAgent;
		this.timeout = timeout;
		this.clock = clock;
		this.client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
	}

	public String userAgent()
	{
		return userAgent;
	}

	/**
	 * Fetches a URL and returns the response whatever its status.
	 * @param uri The URL.
	 * @return The status and body.
	 * @throws RateException NETWORK if the server cannot be reached or does not answer in time.
	 */
	public Response get(URI uri) throws RateException
	{
		HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).header("User-Agent", userAgent)
				.header("Accept", "application/json, text/html;q=0.9, */*;q=0.8").GET().build();
		try
		{
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return new Response(response.statusCode(), response.body());
		}
		catch(HttpTimeoutException e)
		{
			throw new RateException(RateException.Kind.NETWORK, uri.getHost() + " did not answer in time.", e);
		}
		catch(IOException e)
		{
			throw new RateException(RateException.Kind.NETWORK, "Cannot reach " + uri.getHost() + " (" + e.getClass().getSimpleName()
					+ (e.getMessage() == null ? "" : ": " + e.getMessage()) + ").", e);
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new RateException(RateException.Kind.NETWORK, "Fetching " + uri.getHost() + " was interrupted.", e);
		}
	}

	/**
	 * Fetches a URL and fails unless the server answers with success.
	 * @param uri The URL.
	 * @return The body.
	 * @throws RateException With a message explaining the status, e.g. that the key was refused.
	 */
	public String getOk(URI uri) throws RateException
	{
		Response response = get(uri);
		if(!response.isOk()) throw statusError(uri, response.status());
		return response.body();
	}

	/**
	 * Fetches a web page if the site's robots.txt allows it.
	 * @param uri The page.
	 * @return The page's HTML.
	 * @throws RateException NOT_ALLOWED if robots.txt disallows the page, otherwise as {@link #getOk(URI)}.
	 */
	public String getPage(URI uri) throws RateException
	{
		String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
		if(!robotsFor(uri).allows(path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())))
		{
			throw new RateException(RateException.Kind.NOT_ALLOWED, uri.getHost() + " does not allow automated access to "
					+ uri.getPath() + " (robots.txt).");
		}
		return getOk(uri);
	}

	private RobotsTxt robotsFor(URI page) throws RateException
	{
		String host = page.getScheme() + "://" + page.getAuthority();
		CachedRobots cached = robotsByHost.get(host);
		if(cached != null && cached.fetchedAt().plus(ROBOTS_CACHE_TIME).isAfter(clock.instant())) return cached.rules();
		Response response = get(URI.create(host + "/robots.txt"));
		RobotsTxt rules;
		if(response.isOk()) rules = RobotsTxt.parse(response.body(), userAgent);
		else if(response.status() >= 500) throw new RateException(RateException.Kind.NETWORK, page.getHost() + " is not available (HTTP " + response.status() + ").");
		else rules = RobotsTxt.allowAll(); // no robots.txt (404 etc.) means no restrictions
		robotsByHost.put(host, new CachedRobots(rules, clock.instant()));
		return rules;
	}

	static RateException statusError(URI uri, int status)
	{
		String host = uri.getHost();
		return switch(status)
		{
			case 401 -> new RateException(RateException.Kind.API_KEY, host + " did not accept the API key (HTTP 401).");
			case 402 -> new RateException(RateException.Kind.QUOTA, host + " says the plan's limit is reached (HTTP 402).");
			case 403 -> new RateException(RateException.Kind.NOT_ALLOWED, host + " refused the request (HTTP 403).");
			case 404 -> new RateException(RateException.Kind.CONFIGURATION, host + " has no such page (HTTP 404); the address may have changed.");
			case 429 -> new RateException(RateException.Kind.QUOTA, host + " says there were too many requests (HTTP 429).");
			default -> new RateException(RateException.Kind.NETWORK, host + " answered with HTTP " + status + ".");
		};
	}
}
