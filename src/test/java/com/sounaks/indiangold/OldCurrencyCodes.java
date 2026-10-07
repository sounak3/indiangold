package com.sounaks.indiangold;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** The currency codes offered before 5.0, kept so tests can check that old settings still work. */
final class OldCurrencyCodes
{
	private OldCurrencyCodes()
	{
	}

	static List<String> list()
	{
		try(InputStream in = OldCurrencyCodes.class.getResourceAsStream("/old-currency-codes.txt"))
		{
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^#.*$", "").trim();
			return Arrays.asList(text.split("\\s+"));
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
}
