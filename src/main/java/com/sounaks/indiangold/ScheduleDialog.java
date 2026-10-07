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

import com.sounaks.indiangold.rates.RateProvider;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;

/**
 * Chooses when a source with a monthly request quota is updated automatically: up to three times a day.
 * @author Sounak Choudhury
 */
final class ScheduleDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	static final int MAX_TIMES = 3;

	private final List<JCheckBox> enabled = new ArrayList<>();
	private final List<JSpinner> times = new ArrayList<>();
	private final JLabel budget = new JLabel();
	private final RateProvider provider;
	private List<LocalTime> result;

	private ScheduleDialog(Dialog owner, RateProvider provider, List<LocalTime> current)
	{
		super(owner, "Update schedule: " + provider.name(), true);
		this.provider = provider;
		JPanel rows = new JPanel(new GridLayout(MAX_TIMES, 1, 0, 4));
		for(int i = 0; i < MAX_TIMES; i++)
		{
			LocalTime time = i < current.size() ? current.get(i) : LocalTime.of(10 + 3 * i, 30);
			JCheckBox on = new JCheckBox("Update at", i < current.size());
			JSpinner spinner = new JSpinner(new SpinnerDateModel(toDate(time), null, null, Calendar.MINUTE));
			spinner.setEditor(new JSpinner.DateEditor(spinner, "HH:mm"));
			on.addActionListener(e -> {
				spinner.setEnabled(on.isSelected());
				updateBudget();
			});
			spinner.setEnabled(on.isSelected());
			JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING));
			row.add(on);
			row.add(spinner);
			rows.add(row);
			enabled.add(on);
			times.add(spinner);
		}
		JLabel intro = new JLabel("<html>" + provider.name() + " allows " + provider.monthlyQuota() + " requests a month. It is updated at these times<br>"
				+ "(or once when IndianGold starts, if a time was missed). Manual updates use what is left.</html>");
		JButton ok = new JButton("OK");
		ok.addActionListener(e -> {
			result = new ArrayList<>();
			for(int i = 0; i < MAX_TIMES; i++)
			{
				LocalTime time = toTime((Date)times.get(i).getValue());
				if(enabled.get(i).isSelected() && !result.contains(time)) result.add(time);
			}
			dispose();
		});
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		buttons.add(ok);
		buttons.add(cancel);
		JPanel content = new JPanel(new BorderLayout(6, 6));
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		content.add(intro, BorderLayout.NORTH);
		content.add(rows, BorderLayout.CENTER);
		content.add(budget, BorderLayout.SOUTH);
		add(content, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(ok);
		updateBudget();
		pack();
		setLocationRelativeTo(owner);
	}

	private void updateBudget()
	{
		long perDay = enabled.stream().filter(JCheckBox::isSelected).count() * provider.requestsPerFetch();
		int days = LocalDate.now().lengthOfMonth();
		long scheduled = perDay * days;
		long left = provider.monthlyQuota() - scheduled;
		budget.setText("<html>" + perDay + " update" + (perDay == 1 ? "" : "s") + " a day \u00d7 " + days + " days = " + scheduled + " of "
				+ provider.monthlyQuota() + " requests; "
				+ (left >= 0 ? "about " + left + " left for manual updates this month."
						: "<font color=red>" + (-left) + " more than the quota allows; the last days of the month would get no updates.</font>")
				+ "</html>");
	}

	private static Date toDate(LocalTime time)
	{
		return Date.from(LocalDate.now().atTime(time).atZone(ZoneId.systemDefault()).toInstant());
	}

	private static LocalTime toTime(Date date)
	{
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
	}

	/**
	 * Shows the dialog.
	 * @return The chosen times, or null if cancelled; an empty list means no automatic updates.
	 */
	static List<LocalTime> ask(Dialog owner, RateProvider provider, List<LocalTime> current)
	{
		ScheduleDialog dialog = new ScheduleDialog(owner, provider, current);
		dialog.setVisible(true);
		return dialog.result;
	}
}
