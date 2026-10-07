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

import com.sounaks.indiangold.rates.MassUnit;
import com.sounaks.indiangold.rates.Metal;
import com.sounaks.indiangold.rates.ProviderSettings;
import com.sounaks.indiangold.rates.RateService;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The market-rate settings, kept in units.dat with the app's other settings: country, currency, the unit each metal
 * group is shown in, gold purities, the rate sources and their own settings (such as API keys).
 * @author Sounak Choudhury
 */
final class MarketSettings implements RateService.Settings
{
	static final String DEFAULT_SOURCES = "gold-api,westmetall,currency-api,metals-dev:off,manual:off";
	private static final Map<MassUnit, String> UNIT_NAMES = new EnumMap<>(MassUnit.class);

	static
	{
		// Names as in the bundled unit list, so existing units are reused rather than duplicated.
		UNIT_NAMES.put(MassUnit.MILLIGRAM, "miligram (mg)");
		UNIT_NAMES.put(MassUnit.GRAM, "gram (g)");
		UNIT_NAMES.put(MassUnit.KILOGRAM, "kilogram (kg)");
		UNIT_NAMES.put(MassUnit.TONNE, "tonne (t)");
		UNIT_NAMES.put(MassUnit.TROY_OUNCE, "troy ounce (oz t)");
		UNIT_NAMES.put(MassUnit.POUND, "pound (lb)");
		UNIT_NAMES.put(MassUnit.OUNCE, "ounce (oz)");
		UNIT_NAMES.put(MassUnit.TOLA, "tola (bhori / standard tola)");
		UNIT_NAMES.put(MassUnit.TAEL_HONG_KONG, "tael (hong kong, 37.429 g)");
		UNIT_NAMES.put(MassUnit.TAEL, "tael / luong (37.5 g)");
		UNIT_NAMES.put(MassUnit.BAHT, "baht (15.244 g)");
	}

	/**
	 * The unit a metal group is shown in.
	 * @param name The unit's name in the unit list, e.g. "gram (g)".
	 * @param quantity Prices are for this many units, e.g. 10.
	 * @param grams The weight of one unit in grams.
	 */
	record DisplayUnit(String name, double quantity, double grams)
	{
		/** The weight the shown price is for. */
		double totalGrams()
		{
			return quantity * grams;
		}

		/** The short form in the unit's name, e.g. "g" from "gram (g)". */
		String shortName()
		{
			int open = name.indexOf('('), close = name.indexOf(')');
			return open >= 0 && close > open ? name.substring(open + 1, close) : name;
		}

		/** For example "10 g" or "oz t". */
		String label()
		{
			String qty = quantity == Math.rint(quantity) ? String.valueOf((long)quantity) : String.valueOf(quantity);
			return (quantity == 1 ? "" : qty + " ") + shortName();
		}
	}

	private final FileOperations ops;

	MarketSettings(FileOperations ops)
	{
		this.ops = ops;
		migrateUnitSettings();
	}

	static String unitName(MassUnit unit)
	{
		return UNIT_NAMES.get(unit);
	}

	private static String key(Metal.Group group)
	{
		return switch(group)
		{
			case GOLD -> "gold";
			case SILVER -> "silver";
			case PLATINUM_GROUP -> "pgm";
			case BASE -> "base";
		};
	}

	/** Versions before 5.0 had one unit for all precious metals ($punit) and one for base metals ($bunit). */
	private void migrateUnitSettings()
	{
		if(!ops.getValue("$unit.gold", "").isEmpty() || ops.getValue("$punit", "").isEmpty()) return;
		for(Metal.Group group : new Metal.Group[] { Metal.Group.GOLD, Metal.Group.SILVER, Metal.Group.PLATINUM_GROUP })
		{
			ops.setValue("$unit." + key(group), ops.getValue("$punit", ""));
			ops.setValue("$qty." + key(group), ops.getValue("$punitspercurrency", "1"));
		}
		ops.setValue("$unit.base", ops.getValue("$bunit", "pound (lb)"));
		ops.setValue("$qty.base", ops.getValue("$bunitspercurrency", "1"));
	}

	Optional<String> country()
	{
		return Optional.of(ops.getValue("$country", "")).filter(c -> !c.isEmpty()).map(c -> c.toUpperCase(Locale.ROOT));
	}

	/**
	 * Applies a country's defaults: currency, units (added to the unit list if missing), purities and taxes.
	 * @param defaults The country's defaults.
	 */
	void applyCountry(CountryDefaults.Defaults defaults)
	{
		ops.setValue("$country", defaults.country());
		setCurrency(defaults.currency());
		defaults.units().forEach((group, unit) -> setUnit(group, ensureUnit(unit.unit()), unit.quantity()));
		setPurities(defaults.purities());
		if(!defaults.taxes().isEmpty()) ops.setValue("$taxes", TaxSettings.format(defaults.taxes()));
	}

	/**
	 * Makes sure a standard unit is in the unit list, checked.
	 * @param unit The unit.
	 * @return Its name in the list.
	 */
	String ensureUnit(MassUnit unit)
	{
		String name = unitName(unit);
		if(!ops.hasUnit(name)) ops.setValue("*" + name, String.valueOf(0.001 / unit.grams()));
		else if(ops.getValue("*" + name, null) == null)
		{
			// the unit exists but is unchecked: check it so it can be chosen in the calculator
			String value = ops.getValue("_" + name, null);
			ops.removeValue("_" + name);
			ops.setValue("*" + name, value);
		}
		return name;
	}

	String currency()
	{
		return CurrencyCatalog.resolve(ops.getValue("$currency", "USD")).getCurrencyCode();
	}

	void setCurrency(String code)
	{
		ops.setValue("$currency", CurrencyCatalog.resolve(code).getCurrencyCode());
	}

	/**
	 * Gets the unit a metal group is shown in; units removed from the list fall back to grams.
	 * @param group The metal group.
	 * @return The unit.
	 */
	DisplayUnit unit(Metal.Group group)
	{
		String name = ops.getValue("$unit." + key(group), "");
		double grams = gramsOf(name);
		if(!(grams > 0))
		{
			name = unitName(group == Metal.Group.BASE ? MassUnit.KILOGRAM : MassUnit.GRAM);
			grams = group == Metal.Group.BASE ? 1000 : 1;
		}
		double quantity;
		try
		{
			quantity = Double.parseDouble(ops.getValue("$qty." + key(group), "1"));
		}
		catch(NumberFormatException e)
		{
			quantity = 1;
		}
		return new DisplayUnit(name, quantity > 0 ? quantity : 1, grams);
	}

	/**
	 * Tells which metal group shows its rates in a unit.
	 * @param unitName A unit name from the unit list.
	 * @return A name for the group such as "gold", or null if no group uses the unit.
	 */
	String groupUsing(String unitName)
	{
		for(Metal.Group group : Metal.Group.values())
		{
			if(ops.getValue("$unit." + key(group), "").equalsIgnoreCase(unitName))
				return switch(group)
				{
					case GOLD -> "gold";
					case SILVER -> "silver";
					case PLATINUM_GROUP -> "platinum and palladium";
					case BASE -> "base metal";
				};
		}
		return null;
	}

	void setUnit(Metal.Group group, String unitName, double quantity)
	{
		ops.setValue("$unit." + key(group), unitName);
		ops.setValue("$qty." + key(group), quantity == Math.rint(quantity) ? String.valueOf((long)quantity) : String.valueOf(quantity));
	}

	/**
	 * Gets the weight of a unit from the unit list.
	 * @param unitName The unit's name.
	 * @return Grams per unit, or 0 if the unit is not in the list.
	 */
	double gramsOf(String unitName)
	{
		if(unitName == null || unitName.isEmpty() || !ops.hasUnit(unitName)) return 0;
		String value = ops.getValue("*" + unitName.toLowerCase(), ops.getValue("_" + unitName.toLowerCase(), "0"));
		try
		{
			double perMilligram = Double.parseDouble(value);
			return perMilligram > 0 ? 0.001 / perMilligram : 0;
		}
		catch(NumberFormatException e)
		{
			return 0;
		}
	}

	List<CountryDefaults.Purity> purities()
	{
		List<CountryDefaults.Purity> purities = CountryDefaults.Purity.parseList(ops.getValue("$purities", ""));
		return purities.isEmpty() ? List.of(new CountryDefaults.Purity("24K", 0.999), new CountryDefaults.Purity("22K", 0.916),
				new CountryDefaults.Purity("18K", 0.750)) : purities;
	}

	void setPurities(List<CountryDefaults.Purity> purities)
	{
		ops.setValue("$purities", CountryDefaults.Purity.formatList(purities));
	}

	@Override
	public List<RateService.SourceChoice> sources()
	{
		List<RateService.SourceChoice> sources = new ArrayList<>();
		for(String entry : ops.getValue("$sources", DEFAULT_SOURCES).split(","))
		{
			String id = entry.trim();
			if(id.isEmpty()) continue;
			boolean enabled = !id.endsWith(":off");
			sources.add(new RateService.SourceChoice(enabled ? id : id.substring(0, id.length() - 4), enabled));
		}
		return sources;
	}

	void setSources(List<RateService.SourceChoice> sources)
	{
		ops.setValue("$sources", sources.stream().map(s -> s.id() + (s.enabled() ? "" : ":off")).collect(Collectors.joining(",")));
	}

	@Override
	public ProviderSettings providerSettings(String providerId)
	{
		String prefix = "$provider." + providerId + ".";
		return new ProviderSettings()
		{
			@Override
			public Optional<String> get(String name)
			{
				return Optional.ofNullable(ops.getValue(prefix + name.toLowerCase(Locale.ROOT), null));
			}

			@Override
			public void put(String name, String value)
			{
				if(value == null || value.isEmpty()) ops.removeValue(prefix + name.toLowerCase(Locale.ROOT));
				else ops.setValue(prefix + name, value);
			}
		};
	}

	@Override
	public int autoRefreshMinutes()
	{
		try
		{
			return Math.max(0, Integer.parseInt(ops.getValue("$rateauto", "10")));
		}
		catch(NumberFormatException e)
		{
			return 10;
		}
	}

	void setAutoRefreshMinutes(int minutes)
	{
		ops.setValue("$rateauto", String.valueOf(Math.max(0, minutes)));
	}

	@Override
	public List<LocalTime> schedule(String providerId)
	{
		List<LocalTime> times = new ArrayList<>();
		for(String time : providerSettings(providerId).get("schedule").orElse("").split(","))
		{
			try
			{
				if(!time.isBlank()) times.add(LocalTime.parse(time.trim()));
			}
			catch(DateTimeParseException e)
			{
				// skipped
			}
		}
		return times;
	}

	void setSchedule(String providerId, List<LocalTime> times)
	{
		providerSettings(providerId).put("schedule", times.stream().sorted().map(LocalTime::toString).collect(Collectors.joining(",")));
	}

	boolean webDisclaimerAccepted()
	{
		return ops.getValue("$webdisclaimer", "").equals("accepted");
	}

	void acceptWebDisclaimer()
	{
		ops.setValue("$webdisclaimer", "accepted");
	}
}
