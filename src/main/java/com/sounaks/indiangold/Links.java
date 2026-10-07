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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Opens web pages in the browser, and makes link-style buttons.
 * @author Sounak Choudhury
 */
final class Links
{
	private Links()
	{
	}

	/** Opens a page; if the system cannot, shows the address so it can be copied. */
	static void open(Component parent, URI page)
	{
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
		{
			try
			{
				Desktop.getDesktop().browse(page);
				return;
			}
			catch(IOException | UnsupportedOperationException e)
			{
				// fall through to showing the address
			}
		}
		JTextField address = new JTextField(page.toString());
		address.setEditable(false);
		JOptionPane.showMessageDialog(parent, new Object[] { "Open this address in your browser:", address }, "Open web page", JOptionPane.INFORMATION_MESSAGE);
	}

	/** A button that looks like a link and opens the page. */
	static JButton button(String text, URI page)
	{
		JButton link = new JButton("<html><a href=''>" + text + "</a></html>");
		link.setBorderPainted(false);
		link.setContentAreaFilled(false);
		link.setFocusPainted(false);
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.setToolTipText(page.toString());
		link.addActionListener(e -> open(link, page));
		return link;
	}
}
