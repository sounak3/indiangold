package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TaxSettingsTest
{
	@Test
	void parsesThreeNamedTaxes()
	{
		List<TaxSettings.Tax> taxes = TaxSettings.parse("SGST |1.5|CGST |1.5|Tax-3|0.0");

		assertEquals(3, taxes.size());
		assertEquals("SGST ", taxes.get(0).name());
		assertEquals("1.5", taxes.get(1).percent());
		assertEquals(3.0, TaxSettings.totalPercent(taxes), 1e-9);
	}

	@Test
	void emptyLastPercentIsTreatedAsZero()
	{
		// An empty third tax field used to be saved as "...|Tax-3|", which stopped the app from starting.
		List<TaxSettings.Tax> taxes = TaxSettings.parse("SGST |1.5|CGST |1.5|Tax-3|");

		assertEquals(3, taxes.size());
		assertEquals("0.0", taxes.get(2).percent());
		assertEquals(3.0, TaxSettings.totalPercent(taxes), 1e-9);
	}

	@Test
	void emptyOrInvalidPercentInTheMiddleIsTreatedAsZero()
	{
		List<TaxSettings.Tax> taxes = TaxSettings.parse("A||B|abc|C|2%");

		assertEquals(2.0, TaxSettings.totalPercent(taxes), 1e-9);
	}

	@Test
	void missingEntriesGetDefaultNames()
	{
		List<TaxSettings.Tax> taxes = TaxSettings.parse("GST|3");

		assertEquals(List.of("GST", "Tax-2", "Tax-3"), taxes.stream().map(TaxSettings.Tax::name).toList());
		assertEquals(3.0, TaxSettings.totalPercent(taxes), 1e-9);
		assertEquals(3, TaxSettings.parse(null).size());
	}

	@Test
	void extraEntriesAreIgnored()
	{
		assertEquals(3, TaxSettings.parse("A|1|B|1|C|1|D|1").size());
	}

	@Test
	void formatRoundTripsAndKeepsSeparatorOutOfNames()
	{
		List<TaxSettings.Tax> taxes = List.of(new TaxSettings.Tax("VAT|state", "5"), new TaxSettings.Tax("", ""), new TaxSettings.Tax("Cess", "1"));

		String stored = TaxSettings.format(taxes);

		assertEquals("VAT/state|5|Tax-2|0.0|Cess|1", stored);
		assertEquals(TaxSettings.parse(stored), TaxSettings.parse(TaxSettings.format(TaxSettings.parse(stored))));
	}
}
