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

public class CurrencyComboModel extends AbstractListModel implements ComboBoxModel
{
	CurrencyCode cCode;
	String itemList[];

    @Override
	public Object getElementAt(int index)
	{
		return itemList[index];
	}

    @Override
	public int getSize()
	{
		return cCode.size();
	}

	public CurrencyComboModel()
	{
		super();
		cCode = new CurrencyCode("INR");
		itemList = cCode.getCurrencyList();
	}

    @Override
	public void setSelectedItem(Object anItem)
	{
		cCode.setName((String)anItem);
	}

    @Override
	public Object getSelectedItem()
	{
		return cCode.getCode();
	}
}
