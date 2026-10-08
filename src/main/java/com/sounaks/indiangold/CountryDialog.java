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

import com.sounaks.indiangold.rates.Metal;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Asks for the user's country at first start (and once after updating from an older version), shows what it sets,
 * and asks whether web page sources may be used.
 * @author Sounak Choudhury
 */
final class CountryDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	/** Shown wherever web page sources are switched on. */
	static final String WEB_DISCLAIMER = "<html><b>Web page sources</b><br>Some prices (LME base metals and tin) are read from the "
			+ "westmetall.com web page,<br>because no free API offers them. IndianGold reads such pages at most once an hour,<br>"
			+ "only where the site's robots.txt allows it, and says who it is.<br>Whether a site's terms of use allow this is for you "
			+ "to check; you use them<br>at your own responsibility, for personal use.</html>";

	/**
	 * What the user chose.
	 * @param country ISO code of the country.
	 * @param applyDefaults Whether to set the country's currency, units, purities and taxes.
	 * @param webSources Whether web page sources may be used.
	 */
	record Choice(String country, boolean applyDefaults, boolean webSources)
	{
	}

	private final CountryDefaults countries;
	private final JComboBox<CountryDefaults.Country> countryBox;
	private final JLabel preview = new JLabel();
	private final JCheckBox applyBox = new JCheckBox("Also set the currency, units, gold purities and taxes for this country");
	private final JCheckBox webBox = new JCheckBox("Use web page sources (base metals and tin)", true);
	private Choice choice;

	private CountryDialog(Frame owner, CountryDefaults countries, String current, boolean existingUser)
	{
		super(owner, existingUser ? "IndianGold 5: choose your country" : "Welcome to IndianGold", true);
		this.countries = countries;
		countryBox = new JComboBox<>(countries.countries().toArray(CountryDefaults.Country[]::new));
		countries.countries().stream().filter(c -> c.code().equals(current)).findFirst().ifPresent(countryBox::setSelectedItem);
		countryBox.addActionListener(e -> updatePreview());
		countryBox.setMaximumRowCount(20);
		applyBox.setSelected(!existingUser);
		applyBox.setVisible(existingUser); // new users always get the defaults

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
		JLabel intro = new JLabel(existingUser
				? "<html>Market rates now come from new sources and can be shown in your country's usual units.<br>Choose your country:</html>"
				: "<html>Choose your country to set the currency, the units prices are shown in<br>and the gold purities. You can change all of it later in Settings.</html>");
		for(JComponent c : new JComponent[] { intro, countryBox, preview, applyBox, new JSeparator(), new JLabel(WEB_DISCLAIMER), webBox })
		{
			c.setAlignmentX(LEFT_ALIGNMENT);
			content.add(c);
			content.add(Box.createRigidArea(new Dimension(0, 8)));
		}
		countryBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, countryBox.getPreferredSize().height));

		JButton ok = new JButton("OK");
		ok.addActionListener(e -> {
			CountryDefaults.Country selected = (CountryDefaults.Country)countryBox.getSelectedItem();
			choice = new Choice(selected.code(), applyBox.isSelected(), webBox.isSelected());
			dispose();
		});
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		buttons.add(ok);
		getRootPane().setDefaultButton(ok);
		add(content, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		updatePreview();
		pack();
		setLocationRelativeTo(owner);
	}

	private void updatePreview()
	{
		CountryDefaults.Country selected = (CountryDefaults.Country)countryBox.getSelectedItem();
		if(selected == null) return;
		CountryDefaults.Defaults d = countries.forCountry(selected.code());
		Currency currency = Currency.getInstance(d.currency());
		String taxes = d.taxes().isEmpty() ? "none set" : d.taxes().stream().map(t -> t.name() + " " + t.percent() + "%").collect(Collectors.joining(", "));
		preview.setText("<html><font color=gray>Currency: " + currency.getDisplayName() + " (" + currency.getCurrencyCode() + ")"
				+ "<br>Prices per: gold " + label(d, Metal.Group.GOLD) + ", silver " + label(d, Metal.Group.SILVER)
				+ ", platinum " + label(d, Metal.Group.PLATINUM_GROUP) + ", base metals " + label(d, Metal.Group.BASE)
				+ "<br>Gold rows: " + d.purities().stream().map(CountryDefaults.Purity::label).collect(Collectors.joining(", "))
				+ "<br>Taxes: " + taxes + "</font></html>");
	}

	private static String label(CountryDefaults.Defaults d, Metal.Group group)
	{
		CountryDefaults.GroupUnit unit = d.units().get(group);
		String name = MarketSettings.unitName(unit.unit());
		int open = name.indexOf('('), close = name.indexOf(')');
		String shortName = open >= 0 && close > open ? name.substring(open + 1, close) : name;
		double qty = unit.quantity();
		return (qty == 1 ? "" : (qty == Math.rint(qty) ? String.valueOf((long)qty) : String.valueOf(qty)) + " ") + shortName;
	}

	/**
	 * Shows the dialog.
	 * @param owner The main window.
	 * @param countries The country table.
	 * @param current The country to preselect.
	 * @param existingUser Whether the user has settings from an older version, which are kept unless they tick the box.
	 * @return The choice, or empty if the dialog was closed (it will be shown again next time).
	 */
	static Optional<Choice> ask(Frame owner, CountryDefaults countries, String current, boolean existingUser)
	{
		CountryDialog dialog = new CountryDialog(owner, countries, current, existingUser);
		dialog.setVisible(true);
		return Optional.ofNullable(dialog.choice);
	}

	/** For the settings window: the countries in a combo box. */
	static JComboBox<CountryDefaults.Country> countryBox(CountryDefaults countries, String current)
	{
		List<CountryDefaults.Country> all = countries.countries();
		JComboBox<CountryDefaults.Country> box = new JComboBox<>(all.toArray(CountryDefaults.Country[]::new));
		all.stream().filter(c -> c.code().equals(current)).findFirst().ifPresent(box::setSelectedItem);
		box.setMaximumRowCount(20);
		return box;
	}
}
