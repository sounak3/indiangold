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
import java.awt.Component;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * How units and price dates are shown to the user.
 * @author Sounak Choudhury
 */
final class UnitDisplay
{
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy");
	private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm");

	private UnitDisplay()
	{
	}

	/** Shows a unit by its name in the unit list, e.g. "troy ounce (oz t)". */
	static final class ListRenderer extends DefaultListCellRenderer
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
		{
			return super.getListCellRendererComponent(list, value instanceof MassUnit unit ? MarketSettings.unitName(unit) : value, index, selected, focus);
		}
	}

	/**
	 * Formats when a price is valid. Sources that give only a date (e.g. LME prices of the day) are stored as midnight
	 * UTC; those are shown as a date, without a misleading local time.
	 * @param asOf The time.
	 * @return For example "6 Oct 2026" or "7 Oct 2026 11:28".
	 */
	static String asOf(Instant asOf)
	{
		if(asOf.atZone(ZoneOffset.UTC).toLocalTime().equals(LocalTime.MIDNIGHT)) return DAY.format(asOf.atZone(ZoneOffset.UTC));
		return DAY_TIME.format(asOf.atZone(ZoneId.systemDefault()));
	}
}
