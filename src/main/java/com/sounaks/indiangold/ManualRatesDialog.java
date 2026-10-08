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

import com.sounaks.indiangold.rates.FxRates;
import com.sounaks.indiangold.rates.ManualProvider;
import com.sounaks.indiangold.rates.MassUnit;
import com.sounaks.indiangold.rates.Metal;
import com.sounaks.indiangold.rates.ProviderSettings;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.*;

/**
 * Lets the user type in prices, e.g. from a local jeweller's board, with the date they are for. Rows left empty are
 * not used; the gold purity rows are calculated from the gold price entered here (pure gold).
 * @author Sounak Choudhury
 */
final class ManualRatesDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	private final JSpinner date;
	private final JComboBox<CurrencyCatalog.Choice> currency;
	private final Map<Metal, NumberField> prices = new EnumMap<>(Metal.class);
	private final Map<Metal, NumberField> quantities = new EnumMap<>(Metal.class);
	private final Map<Metal, JComboBox<MassUnit>> units = new EnumMap<>(Metal.class);
	private boolean saved;

	private ManualRatesDialog(Dialog owner, MarketSettings settings, FxRates fx)
	{
		super(owner, "Enter prices", true);
		ProviderSettings stored = settings.providerSettings(ManualProvider.ID);

		LocalDate day = stored.get(ManualProvider.DATE).flatMap(ManualRatesDialog::parseDate).orElse(LocalDate.now());
		date = new JSpinner(new SpinnerDateModel(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant()), null, null, Calendar.DAY_OF_MONTH));
		date.setEditor(new JSpinner.DateEditor(date, "d MMM yyyy"));
		String currencyCode = stored.get(ManualProvider.CURRENCY).orElse(settings.currency());
		currency = new JComboBox<>(CurrencyCatalog.choices(fx, currencyCode).toArray(CurrencyCatalog.Choice[]::new));
		for(int i = 0; i < currency.getItemCount(); i++)
			if(currency.getItemAt(i).currency().getCurrencyCode().equals(currencyCode)) currency.setSelectedIndex(i);

		JPanel table = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 4, 2, 4);
		c.anchor = GridBagConstraints.LINE_START;
		c.gridy = 0;
		String[] headings = { "Metal", "Price", "per", "Unit" };
		for(int i = 0; i < headings.length; i++)
		{
			c.gridx = i;
			table.add(new JLabel("<html><b>" + headings[i] + "</b></html>"), c);
		}
		for(Metal metal : Metal.values())
		{
			MarketSettings.DisplayUnit shown = settings.unit(metal.group());
			NumberField price = new NumberField(9, false);
			price.setText(stored.get(ManualProvider.priceKey(metal)).orElse(""));
			NumberField quantity = new NumberField(4, false);
			quantity.setText(stored.get(ManualProvider.perKey(metal)).orElse(format(shown.quantity())));
			JComboBox<MassUnit> unit = new JComboBox<>(MassUnit.values());
			unit.setRenderer(new UnitDisplay.ListRenderer());
			unit.setSelectedItem(stored.get(ManualProvider.unitKey(metal)).flatMap(MassUnit::fromCode).orElse(massUnitOf(shown)));
			prices.put(metal, price);
			quantities.put(metal, quantity);
			units.put(metal, unit);
			c.gridy++;
			c.gridx = 0;
			table.add(new JLabel(metal == Metal.GOLD ? "Gold (24K, pure)" : metal.displayName()), c);
			c.gridx = 1;
			table.add(price, c);
			c.gridx = 2;
			table.add(quantity, c);
			c.gridx = 3;
			table.add(unit, c);
		}

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
		top.add(new JLabel("Prices of"));
		top.add(date);
		top.add(new JLabel("in"));
		top.add(currency);
		JLabel hint = new JLabel("<html><font color=gray>Leave a price empty if you don't have it; another source may fill it in.<br>"
				+ "Gold rows such as 22K are calculated from the gold price.</font></html>");

		JButton ok = new JButton("OK");
		ok.addActionListener(e -> save(stored));
		JButton clear = new JButton("Clear all");
		clear.addActionListener(e -> prices.values().forEach(field -> field.setText("")));
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		buttons.add(clear);
		buttons.add(ok);
		buttons.add(cancel);

		JPanel content = new JPanel(new BorderLayout(6, 6));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
		content.add(top, BorderLayout.NORTH);
		content.add(table, BorderLayout.CENTER);
		content.add(hint, BorderLayout.SOUTH);
		add(content, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(ok);
		pack();
		setLocationRelativeTo(owner);
	}

	private void save(ProviderSettings stored)
	{
		LocalDate day = ((Date)date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if(day.isAfter(LocalDate.now()))
		{
			JOptionPane.showMessageDialog(this, "The date can't be in the future.", "Enter prices", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		stored.put(ManualProvider.DATE, day.toString());
		stored.put(ManualProvider.CURRENCY, ((CurrencyCatalog.Choice)currency.getSelectedItem()).currency().getCurrencyCode());
		for(Metal metal : Metal.values())
		{
			String price = prices.get(metal).getText().trim();
			boolean has = !price.isEmpty() && prices.get(metal).getNumberInput() > 0;
			stored.put(ManualProvider.priceKey(metal), has ? price : "");
			stored.put(ManualProvider.perKey(metal), has ? format(Math.max(quantities.get(metal).getNumberInput(), 0) == 0 ? 1 : quantities.get(metal).getNumberInput()) : "");
			stored.put(ManualProvider.unitKey(metal), has ? ((MassUnit)units.get(metal).getSelectedItem()).code() : "");
		}
		saved = true;
		dispose();
	}

	private static MassUnit massUnitOf(MarketSettings.DisplayUnit shown)
	{
		for(MassUnit unit : MassUnit.values())
			if(MarketSettings.unitName(unit).equalsIgnoreCase(shown.name())) return unit;
		return MassUnit.GRAM;
	}

	private static Optional<LocalDate> parseDate(String text)
	{
		try
		{
			return Optional.of(LocalDate.parse(text));
		}
		catch(java.time.format.DateTimeParseException e)
		{
			return Optional.empty();
		}
	}

	private static String format(double quantity)
	{
		return quantity == Math.rint(quantity) ? String.valueOf((long)quantity) : String.valueOf(quantity);
	}

	/**
	 * Shows the dialog; the prices are kept with the other settings (saved by the settings window's OK).
	 * @return True if the user pressed OK.
	 */
	static boolean show(Dialog owner, MarketSettings settings, FxRates fx)
	{
		ManualRatesDialog dialog = new ManualRatesDialog(owner, settings, fx);
		dialog.setVisible(true);
		return dialog.saved;
	}
}
