/*
 * Copyright (C) 2021 Sounak
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
import com.sounaks.indiangold.rates.Quote;
import com.sounaks.indiangold.rates.RateProvider;
import com.sounaks.indiangold.rates.RateService;
import java.awt.*;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * The market rates bar: one row per gold purity, silver, platinum and palladium, and the base metals the enabled
 * sources offer. Prices come from the {@link RateService} and are shown in the user's currency and units.
 * @author Sounak Choudhury
 */
public class RateBar extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Duration STALE_AFTER = Duration.ofHours(48);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d-MMM HH:mm");

	private final transient RateService service;
	private final transient MarketSettings settings;
	private final transient FileOperations fileOps;
	private final JPanel rowsPanel;
	private final RateLabel header;
	private final JLabel side;
	private final List<RateLabel> rows = new ArrayList<>();
	private final List<MouseListener> mouseListeners = new ArrayList<>();
	private Border borderType;
	private boolean updating;
	private transient Runnable sizeChanged = () -> { };
	private Dimension shownSize;

	RateBar(RateService service, MarketSettings settings, FileOperations fileOps, Border borderType)
	{
		super(new BorderLayout());
		this.service = service;
		this.settings = settings;
		this.fileOps = fileOps;
		this.borderType = borderType;
		rowsPanel = new JPanel(new GridLayout(0, 1));
		header = new RateLabel(null, "", 1, null);
		side = new JLabel();
		side.setForeground(Color.red.darker());
		side.setHorizontalAlignment(SwingConstants.CENTER);
		init();
		service.addListener(rates -> SwingUtilities.invokeLater(() -> {
			updating = false;
			show(rates);
		}));
		rebuild();
	}

	private void init()
	{
		add(Box.createVerticalStrut(4), BorderLayout.NORTH);
		add(rowsPanel, BorderLayout.CENTER);
		add(side, BorderLayout.EAST);
	}

	/** Called when new contents change the bar's preferred size, so the window can fit itself to it. */
	void setSizeChangedListener(Runnable listener)
	{
		sizeChanged = listener == null ? () -> { } : listener;
	}

	/** Redraws the bar after a settings change (currency, units, purities, sources). */
	public void rebuild()
	{
		show(service.current());
	}

	/** True while a refresh asked for by the user is running. */
	public boolean isUpdating()
	{
		return updating;
	}

	/**
	 * Fetches new prices now. If a source with a monthly quota is enabled, the user is first told how many updates
	 * are left today and asked whether to use one.
	 * @param parent The window to show the question over.
	 */
	public void refreshNow(Component parent)
	{
		List<RateProvider> limited = new ArrayList<>();
		for(RateService.SourceChoice choice : settings.sources())
		{
			if(!choice.enabled()) continue;
			service.registry().find(choice.id()).map(ProviderRegistry.Entry::provider)
					.filter(p -> p.monthlyQuota() > 0 && (!p.needsApiKey() || settings.providerSettings(p.id()).apiKey().isPresent()))
					.ifPresent(limited::add);
		}
		boolean includeLimited = false;
		if(!limited.isEmpty())
		{
			StringBuilder message = new StringBuilder("<html>");
			for(RateProvider provider : limited)
			{
				var budget = service.budget(provider);
				message.append("<b>").append(provider.name()).append("</b>: ");
				if(budget.remainingToday() > 0)
					message.append(budget.remainingToday()).append(budget.remainingToday() == 1 ? " update" : " updates").append(" left today");
				else
					message.append("<font color=red>today's share is used up</font>; updating now uses tomorrow's");
				message.append(" (").append(budget.remainingThisMonth()).append(" of ").append(budget.monthlyQuota()).append(" left this month).<br>");
			}
			message.append("<br>Update these too? Free sources are updated either way.</html>");
			int answer = JOptionPane.showOptionDialog(parent, message.toString(), "Update market rates", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, new String[] { "Update all", "Free sources only", "Cancel" }, "Free sources only");
			if(answer == 2 || answer == JOptionPane.CLOSED_OPTION) return;
			includeLimited = answer == 0;
		}
		updating = true;
		header.setText("Updating\u2026", "please wait");
		service.refreshNow(includeLimited);
	}

	private void show(RateService.Rates rates)
	{
		String currency = settings.currency();
		FxRates fx = rates.fx();
		boolean currencyKnown = fx.has(currency);
		Set<Metal> offered = EnumSet.noneOf(Metal.class);
		for(RateService.SourceChoice choice : settings.sources())
		{
			if(choice.enabled()) service.registry().find(choice.id()).ifPresent(entry -> offered.addAll(entry.provider().metals()));
		}
		offered.addAll(rates.quotes().keySet());

		rows.clear();
		rowsPanel.removeAll();
		for(CountryDefaults.Purity purity : settings.purities()) rows.add(row(Metal.GOLD, "Gold " + purity.label(), purity.fineness(), rates, currency));
		for(Metal metal : Metal.values())
		{
			if(metal == Metal.GOLD) continue;
			if(metal.isPrecious() || offered.contains(metal)) rows.add(row(metal, metal.displayName(), 1, rates, currency));
		}
		Instant newest = null;
		for(Quote quote : rates.quotes().values()) if(newest == null || quote.asOf().isAfter(newest)) newest = quote.asOf();

		if(updating) header.setText("Updating\u2026", "please wait");
		else if(newest == null) header.setText("No prices yet", "right-click to update");
		else header.setText("As of", TIME.format(newest.atZone(ZoneId.systemDefault())));
		header.setToolTipText(statusTooltip(currencyKnown, currency, fx));
		rowsPanel.add(header);
		rows.forEach(rowsPanel::add);
		side.setIcon(new RotatedIcon(new TextIcon(side, currencyKnown ? currency : "USD (no " + currency + " rate yet)"), RotatedIcon.Rotate.DOWN));
		applyBorders();
		updateToolTips();
		for(MouseListener listener : mouseListeners) attach(listener);
		revalidate();
		repaint();
		Dimension size = getPreferredSize();
		if(shownSize != null && !size.equals(shownSize) && isShowing()) sizeChanged.run();
		shownSize = size;
	}

	private RateLabel row(Metal metal, String title, double fineness, RateService.Rates rates, String currency)
	{
		MarketSettings.DisplayUnit unit = settings.unit(metal.group());
		RateLabel label = new RateLabel(metal, title, fineness, unit);
		Quote quote = rates.quotes().get(metal);
		if(quote == null) return label;
		String shownCurrency = rates.fx().has(currency) ? currency : "USD";
		double price = quote.priceIn(rates.fx(), shownCurrency, unit.totalGrams(), fineness);
		if(Double.isNaN(price)) return label;
		boolean stale = quote.asOf().isBefore(Instant.now().minus(STALE_AFTER));
		String source = service.registry().find(quote.providerId()).map(e -> e.provider().name()).orElse("an earlier version of IndianGold");
		label.setPrice(price, decimals(price, shownCurrency), stale);
		label.tooltipTemplate = "<html><b>" + escape(title) + "</b>" + (fineness < 1 ? " (" + percent(fineness) + " gold)" : "") + "<br>"
				+ label.displayRate + " " + shownCurrency + " per " + escape(unit.label()) + "<br>"
				+ "From " + escape(source) + ", as of " + UnitDisplay.asOf(quote.asOf())
				+ (stale ? "<br><font color=red>More than two days old</font>" : "")
				+ (fineness < 1 ? "<br><i>Calculated from the pure gold price; jewellers add premiums and taxes.</i>" : "")
				+ "<br><br>%HINT%</html>";
		return label;
	}

	private static String percent(double fineness)
	{
		DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
		return format.format(fineness * 100) + "%";
	}

	/** Enough decimals for small prices (e.g. tin per gram), otherwise the currency's usual number. */
	private static int decimals(double price, String currency)
	{
		int digits;
		try
		{
			digits = Math.max(0, Currency.getInstance(currency).getDefaultFractionDigits());
		}
		catch(IllegalArgumentException e)
		{
			digits = 2;
		}
		if(price < 1) return Math.max(digits, 4);
		if(price < 10) return Math.max(digits, 3);
		return digits;
	}

	private String statusTooltip(boolean currencyKnown, String currency, FxRates fx)
	{
		StringBuilder tip = new StringBuilder("<html><b>Market rates</b>");
		for(RateService.SourceChoice choice : settings.sources())
		{
			if(!choice.enabled()) continue;
			service.registry().find(choice.id()).map(ProviderRegistry.Entry::provider).ifPresent(provider -> {
				RateService.Status status = service.status(provider);
				tip.append("<br>").append(escape(provider.name())).append(": ");
				if(status.lastError().isPresent()) tip.append("<font color=red>").append(escape(status.lastError().get())).append("</font>");
				else if(status.lastSuccess().isPresent()) tip.append("updated ").append(TIME.format(status.lastSuccess().get().atZone(ZoneId.systemDefault())));
				else tip.append("not updated yet");
			});
		}
		if(!currencyKnown) tip.append("<br><font color=red>No exchange rate for ").append(currency).append(" yet; prices are in US dollars.</font>");
		else if(!currency.equals("USD")) tip.append("<br>1 USD = ").append(NumberFormat.getNumberInstance().format(fx.fromUsd(1, currency))).append(' ').append(currency);
		return tip.append("</html>").toString();
	}

	static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	@Override
	public void addMouseListener(MouseListener listener)
	{
		if(listener == null) return;
		mouseListeners.add(listener);
		attach(listener);
	}

	private void attach(MouseListener listener)
	{
		header.removeMouseListener(listener);
		header.addMouseListener(listener);
		for(RateLabel row : rows)
		{
			row.removeMouseListener(listener);
			row.addMouseListener(listener);
		}
		side.removeMouseListener(listener);
		side.addMouseListener(listener);
	}

	@Override
	public void removeMouseListener(MouseListener listener)
	{
		mouseListeners.remove(listener);
		if(header == null) return;
		header.removeMouseListener(listener);
		for(RateLabel row : rows) row.removeMouseListener(listener);
		side.removeMouseListener(listener);
	}

	@Override
	public void setBorder(Border border)
	{
		if(border == null || header == null) return;
		borderType = border;
		applyBorders();
	}

	private void applyBorders()
	{
		header.setBorder(borderType);
		rows.forEach(row -> row.setBorder(borderType));
		side.setBorder(borderType);
	}

	/** Updates the click hints in the tooltips after the click policy changed. */
	public void updateToolTips()
	{
		boolean singleClickFills = fileOps.getValue("$clickcondition", "1").equals("1");
		String hint = singleClickFills ? "Click to use this rate, double-click to show or hide the calculator,<br>right-click to update the rates."
				: "Double-click to use this rate, right-click to show or hide the calculator,<br>click to update the rates.";
		for(RateLabel row : rows)
		{
			if(row.tooltipTemplate != null) row.setToolTipText(row.tooltipTemplate.replace("%HINT%", hint));
			else row.setToolTipText("<html>No price for " + escape(row.title) + " yet.<br>" + hint + "</html>");
		}
	}

	/** One row of the bar: a metal (or gold purity) and its price; the header row has no metal. */
	class RateLabel extends JLabel
	{
		private static final long serialVersionUID = 1L;
		private final Metal metal;
		private final String title;
		private final double fineness;
		private final transient MarketSettings.DisplayUnit unit;
		private String displayRate = "\u2014";
		private String rawRate;
		private boolean stale;
		private String tooltipTemplate;

		RateLabel(Metal metal, String title, double fineness, MarketSettings.DisplayUnit unit)
		{
			this.metal = metal;
			this.title = title;
			this.fineness = fineness;
			this.unit = unit;
			setHorizontalAlignment(SwingConstants.CENTER);
			setBorder(borderType);
			if(metal != null) render(false);
		}

		void setPrice(double price, int decimals, boolean stale)
		{
			NumberFormat shown = NumberFormat.getNumberInstance();
			shown.setMinimumFractionDigits(decimals);
			shown.setMaximumFractionDigits(decimals);
			displayRate = shown.format(price);
			DecimalFormat raw = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
			raw.setMaximumFractionDigits(decimals);
			rawRate = raw.format(price);
			this.stale = stale;
			render(false);
		}

		/** For the header row: a grey caption and a small second line. */
		void setText(String line1, String line2)
		{
			super.setText("<html><center><font color=gray>" + escape(line1) + "</font><br><font size=-2>" + escape(line2) + "</font></center></html>");
		}

		private void render(boolean light)
		{
			if(metal == null) return;
			String nameColor = metal.isPrecious() ? (light ? "#e0a000" : "red") : (light ? "#00b0d0" : "blue");
			String priceColor = rawRate == null || stale ? "gray" : "black";
			super.setText("<html><center><font color=" + nameColor + ">" + escape(title) + "</font><br><font color=" + priceColor + ">"
					+ displayRate + "</font><font size=-2 color=gray> /" + escape(unit.label()) + "</font></center></html>");
		}

		public void glow()
		{
			render(true);
		}

		public void dim()
		{
			render(false);
		}

		/** The price as plain digits with "." for decimals, for the calculator; null if there is no price. */
		public String getRate()
		{
			return rawRate;
		}

		public Metal metal()
		{
			return metal;
		}

		public MarketSettings.DisplayUnit unit()
		{
			return unit;
		}

		@Override
		public String getName()
		{
			return title;
		}
	}
}
