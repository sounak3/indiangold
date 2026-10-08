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

/**
 *
 * @author Sounak Choudhury
 */
import javax.swing.*;
import java.awt.*;

public class CheckboxListRenderer extends JCheckBox implements ListCellRenderer<Object>
{
	public CheckboxListRenderer()
	{
            init();
	}
	
        private void init()
        {
            setBackground(UIManager.getColor("List.textBackground"));
            setForeground(UIManager.getColor("List.textForeground"));
        }
        
        @Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus)
	{
		setEnabled(list.isEnabled());
		setSelected(((CheckableItem)value).isSelected());
		setFont(list.getFont());
		setText(value.toString());
		if(isSelected)
		{
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else
		{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		return this;
	}

        /**
         * Checks whether a point in the list is on the check box of a unit, as opposed to its name or empty space.
         * Only clicks on the check box toggle a unit; a click on the name just selects it.
         * @param list The unit list.
         * @param point The clicked point, in list coordinates.
         * @return True if the point is on the check box of a list entry.
         */
        static boolean isOnCheckBox(JList<?> list, Point point)
        {
            int index = list.locationToIndex(point);
            if(index < 0) return false;
            Rectangle cell = list.getCellBounds(index, index);
            if(cell == null || !cell.contains(point)) return false;
            Icon box = UIManager.getIcon("CheckBox.icon");
            int boxWidth = (box == null ? 16 : box.getIconWidth()) + new JCheckBox().getInsets().left + 4;
            return point.x - cell.x < boxWidth;
        }
}
