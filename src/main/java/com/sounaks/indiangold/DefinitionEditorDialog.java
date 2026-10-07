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
import com.sounaks.indiangold.rates.MassUnit;
import com.sounaks.indiangold.rates.Metal;
import com.sounaks.indiangold.rates.ProviderDefinition;
import com.sounaks.indiangold.rates.ProviderSettings;
import com.sounaks.indiangold.rates.Quote;
import com.sounaks.indiangold.rates.RateProvider;
import com.sounaks.indiangold.rates.RateService;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Creates or changes a rate source defined by a properties file (a JSON API or a web page), with a Test button that
 * shows what the definition reads. The form covers the common keys; the "All keys" tab shows every key, including
 * the advanced ones (error.*, fx.*, usage.*). Saved to ~/.indiangold/providers/ID.properties.
 * @author Sounak Choudhury
 */
final class DefinitionEditorDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	/** Keys the form edits; everything else is kept as it is. */
	private static final Set<String> FORM_KEYS = Set.of("id", "name", "type", "description", "url", "url.fallback", "apikey", "signup", "terms",
			"currency", "minInterval", "quota.monthly", "schedule", "asOf.path", "asOf.select", "asOf.format", "number.decimal", "number.grouping");

	private final transient RateService service;
	private final transient MarketSettings settings;
	private Properties properties;
	private final JTabbedPane tabs = new JTabbedPane();
	private final JTextArea raw = new JTextArea(22, 70);
	private final JTextField id = new JTextField(20), name = new JTextField(30), description = new JTextField(40), url = new JTextField(40),
			fallback = new JTextField(40), signup = new JTextField(40), terms = new JTextField(40), currency = new JTextField(5),
			schedule = new JTextField(12), asOf = new JTextField(30), asOfFormat = new JTextField(14), decimal = new JTextField(2), grouping = new JTextField(2);
	private final JRadioButton jsonApi = new JRadioButton("JSON API"), webPage = new JRadioButton("Web page");
	private final JCheckBox apiKey = new JCheckBox("Needs an API key (write {apikey} in the address)");
	private final JSpinner interval = new JSpinner(new SpinnerNumberModel(5, 1, 24 * 60, 1));
	private final JSpinner quota = new JSpinner(new SpinnerNumberModel(0, 0, 10_000_000, 10));
	private final JLabel locatorHeading = new JLabel();
	private final DefaultTableModel metals = new DefaultTableModel(new Object[] { "Use", "Metal", "Where (path or selector)", "Unit", "Per", "Invert", "Own address (optional)" }, 0)
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Class<?> getColumnClass(int column)
		{
			return column == 0 || column == 5 ? Boolean.class : column == 3 ? MassUnit.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return column != 1;
		}
	};
	private Path saved;

	private DefinitionEditorDialog(Dialog owner, String title, Properties start, RateService service, MarketSettings settings)
	{
		super(owner, title, true);
		this.service = service;
		this.settings = settings;
		this.properties = start;

		ButtonGroup types = new ButtonGroup();
		types.add(jsonApi);
		types.add(webPage);
		jsonApi.addActionListener(e -> updateTypeHints());
		webPage.addActionListener(e -> updateTypeHints());

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 4, 3, 4);
		c.anchor = GridBagConstraints.LINE_START;
		int[] row = { 0 };
		java.util.function.BiConsumer<String, JComponent> line = (label, field) -> {
			c.gridy = row[0]++;
			c.gridx = 0;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			form.add(new JLabel(label), c);
			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			form.add(field, c);
		};
		JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		typeRow.add(jsonApi);
		typeRow.add(webPage);
		JPanel idRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		idRow.add(id);
		idRow.add(new JLabel("  lower-case letters, digits and dashes"));
		line.accept("Id", idRow);
		line.accept("Name", name);
		line.accept("Type", typeRow);
		line.accept("Description", description);
		line.accept("Address", url);
		line.accept("Fallback address", fallback);
		line.accept("", apiKey);
		line.accept("Get-a-key page", signup);
		line.accept("Terms of use page", terms);
		JPanel timing = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		timing.add(new JLabel("Currency "));
		timing.add(currency);
		timing.add(new JLabel("   at most every "));
		timing.add(interval);
		timing.add(new JLabel(" minutes   requests per month "));
		timing.add(quota);
		timing.add(new JLabel(" (0 = no limit)  at "));
		timing.add(schedule);
		line.accept("Prices", timing);
		JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		dateRow.add(asOf);
		dateRow.add(new JLabel("  format "));
		dateRow.add(asOfFormat);
		dateRow.add(new JLabel("  numbers: decimal "));
		dateRow.add(decimal);
		dateRow.add(new JLabel(" grouping "));
		dateRow.add(grouping);
		line.accept("Price date at", dateRow);

		JTable metalTable = new JTable(metals);
		metalTable.setRowHeight(metalTable.getRowHeight() + 4);
		JComboBox<MassUnit> unitBox = new JComboBox<>(MassUnit.values());
		unitBox.setRenderer(new UnitDisplay.ListRenderer());
		metalTable.setDefaultEditor(MassUnit.class, new DefaultCellEditor(unitBox));
		metalTable.setDefaultRenderer(MassUnit.class, new UnitDisplay.TableRenderer());
		metalTable.getColumnModel().getColumn(0).setMaxWidth(40);
		metalTable.getColumnModel().getColumn(1).setPreferredWidth(80);
		metalTable.getColumnModel().getColumn(2).setPreferredWidth(330);
		metalTable.getColumnModel().getColumn(3).setPreferredWidth(80);
		metalTable.getColumnModel().getColumn(4).setPreferredWidth(40);
		metalTable.getColumnModel().getColumn(5).setMaxWidth(50);
		metalTable.getColumnModel().getColumn(6).setPreferredWidth(230);
		metalTable.setPreferredScrollableViewportSize(new Dimension(900, metalTable.getRowHeight() * Metal.values().length));
		JPanel metalPanel = new JPanel(new BorderLayout());
		metalPanel.add(locatorHeading, BorderLayout.NORTH);
		metalPanel.add(new JScrollPane(metalTable), BorderLayout.CENTER);

		JPanel formTab = new JPanel(new BorderLayout(6, 6));
		formTab.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		formTab.add(form, BorderLayout.NORTH);
		formTab.add(metalPanel, BorderLayout.CENTER);
		raw.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, raw.getFont().getSize()));
		JPanel rawTab = new JPanel(new BorderLayout());
		rawTab.add(new JLabel("<html>Every key of the definition. Advanced keys: error.path, error.code.path, error.kind.CODE, fx.path, fx.url, "
				+ "fx.direction, usage.url, usage.*.path, asOf.locale.<br>See the comments in the built-in definitions for examples.</html>"), BorderLayout.NORTH);
		rawTab.add(new JScrollPane(raw), BorderLayout.CENTER);
		tabs.addTab("Form", formTab);
		tabs.addTab("All keys", rawTab);
		tabs.addChangeListener(e -> {
			if(tabs.getSelectedIndex() == 1) raw.setText(format(fromForm()));
			else loadForm(parse(raw.getText()));
		});

		JButton test = new JButton("Test");
		test.setToolTipText("Fetches the address now and shows what this definition reads");
		test.addActionListener(e -> test(test));
		JButton save = new JButton("Save");
		save.addActionListener(e -> save());
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		buttons.add(test);
		buttons.add(save);
		buttons.add(cancel);
		add(tabs, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		loadForm(start);
		pack();
		setLocationRelativeTo(owner);
	}

	private void updateTypeHints()
	{
		locatorHeading.setText(webPage.isSelected()
				? "<html>For a web page, give a CSS selector of the element holding each price, e.g. <code>table#prices tr:has(td:containsOwn(Gold)) td:eq(1)</code>.<br>"
						+ "Web pages are read at most once an hour, only where robots.txt allows it.</html>"
				: "<html>For a JSON API, give a JSON pointer to each price, e.g. <code>/metals/gold</code> or <code>/data/0/price</code>. "
						+ "Tick Invert if the value is ounces per dollar rather than dollars per ounce.</html>");
	}

	private void loadForm(Properties p)
	{
		properties = p;
		id.setText(p.getProperty("id", ""));
		name.setText(p.getProperty("name", ""));
		boolean page = ProviderDefinition.TYPE_WEB_PAGE.equals(p.getProperty("type", ProviderDefinition.TYPE_JSON_API));
		webPage.setSelected(page);
		jsonApi.setSelected(!page);
		description.setText(p.getProperty("description", ""));
		url.setText(p.getProperty("url", ""));
		fallback.setText(p.getProperty("url.fallback", ""));
		apiKey.setSelected("required".equalsIgnoreCase(p.getProperty("apikey", "")));
		signup.setText(p.getProperty("signup", ""));
		terms.setText(p.getProperty("terms", ""));
		currency.setText(p.getProperty("currency", "USD"));
		interval.setValue(minutes(p.getProperty("minInterval", page ? "60" : "5")));
		quota.setValue(intValue(p.getProperty("quota.monthly", "0")));
		schedule.setText(p.getProperty("schedule", ""));
		asOf.setText(p.getProperty(page ? "asOf.select" : "asOf.path", ""));
		asOfFormat.setText(p.getProperty("asOf.format", ""));
		decimal.setText(p.getProperty("number.decimal", "."));
		grouping.setText(p.getProperty("number.grouping", ","));
		metals.setRowCount(0);
		for(Metal metal : Metal.values())
		{
			String prefix = "metal." + metal.key() + ".";
			String locator = p.getProperty(prefix + (page ? "select" : "path"), "");
			metals.addRow(new Object[] { !locator.isEmpty(), metal.displayName(), locator,
					MassUnit.fromCode(p.getProperty(prefix + "unit", metal.isPrecious() ? "toz" : "t")).orElse(MassUnit.TROY_OUNCE),
					p.getProperty(prefix + "per", "1"), Boolean.parseBoolean(p.getProperty(prefix + "invert", "false")), p.getProperty(prefix + "url", "") });
		}
		updateTypeHints();
	}

	/** The definition as the form shows it, keeping every key the form does not edit. */
	private Properties fromForm()
	{
		Properties p = new Properties();
		for(String key : properties.stringPropertyNames())
			if(!FORM_KEYS.contains(key) && !key.startsWith("metal.")) p.setProperty(key, properties.getProperty(key));
		boolean page = webPage.isSelected();
		put(p, "id", id.getText());
		put(p, "name", name.getText());
		put(p, "type", page ? ProviderDefinition.TYPE_WEB_PAGE : ProviderDefinition.TYPE_JSON_API);
		put(p, "description", description.getText());
		put(p, "url", url.getText());
		put(p, "url.fallback", fallback.getText());
		if(apiKey.isSelected()) p.setProperty("apikey", "required");
		put(p, "signup", signup.getText());
		put(p, "terms", terms.getText());
		put(p, "currency", currency.getText().toUpperCase(java.util.Locale.ROOT));
		p.setProperty("minInterval", String.valueOf(interval.getValue()));
		if((Integer)quota.getValue() > 0) p.setProperty("quota.monthly", String.valueOf(quota.getValue()));
		put(p, "schedule", schedule.getText());
		put(p, page ? "asOf.select" : "asOf.path", asOf.getText());
		put(p, "asOf.format", asOfFormat.getText());
		if(page)
		{
			put(p, "number.decimal", decimal.getText());
			put(p, "number.grouping", grouping.getText());
		}
		for(int row = 0; row < metals.getRowCount(); row++)
		{
			if(!Boolean.TRUE.equals(metals.getValueAt(row, 0))) continue;
			String prefix = "metal." + Metal.values()[row].key() + ".";
			put(p, prefix + (page ? "select" : "path"), String.valueOf(metals.getValueAt(row, 2)));
			p.setProperty(prefix + "unit", ((MassUnit)metals.getValueAt(row, 3)).code());
			put(p, prefix + "per", String.valueOf(metals.getValueAt(row, 4)));
			if(Boolean.TRUE.equals(metals.getValueAt(row, 5))) p.setProperty(prefix + "invert", "true");
			put(p, prefix + "url", String.valueOf(metals.getValueAt(row, 6)));
		}
		return p;
	}

	private Properties current()
	{
		return tabs.getSelectedIndex() == 1 ? parse(raw.getText()) : fromForm();
	}

	private static void put(Properties p, String key, String value)
	{
		if(value != null && !value.isBlank()) p.setProperty(key, value.trim());
	}

	private ProviderDefinition validated()
	{
		ProviderDefinition definition = ProviderDefinition.of(current());
		if(!definition.isValid())
			JOptionPane.showMessageDialog(this, "<html>Please fix:<ul><li>" + String.join("<li>", definition.problems().stream().map(RateBar::escape).toList())
					+ "</ul></html>", "Definition", JOptionPane.WARNING_MESSAGE);
		return definition.isValid() ? definition : null;
	}

	private void test(JButton button)
	{
		ProviderDefinition definition = validated();
		if(definition == null) return;
		RateProvider provider = definition.createProvider();
		ProviderSettings providerSettings = settings.providerSettings(definition.id());
		if(provider.needsApiKey() && providerSettings.apiKey().isEmpty())
		{
			String key = JOptionPane.showInputDialog(this, "API key to test with (it is kept with this source's settings):", "Test", JOptionPane.QUESTION_MESSAGE);
			if(key == null || key.isBlank()) return;
			providerSettings.put(ProviderSettings.API_KEY, key.trim());
		}
		button.setEnabled(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		service.test(provider, providerSettings).whenComplete((snapshot, failure) -> SwingUtilities.invokeLater(() -> {
			button.setEnabled(true);
			setCursor(Cursor.getDefaultCursor());
			if(failure != null)
			{
				Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
				JOptionPane.showMessageDialog(this, cause.getMessage(), "Test failed", JOptionPane.WARNING_MESSAGE);
				return;
			}
			StringBuilder text = new StringBuilder("<html><b>").append(RateBar.escape(definition.name())).append(" read:</b><br><br>");
			Set<Metal> missing = new TreeSet<>(definition.metals().keySet());
			for(Metal metal : Metal.values())
			{
				Quote quote = snapshot.quotes().get(metal);
				if(quote == null) continue;
				missing.remove(metal);
				ProviderDefinition.MetalSpec spec = definition.metals().get(metal);
				NumberFormat format = NumberFormat.getNumberInstance();
				format.setMaximumFractionDigits(4);
				text.append(metal.displayName()).append(": ")
						.append(format.format(quote.priceIn(FxRates.usdOnly(), quote.currency(), spec.per() * spec.unit().grams(), 1)))
						.append(' ').append(quote.currency()).append(" per ").append(spec.per() == 1 ? "" : format.format(spec.per()) + " ")
						.append(spec.unit().code()).append(" <font color=gray>(as of ").append(UnitDisplay.asOf(quote.asOf()))
						.append(")</font><br>");
			}
			snapshot.fx().ifPresent(fx -> text.append("Exchange rates for ").append(fx.perUsd().size()).append(" currencies<br>"));
			if(!missing.isEmpty())
				text.append("<br><font color=red>Not found: ").append(String.join(", ", missing.stream().map(Metal::displayName).toList())).append("</font>");
			JOptionPane.showMessageDialog(this, text.append("</html>").toString(), "Test", JOptionPane.INFORMATION_MESSAGE);
		}));
	}

	private void save()
	{
		ProviderDefinition definition = validated();
		if(definition == null) return;
		try
		{
			saved = service.registry().save(definition);
			dispose();
		}
		catch(IllegalArgumentException | IOException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot save", JOptionPane.WARNING_MESSAGE);
		}
	}

	private static String format(Properties p)
	{
		StringWriter out = new StringWriter();
		new TreeSet<>(p.stringPropertyNames()).forEach(key -> out.append(key).append('=').append(p.getProperty(key).replace("\\", "\\\\")).append('\n'));
		return out.toString();
	}

	private static Properties parse(String text)
	{
		Properties p = new Properties();
		try
		{
			p.load(new StringReader(text));
		}
		catch(IOException | IllegalArgumentException e)
		{
			// keep what could be read
		}
		return p;
	}

	private static int minutes(String value)
	{
		try
		{
			return value.toUpperCase(java.util.Locale.ROOT).startsWith("P")
					? (int)Math.max(1, java.time.Duration.parse(value.toUpperCase(java.util.Locale.ROOT)).toMinutes()) : Math.max(1, Integer.parseInt(value.trim()));
		}
		catch(RuntimeException e)
		{
			return 5;
		}
	}

	private static int intValue(String value)
	{
		try
		{
			return Math.max(0, Integer.parseInt(value.trim()));
		}
		catch(NumberFormatException e)
		{
			return 0;
		}
	}

	/**
	 * Opens the editor.
	 * @param start The definition to start from (an empty one for a new source).
	 * @return The saved file, or null if nothing was saved.
	 */
	static Path edit(Dialog owner, String title, Properties start, RateService service, MarketSettings settings)
	{
		DefinitionEditorDialog dialog = new DefinitionEditorDialog(owner, title, start, service, settings);
		dialog.setVisible(true);
		return dialog.saved;
	}

	/** A starting point for a new source. */
	static Properties blank()
	{
		Properties p = new Properties();
		p.setProperty("id", "my-source");
		p.setProperty("name", "My source");
		p.setProperty("type", ProviderDefinition.TYPE_JSON_API);
		p.setProperty("currency", "USD");
		return p;
	}
}
