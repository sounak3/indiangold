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
import com.sounaks.indiangold.rates.Quote;
import com.sounaks.indiangold.rates.RateService;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Lets the user choose how market prices are shown: the quantity and unit for each metal group (e.g. gold per 10 g,
 * silver per kg) and the gold purity rows. Prices are always the market prices converted to these units.
 * @author Sounak Choudhury
 */
final class CustomizeRatesDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	private static final Metal.Group[] GROUPS = { Metal.Group.GOLD, Metal.Group.SILVER, Metal.Group.PLATINUM_GROUP, Metal.Group.BASE };

	private final FileOperations ops;
	private final MarketSettings settings;
	private final transient RateService.Rates rates;
	private final Map<Metal.Group, NumberField> quantities = new EnumMap<>(Metal.Group.class);
	private final Map<Metal.Group, JComboBox<String>> units = new EnumMap<>(Metal.Group.class);
	private final Map<Metal.Group, JLabel> previews = new EnumMap<>(Metal.Group.class);
	private final DefaultTableModel purityModel = new DefaultTableModel(new Object[] { "Gold row", "Purity %", "Price" }, 0)
	{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return column < 2;
		}
	};
	private boolean saved;

	private CustomizeRatesDialog(Dialog owner, FileOperations ops, MarketSettings settings, RateService service)
	{
		super(owner, "Customize rates", true);
		this.ops = ops;
		this.settings = settings;
		this.rates = service.current();

		JPanel groups = new JPanel(new GridBagLayout());
		groups.setBorder(BorderFactory.createTitledBorder("Show market prices in your own units"));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 4, 3, 4);
		c.anchor = GridBagConstraints.LINE_START;
		Vector<String> checkedUnits = new Vector<>(ops.getCheckedUnitNames());
		int row = 0;
		for(Metal.Group group : GROUPS)
		{
			MarketSettings.DisplayUnit current = settings.unit(group);
			NumberField quantity = new NumberField(5, false);
			quantity.setText(format(current.quantity()));
			JComboBox<String> unit = new JComboBox<>(checkedUnits);
			if(!checkedUnits.contains(current.name())) unit.addItem(current.name());
			unit.setSelectedItem(current.name());
			JLabel preview = new JLabel();
			quantities.put(group, quantity);
			units.put(group, unit);
			previews.put(group, preview);
			quantity.getDocument().addDocumentListener(onChange(this::updatePreviews));
			unit.addActionListener(e -> updatePreviews());

			c.gridy = row++;
			c.gridx = 0;
			groups.add(new JLabel("<html><font color=" + (group == Metal.Group.BASE ? "blue" : "red") + ">" + title(group) + "</font></html>"), c);
			c.gridx = 1;
			groups.add(new JLabel(settings.currency() + " per"), c);
			c.gridx = 2;
			groups.add(quantity, c);
			c.gridx = 3;
			groups.add(unit, c);
			c.gridx = 4;
			groups.add(preview, c);
		}

		for(CountryDefaults.Purity purity : settings.purities())
			purityModel.addRow(new Object[] { purity.label(), percent(purity.fineness()), "" });
		purityModel.addTableModelListener(e -> {
			if(e.getColumn() != 2) SwingUtilities.invokeLater(this::updatePreviews);
		});
		JTable purityTable = new JTable(purityModel);
		purityTable.setRowHeight(purityTable.getRowHeight() + 4);
		purityTable.setPreferredScrollableViewportSize(new java.awt.Dimension(380, purityTable.getRowHeight() * 5));
		JButton addPurity = new JButton("Add row");
		addPurity.addActionListener(e -> purityModel.addRow(new Object[] { "", "", "" }));
		JButton removePurity = new JButton("Remove row");
		removePurity.addActionListener(e -> {
			if(purityTable.isEditing()) purityTable.getCellEditor().stopCellEditing();
			int selected = purityTable.getSelectedRow();
			if(selected >= 0 && purityModel.getRowCount() > 1) purityModel.removeRow(selected);
		});
		JPanel purityButtons = new JPanel(new FlowLayout(FlowLayout.LEADING));
		purityButtons.add(addPurity);
		purityButtons.add(removePurity);
		JPanel purities = new JPanel(new BorderLayout());
		purities.setBorder(BorderFactory.createTitledBorder("Gold rows in the rate bar (e.g. 22K = 91.6%)"));
		purities.add(new JScrollPane(purityTable), BorderLayout.CENTER);
		purities.add(purityButtons, BorderLayout.SOUTH);

		JButton reset = new JButton("Reset to country defaults");
		reset.setToolTipText("Use the usual units and gold rows of your country (Settings \u2192 country).");
		reset.addActionListener(e -> resetToCountry());
		JButton ok = new JButton("OK");
		ok.addActionListener(e -> save(purityTable));
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		buttons.add(reset);
		buttons.add(ok);
		buttons.add(cancel);

		JPanel content = new JPanel(new BorderLayout(6, 6));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
		content.add(groups, BorderLayout.NORTH);
		content.add(purities, BorderLayout.CENTER);
		add(content, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(ok);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		updatePreviews();
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Shows the dialog. Changes go into the settings like the rest of the settings window: saved with its OK,
	 * dropped with its Cancel.
	 * @return True if the user pressed OK.
	 */
	static boolean show(Dialog owner, FileOperations ops, MarketSettings settings, RateService service)
	{
		CustomizeRatesDialog dialog = new CustomizeRatesDialog(owner, ops, settings, service);
		dialog.setVisible(true);
		return dialog.saved;
	}

	/** A one-line summary for the settings window, e.g. "INR: gold per 10 g, silver per kg, ...". */
	static String summary(MarketSettings settings)
	{
		List<String> parts = new ArrayList<>();
		for(Metal.Group group : GROUPS) parts.add(title(group).toLowerCase(Locale.ROOT) + " per " + settings.unit(group).label());
		List<String> rows = new ArrayList<>();
		settings.purities().forEach(p -> rows.add(p.label()));
		return "<html>" + settings.currency() + ": " + String.join(", ", parts) + "<br>Gold rows: " + String.join(", ", rows) + "</html>";
	}

	private static String title(Metal.Group group)
	{
		return switch(group)
		{
			case GOLD -> "Gold";
			case SILVER -> "Silver";
			case PLATINUM_GROUP -> "Platinum & palladium";
			case BASE -> "Base metals";
		};
	}

	/** The metal whose price is previewed for a group. */
	private static Metal sample(Metal.Group group)
	{
		return switch(group)
		{
			case GOLD -> Metal.GOLD;
			case SILVER -> Metal.SILVER;
			case PLATINUM_GROUP -> Metal.PLATINUM;
			case BASE -> Metal.COPPER;
		};
	}

	private double grams(Metal.Group group)
	{
		double quantity;
		try
		{
			quantity = quantities.get(group).getNumberInput();
		}
		catch(NumberFormatException e)
		{
			quantity = 0;
		}
		return quantity * settings.gramsOf((String)units.get(group).getSelectedItem());
	}

	private String price(Metal metal, double grams, double fineness)
	{
		Quote quote = rates.quotes().get(metal);
		if(quote == null || !(grams > 0)) return "\u2014";
		String currency = rates.fx().has(settings.currency()) ? settings.currency() : "USD";
		double price = quote.priceIn(rates.fx(), currency, grams, fineness);
		if(Double.isNaN(price)) return "\u2014";
		NumberFormat format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(price < 10 ? 4 : 2);
		format.setMinimumFractionDigits(2);
		return format.format(price) + " " + currency;
	}

	private void updatePreviews()
	{
		for(Metal.Group group : GROUPS)
			previews.get(group).setText("<html><font color=gray>= " + price(sample(group), grams(group), 1)
					+ (group == Metal.Group.BASE ? " (copper)" : group == Metal.Group.PLATINUM_GROUP ? " (platinum)" : "") + "</font></html>");
		double goldGrams = grams(Metal.Group.GOLD);
		for(int row = 0; row < purityModel.getRowCount(); row++)
		{
			double fineness = fineness(purityModel.getValueAt(row, 1));
			String text = fineness > 0 ? price(Metal.GOLD, goldGrams, fineness) : "enter 1 to 100";
			if(!text.equals(purityModel.getValueAt(row, 2))) purityModel.setValueAt(text, row, 2);
		}
	}

	private void resetToCountry()
	{
		CountryDefaults.Defaults defaults = CountryDefaults.load().forCountry(settings.country().orElse(CountryDefaults.systemCountry()));
		for(Metal.Group group : GROUPS)
		{
			CountryDefaults.GroupUnit unit = defaults.units().get(group);
			String name = settings.ensureUnit(unit.unit()); // e.g. tonne, which may not be in the unit list yet
			if(((DefaultComboBoxModel<String>)units.get(group).getModel()).getIndexOf(name) < 0) units.get(group).addItem(name);
			units.get(group).setSelectedItem(name);
			quantities.get(group).setText(format(unit.quantity()));
		}
		purityModel.setRowCount(0);
		for(CountryDefaults.Purity purity : defaults.purities()) purityModel.addRow(new Object[] { purity.label(), percent(purity.fineness()), "" });
		updatePreviews();
	}

	private void save(JTable purityTable)
	{
		if(purityTable.isEditing()) purityTable.getCellEditor().stopCellEditing();
		for(Metal.Group group : GROUPS)
		{
			if(!(grams(group) > 0))
			{
				JOptionPane.showMessageDialog(this, "Enter a quantity above 0 for " + title(group).toLowerCase(Locale.ROOT) + ".", "Customize rates", JOptionPane.INFORMATION_MESSAGE);
				quantities.get(group).requestFocus();
				return;
			}
		}
		List<CountryDefaults.Purity> purities = new ArrayList<>();
		for(int row = 0; row < purityModel.getRowCount(); row++)
		{
			String label = String.valueOf(purityModel.getValueAt(row, 0)).trim();
			double fineness = fineness(purityModel.getValueAt(row, 1));
			if(label.isEmpty() && fineness == 0) continue;
			if(label.isEmpty() || fineness == 0)
			{
				JOptionPane.showMessageDialog(this, "Every gold row needs a name and a purity between 1 and 100 %.", "Customize rates", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			purities.add(new CountryDefaults.Purity(label, fineness));
		}
		if(purities.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Keep at least one gold row.", "Customize rates", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		for(Metal.Group group : GROUPS)
		{
			settings.setUnit(group, (String)units.get(group).getSelectedItem(), quantities.get(group).getNumberInput());
		}
		settings.setPurities(purities);
		saved = true;
		dispose();
	}

	private static double fineness(Object percentText)
	{
		try
		{
			double percent = Double.parseDouble(String.valueOf(percentText).replace("%", "").trim());
			return percent >= 1 && percent <= 100 ? percent / 100 : 0;
		}
		catch(NumberFormatException e)
		{
			return 0;
		}
	}

	private static String percent(double fineness)
	{
		return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(fineness * 100);
	}

	private static String format(double quantity)
	{
		return quantity == Math.rint(quantity) ? String.valueOf((long)quantity) : String.valueOf(quantity);
	}

	private static DocumentListener onChange(Runnable action)
	{
		return new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				action.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				action.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				action.run();
			}
		};
	}
}
