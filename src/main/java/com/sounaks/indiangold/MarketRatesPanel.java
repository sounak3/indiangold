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
import com.sounaks.indiangold.rates.Metal;
import com.sounaks.indiangold.rates.ProviderRegistry;
import com.sounaks.indiangold.rates.ProviderSettings;
import com.sounaks.indiangold.rates.Quote;
import com.sounaks.indiangold.rates.RateException;
import com.sounaks.indiangold.rates.RateProvider;
import com.sounaks.indiangold.rates.RateService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

/**
 * The "Market rates" tab of the settings window: which sources to use and in which order, each source's own
 * settings (API key, schedule, manual prices), how often free sources are updated, and the units rates are shown in.
 * Changes go into the settings at once; the settings window's OK saves them and Cancel drops them.
 * @author Sounak Choudhury
 */
final class MarketRatesPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d MMM HH:mm");
	private static final int[] INTERVALS = { 0, 5, 10, 15, 30, 60 };
	/** The plug-in guide in the repository, docs/PLUGINS.md. */
	static final java.net.URI PLUGIN_GUIDE = java.net.URI.create("https://github.com/sounak3/indiangold/blob/master/docs/PLUGINS.md");

	private final JDialog owner;
	private final transient MarketSettings settings;
	private final transient RateService service;
	private final List<RateService.SourceChoice> sources = new ArrayList<>();
	private final SourcesModel model = new SourcesModel();
	private final JTable table = new JTable(model);
	private final JPanel details = new JPanel(new BorderLayout());
	private final JLabel unitsSummary = new JLabel();
	private boolean manualPricesChanged;

	MarketRatesPanel(JDialog owner, FileOperations ops, MarketSettings settings, RateService service)
	{
		super(new BorderLayout(6, 6));
		this.owner = owner;
		this.settings = settings;
		this.service = service;
		setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		// Sources in the user's order, then any new ones (e.g. a plug-in just installed), switched off.
		sources.addAll(settings.sources());
		for(ProviderRegistry.Entry entry : service.registry().entries())
			if(sources.stream().noneMatch(s -> s.id().equals(entry.provider().id()))) sources.add(new RateService.SourceChoice(entry.provider().id(), false));
		sources.removeIf(s -> service.registry().find(s.id()).isEmpty());

		table.setRowHeight(table.getRowHeight() + 6);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(0).setMaxWidth(40);
		table.getColumnModel().getColumn(1).setPreferredWidth(170);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);
		table.getColumnModel().getColumn(3).setPreferredWidth(230);
		table.setPreferredScrollableViewportSize(new Dimension(560, table.getRowHeight() * 6));
		table.getSelectionModel().addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting()) showDetails();
		});
		JButton up = new JButton("Move up");
		up.setToolTipText("Sources higher in the list are used first for each metal");
		up.addActionListener(e -> move(-1));
		JButton down = new JButton("Move down");
		down.addActionListener(e -> move(1));
		JButton reload = new JButton("Reload plug-ins");
		reload.setToolTipText("Looks for new or changed plug-in jars in the plug-ins folder");
		reload.addActionListener(e -> reloadSources());
		JButton plugins = new JButton("Plug-ins folder...");
		plugins.setToolTipText("<html>Rate sources can be added as plug-in jar files in this folder.<br>Plug-ins can run code on your computer: only use plug-ins you trust.</html>");
		plugins.addActionListener(e -> openPluginsFolder());
		JPanel order = new JPanel(new GridLayout(4, 1, 0, 4));
		order.add(up);
		order.add(down);
		order.add(reload);
		order.add(plugins);
		JPanel orderHolder = new JPanel(new BorderLayout());
		orderHolder.add(order, BorderLayout.NORTH);
		JPanel list = new JPanel(new BorderLayout(6, 0));
		list.setBorder(BorderFactory.createTitledBorder("Sources (for each metal, the first one switched on with a recent price is used)"));
		list.add(new JScrollPane(table), BorderLayout.CENTER);
		list.add(orderHolder, BorderLayout.EAST);

		details.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		// Sources differ in how much they show; a fixed height with a scroll bar keeps the dialog steady.
		JScrollPane detailsScroll = new JScrollPane(details, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		detailsScroll.setBorder(BorderFactory.createTitledBorder("Selected source"));
		detailsScroll.setPreferredSize(new Dimension(600, 250));
		detailsScroll.getVerticalScrollBar().setUnitIncrement(12);

		JComboBox<String> interval = new JComboBox<>();
		int current = settings.autoRefreshMinutes();
		int selected = -1;
		for(int i = 0; i < INTERVALS.length; i++)
		{
			interval.addItem(INTERVALS[i] == 0 ? "only when I ask (and at start)" : "every " + INTERVALS[i] + " minutes");
			if(INTERVALS[i] == current) selected = i;
		}
		if(selected < 0)
		{
			interval.addItem("every " + current + " minutes");
			selected = interval.getItemCount() - 1;
		}
		interval.setSelectedIndex(selected);
		interval.addActionListener(e -> {
			if(interval.getSelectedIndex() < INTERVALS.length) settings.setAutoRefreshMinutes(INTERVALS[interval.getSelectedIndex()]);
		});
		JPanel updates = new JPanel(new FlowLayout(FlowLayout.LEADING));
		updates.add(new JLabel("Update free sources"));
		updates.add(interval);
		updates.add(new JLabel("<html><font color=gray>(web pages at most hourly; limited sources follow their schedule)</font></html>"));

		JButton customize = new JButton("Customize rates...");
		customize.setToolTipText("Choose the quantity and unit for each metal group, and the gold rows (24K, 22K, ...)");
		customize.addActionListener(e -> {
			if(CustomizeRatesDialog.show(owner, ops, settings, service)) refreshSummary();
		});
		JPanel units = new JPanel(new BorderLayout(10, 0));
		units.setBorder(BorderFactory.createTitledBorder("Rates are shown in"));
		units.add(unitsSummary, BorderLayout.CENTER);
		units.add(customize, BorderLayout.EAST);
		refreshSummary();

		JPanel help = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		help.add(new JLabel("Developers: "));
		help.add(Links.button("How to write a plug-in", PLUGIN_GUIDE));
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(help, BorderLayout.CENTER);
		bottom.add(updates, BorderLayout.NORTH);
		bottom.add(units, BorderLayout.SOUTH);
		add(list, BorderLayout.NORTH);
		add(detailsScroll, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);
		if(!sources.isEmpty()) table.setRowSelectionInterval(0, 0);
	}

	/** Whether the user entered manual prices, so they should be read now. */
	boolean manualPricesChanged()
	{
		return manualPricesChanged;
	}

	void refreshSummary()
	{
		unitsSummary.setText(CustomizeRatesDialog.summary(settings));
	}

	private RateProvider provider(int row)
	{
		return service.registry().find(sources.get(row).id()).orElseThrow().provider();
	}

	/**
	 * Refreshes the source list without losing the selection (a full table refresh clears it, which left the details
	 * area empty after Test). With nothing selected, the first source switched on is selected.
	 */
	private void refreshTable()
	{
		int row = table.getSelectedRow();
		model.fireTableDataChanged();
		for(int i = 0; row < 0 && i < sources.size(); i++) if(sources.get(i).enabled()) row = i;
		if(row < 0 && !sources.isEmpty()) row = 0;
		if(row >= 0 && row < sources.size())
		{
			table.setRowSelectionInterval(row, row); // also shows the details
			table.scrollRectToVisible(table.getCellRect(row, 0, true));
		}
	}

	private void move(int direction)
	{
		int row = table.getSelectedRow();
		int target = row + direction;
		if(row < 0 || target < 0 || target >= sources.size()) return;
		sources.add(target, sources.remove(row));
		settings.setSources(sources);
		model.fireTableDataChanged();
		table.setRowSelectionInterval(target, target);
	}

	private void reloadSources()
	{
		int row = table.getSelectedRow();
		service.registry().reload();
		service.registry().problems().forEach(problem -> System.out.println("Rate source not loaded: " + problem));
		sources.removeIf(s -> service.registry().find(s.id()).isEmpty());
		int added = 0;
		for(ProviderRegistry.Entry entry : service.registry().entries())
		{
			if(sources.stream().noneMatch(s -> s.id().equals(entry.provider().id())))
			{
				sources.add(new RateService.SourceChoice(entry.provider().id(), false)); // new plug-ins start switched off
				added++;
			}
		}
		settings.setSources(sources);
		model.fireTableDataChanged();
		if(!sources.isEmpty()) table.setRowSelectionInterval(Math.min(Math.max(row, 0), sources.size() - 1), Math.min(Math.max(row, 0), sources.size() - 1));
		showDetails();
		List<String> problems = service.registry().problems();
		if(added > 0 || !problems.isEmpty())
			message("<html>" + (added == 0 ? "No new sources found." : added + " new source" + (added == 1 ? "" : "s") + " added, switched off; switch "
					+ (added == 1 ? "it" : "them") + " on to use.") + (problems.isEmpty() ? "" : "<br><br><b>Not loaded:</b><br>"
					+ String.join("<br>", problems.stream().map(RateBar::escape).toList())) + "</html>");
	}

	private void openPluginsFolder()
	{
		Optional<java.nio.file.Path> folder = service.registry().pluginsFolder();
		if(folder.isEmpty()) return;
		try
		{
			java.nio.file.Files.createDirectories(folder.get());
		}
		catch(java.io.IOException e)
		{
			message("Cannot create " + folder.get() + ": " + e.getMessage());
			return;
		}
		Links.openFolder(owner, folder.get().toFile());
	}


	private void setEnabled(int row, boolean on)
	{
		RateProvider provider = provider(row);
		if(on && provider.isWebPage() && !settings.webDisclaimerAccepted())
		{
			int answer = JOptionPane.showConfirmDialog(owner, new JLabel(CountryDialog.WEB_DISCLAIMER), "Use a web page source?",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if(answer != JOptionPane.OK_OPTION) return;
			settings.acceptWebDisclaimer();
		}
		sources.set(row, new RateService.SourceChoice(provider.id(), on));
		settings.setSources(sources);
		model.fireTableRowsUpdated(row, row);
		showDetails();
	}

	private static String kind(ProviderRegistry.Entry entry)
	{
		RateProvider p = entry.provider();
		if(p.isManual()) return "Manual";
		String kind = p.isWebPage() ? "web page" : p.needsApiKey() ? "API, your key" : "API, free";
		if(entry.origin() == ProviderRegistry.Origin.PLUGIN) return "Plug-in, " + kind;
		return Character.toUpperCase(kind.charAt(0)) + kind.substring(1);
	}

	private String status(RateService.SourceChoice choice)
	{
		RateProvider provider = service.registry().find(choice.id()).orElseThrow().provider();
		if(provider.needsApiKey() && settings.providerSettings(provider.id()).apiKey().isEmpty()) return "Needs your API key";
		RateService.Status status = service.status(provider);
		if(!choice.enabled()) return "Off";
		if(status.lastError().isPresent()) return "Problem: " + status.lastError().get();
		if(status.lastSuccess().isPresent()) return "Updated " + TIME.format(status.lastSuccess().get().atZone(ZoneId.systemDefault()));
		return "Not updated yet";
	}

	private void showDetails()
	{
		details.removeAll();
		int row = table.getSelectedRow();
		if(row < 0)
		{
			details.revalidate();
			details.repaint();
			return;
		}
		ProviderRegistry.Entry entry = service.registry().find(sources.get(row).id()).orElseThrow();
		RateProvider provider = entry.provider();
		ProviderSettings providerSettings = settings.providerSettings(provider.id());
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		List<JComponent> lines = new ArrayList<>();

		lines.add(new JLabel("<html><div style='width:520px'><b>" + RateBar.escape(provider.name()) + "</b> \u2014 " + RateBar.escape(provider.description())
				+ "<br><font color=gray>Metals: " + String.join(", ", provider.metals().stream().map(Metal::displayName).toList())
				+ (provider.providesFx() ? "; exchange rates" : "") + "</font></div></html>"));

		JPanel links = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		provider.termsPage().ifPresent(page -> links.add(Links.button("Terms of use", page)));
		entry.file().ifPresent(jar -> links.add(new JLabel("<html><font color=gray>&nbsp;&nbsp;Plug-in " + RateBar.escape(jar.getFileName().toString())
				+ " (plug-ins can run code on your computer)</font></html>")));
		if(links.getComponentCount() > 0) lines.add(links);

		if(provider.isWebPage())
			lines.add(new JLabel("<html><font color=#a06000>Reads a web page at most once an hour, where its robots.txt allows. Check the site's terms; personal use only.</font></html>"));

		if(provider.needsApiKey())
		{
			JPasswordField key = new JPasswordField(providerSettings.apiKey().orElse(""), 28);
			char echo = key.getEchoChar();
			key.getDocument().addDocumentListener(onChange(() -> {
				providerSettings.put(ProviderSettings.API_KEY, new String(key.getPassword()).trim());
				model.fireTableRowsUpdated(row, row);
			}));
			JCheckBox show = new JCheckBox("Show");
			show.addActionListener(e -> key.setEchoChar(show.isSelected() ? (char)0 : echo));
			JPanel keyRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
			keyRow.add(new JLabel("API key:"));
			keyRow.add(key);
			keyRow.add(show);
			provider.signupPage().ifPresent(page -> {
				JButton get = new JButton("Get a free key...");
				get.setToolTipText("Opens " + page + "; create an account there and copy the key shown on its dashboard");
				get.addActionListener(e -> Links.open(owner, page));
				keyRow.add(get);
			});
			lines.add(keyRow);
			provider.signupHint().ifPresent(hint -> lines.add(new JLabel("<html><div style='width:520px'><font color=#a06000>" + RateBar.escape(hint) + "</font></div></html>")));
			lines.add(new JLabel("<html><font color=gray>The key is kept in your own settings file on this computer. Most providers allow one free account per person.</font></html>"));
		}

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
		if(provider.monthlyQuota() > 0)
		{
			var budget = service.budget(provider);
			List<java.time.LocalTime> times = settings.schedule(provider.id()).orElse(provider.defaultSchedule());
			JSpinner quota = new JSpinner(new SpinnerNumberModel(service.quotaOf(provider), 1, 10_000_000, 1));
			quota.setToolTipText("<html>The requests per month your plan allows. Metals.Dev's free plan allows 100,<br>"
					+ "or 25 for accounts that sign in with an access code. \"Check usage\" fills this in.</html>");
			quota.addChangeListener(e -> {
				providerSettings.put(RateService.QUOTA_SETTING, String.valueOf(quota.getValue()));
				SwingUtilities.invokeLater(this::showDetails);
			});
			JPanel quotaRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
			quotaRow.add(new JLabel("Your plan allows"));
			quotaRow.add(quota);
			quotaRow.add(new JLabel("requests a month."));
			lines.add(quotaRow);
			lines.add(new JLabel("<html>Used this month: " + budget.usedThisMonth() + " of " + budget.monthlyQuota()
					+ "; today's share: " + budget.remainingToday() + " left (scheduled updates stay within it).<br>Automatic updates: "
					+ (times.isEmpty() ? "off" : String.join(", ", times.stream().map(Object::toString).toList())) + "</html>"));
			JButton schedule = new JButton("Schedule...");
			schedule.addActionListener(e -> {
				List<java.time.LocalTime> chosen = ScheduleDialog.ask(owner, provider, service.quotaOf(provider), times);
				if(chosen != null)
				{
					settings.setSchedule(provider.id(), chosen);
					showDetails();
				}
			});
			actions.add(schedule);
			JButton usage = new JButton("Check usage");
			usage.setToolTipText("Asks " + provider.name() + " how many requests are used, including from other computers with the same key");
			usage.addActionListener(e -> run(usage, service.usage(provider.id()), result -> {
				if(result.isEmpty()) message(provider.name() + " does not report its usage.");
				else message(provider.name() + ": " + result.get().used() + " of " + result.get().total() + " requests used this month"
						+ (result.get().plan().isEmpty() ? "." : " (plan: " + result.get().plan() + ")."));
				showDetails();
			}));
			actions.add(usage);
		}
		if(provider.isManual())
		{
			JButton enter = new JButton("Enter prices...");
			enter.addActionListener(e -> {
				if(ManualRatesDialog.show(owner, settings, service.current().fx()))
				{
					manualPricesChanged = true;
					if(!sources.get(row).enabled()) setEnabled(row, true);
				}
			});
			actions.add(enter);
		}
		else
		{
			JButton test = new JButton("Test");
			test.setToolTipText("Fetches prices from this source now and shows them");
			test.addActionListener(e -> test(test, provider, providerSettings, row));
			actions.add(test);
		}
		lines.add(actions);

		RateService.Status status = service.status(provider);
		status.lastError().ifPresent(error -> {
			JLabel problem = new JLabel("<html><div style='width:520px'>Last problem: " + RateBar.escape(error) + "</div></html>");
			problem.setForeground(Color.red.darker());
			lines.add(problem);
		});

		for(JComponent line : lines)
		{
			line.setAlignmentX(LEFT_ALIGNMENT);
			content.add(line);
			content.add(Box.createVerticalStrut(4));
		}
		details.add(content, BorderLayout.NORTH);
		details.revalidate();
		details.repaint();
	}

	private void test(JButton button, RateProvider provider, ProviderSettings providerSettings, int row)
	{
		if(provider.needsApiKey() && providerSettings.apiKey().isEmpty())
		{
			message("Enter the API key first" + provider.signupPage().map(page -> "; \"Get a free key...\" opens " + page).orElse("") + ".");
			return;
		}
		if(provider.monthlyQuota() > 0)
		{
			int left = service.budget(provider).remainingThisMonth();
			int answer = JOptionPane.showConfirmDialog(owner, "Testing uses " + provider.requestsPerFetch() + " of the " + left
					+ " requests left this month. Test now?", "Test " + provider.name(), JOptionPane.OK_CANCEL_OPTION);
			if(answer != JOptionPane.OK_OPTION) return;
		}
		run(button, service.test(provider, providerSettings), snapshot -> {
			StringBuilder text = new StringBuilder("<html><b>" + RateBar.escape(provider.name()) + " works.</b><br><br>");
			FxRates fx = snapshot.fx().orElse(FxRates.usdOnly());
			for(Metal metal : Metal.values())
			{
				Quote quote = snapshot.quotes().get(metal);
				if(quote == null) continue;
				MarketSettings.DisplayUnit unit = settings.unit(quote.metal().group());
				NumberFormat format = NumberFormat.getNumberInstance();
				format.setMaximumFractionDigits(2);
				text.append(quote.metal().displayName()).append(": ").append(format.format(quote.priceIn(fx, quote.currency(), unit.totalGrams(), 1)))
						.append(' ').append(quote.currency()).append(" per ").append(RateBar.escape(unit.label())).append("<br>");
			}
			if(snapshot.fx().isPresent()) text.append("Exchange rates for ").append(fx.perUsd().size()).append(" currencies<br>");
			boolean off = !sources.get(row).enabled();
			if(off) text.append("<br>Switch this source on?");
			text.append("</html>");
			if(off)
			{
				if(JOptionPane.showConfirmDialog(owner, text.toString(), "Test " + provider.name(), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
					setEnabled(row, true);
			}
			else
			{
				message(text.toString());
			}
			showDetails();
		});
	}

	/** Runs a background task with the button disabled, then shows its result or error on the Swing thread. */
	private <T> void run(JButton button, java.util.concurrent.CompletableFuture<T> task, java.util.function.Consumer<T> onSuccess)
	{
		button.setEnabled(false);
		owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		task.whenComplete((result, failure) -> SwingUtilities.invokeLater(() -> {
			button.setEnabled(true);
			owner.setCursor(Cursor.getDefaultCursor());
			refreshTable();
			if(failure == null)
			{
				onSuccess.accept(result);
				refreshTable();
				table.requestFocusInWindow();
				return;
			}
			Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
			String hint = cause instanceof RateException re ? switch(re.kind())
			{
				case API_KEY -> "\nCheck the API key.";
				case QUOTA -> "\nThe source's request limit is used up for now.";
				case NETWORK -> "\nCheck the internet connection, or try again later.";
				case NOT_ALLOWED -> "\nThe site does not allow automated access to this page.";
				case UNEXPECTED_RESPONSE -> "\nThe page or API may have changed.";
				case CONFIGURATION -> "";
			} : "";
			JOptionPane.showMessageDialog(owner, cause.getMessage() + hint, "Problem", JOptionPane.WARNING_MESSAGE);
			refreshTable();
			table.requestFocusInWindow();
		}));
	}

	private void message(String text)
	{
		JOptionPane.showMessageDialog(owner, text, "Market rates", JOptionPane.INFORMATION_MESSAGE);
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

	private final class SourcesModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final String[] columns = { "On", "Source", "Type", "Status" };

		@Override
		public int getRowCount()
		{
			return sources.size();
		}

		@Override
		public int getColumnCount()
		{
			return columns.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return columns[column];
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			return column == 0 ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return column == 0;
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			RateService.SourceChoice choice = sources.get(row);
			Optional<ProviderRegistry.Entry> entry = service.registry().find(choice.id());
			return switch(column)
			{
				case 0 -> choice.enabled();
				case 1 -> entry.map(e -> e.provider().name()).orElse(choice.id());
				case 2 -> entry.map(MarketRatesPanel::kind).orElse("");
				default -> status(choice);
			};
		}

		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if(column == 0) setEnabled(row, Boolean.TRUE.equals(value));
		}
	}
}
