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

/**
 * A fetch that failed, with a message meant for the user (shown in the rate bar and in the Test result).
 * @author Sounak Choudhury
 */
public class RateException extends Exception
{
	private static final long serialVersionUID = 1L;

	/** Why a fetch failed; the settings window uses it to point at the right fix. */
	public enum Kind
	{
		/** No connection, a timeout or a server error. Trying again later may work. */
		NETWORK,
		/** The API key is missing, invalid or disabled. */
		API_KEY,
		/** The provider's request quota is used up. */
		QUOTA,
		/** The site's robots.txt does not allow fetching the page. */
		NOT_ALLOWED,
		/** The response did not contain the expected prices; the page or API probably changed. */
		UNEXPECTED_RESPONSE,
		/** The provider definition is incomplete or wrong. */
		CONFIGURATION
	}

	private final Kind kind;

	public RateException(Kind kind, String message)
	{
		super(message);
		this.kind = kind;
	}

	public RateException(Kind kind, String message, Throwable cause)
	{
		super(message, cause);
		this.kind = kind;
	}

	public Kind kind()
	{
		return kind;
	}
}
