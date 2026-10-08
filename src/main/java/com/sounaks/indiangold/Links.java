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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Opens web pages, folders and e-mail in the user's programs, and makes link-style buttons.
 * <p>
 * On Linux this never uses java.awt.Desktop: it calls GTK inside the app's own process, and when the app inherits
 * a snap environment (e.g. started from a snap-packaged IDE) GTK loads incompatible libraries and the whole app
 * dies. xdg-open runs as a separate process instead, so a failure there can't take the app down.
 * @author Sounak Choudhury
 */
final class Links
{
	private static final boolean LINUX = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
	/** Variables a snap-packaged parent leaves behind that make GTK programs load the snap's libraries. */
	private static final List<String> SNAP_GTK_VARIABLES = List.of("GTK_PATH", "GTK_EXE_PREFIX", "GTK_IM_MODULE_FILE", "GTK3_MODULES",
			"GIO_MODULE_DIR", "GDK_PIXBUF_MODULEDIR", "GDK_PIXBUF_MODULE_FILE", "GSETTINGS_SCHEMA_DIR", "LOCPATH");

	private Links()
	{
	}

	/** Opens a web page or mailto: address; if that is not possible, shows it so it can be copied. */
	static void open(Component parent, URI target)
	{
		launch(parent, target.toString(), () -> {
			if(target.getScheme() != null && target.getScheme().equals("mailto")) Desktop.getDesktop().mail(target);
			else Desktop.getDesktop().browse(target);
		});
	}

	/** Opens a folder in the file manager; if that is not possible, shows its path. */
	static void openFolder(Component parent, File folder)
	{
		launch(parent, folder.getAbsolutePath(), () -> Desktop.getDesktop().open(folder));
	}

	private interface DesktopCall
	{
		void run() throws IOException;
	}

	private static void launch(Component parent, String target, DesktopCall desktopCall)
	{
		if(LINUX)
		{
			try
			{
				ProcessBuilder builder = new ProcessBuilder("xdg-open", target);
				cleanSnapEnvironment(builder.environment());
				builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
				return;
			}
			catch(IOException e)
			{
				showAddress(parent, target);
				return;
			}
		}
		// Windows and macOS: Desktop is reliable there; run it off the Swing thread so a slow browser start can't freeze the window.
		Thread opener = new Thread(() -> {
			try
			{
				if(!Desktop.isDesktopSupported()) throw new IOException("not supported");
				desktopCall.run();
			}
			catch(IOException | RuntimeException e)
			{
				SwingUtilities.invokeLater(() -> showAddress(parent, target));
			}
		}, "open-link");
		opener.setDaemon(true);
		opener.start();
	}

	/**
	 * Removes what a snap-packaged parent process leaves in the environment, so programs started from here use the
	 * system's own libraries, and restores the XDG directories the snap replaced.
	 * @param environment The environment of the process to start.
	 */
	static void cleanSnapEnvironment(Map<String, String> environment)
	{
		for(String name : SNAP_GTK_VARIABLES)
		{
			String value = environment.get(name);
			if(value != null && value.contains("/snap/")) environment.remove(name);
		}
		for(String name : List.copyOf(environment.keySet()))
		{
			// e.g. XDG_DATA_DIRS_VSCODE_SNAP_ORIG holds the value before the snap changed XDG_DATA_DIRS
			int marker = name.indexOf("_VSCODE_SNAP_ORIG");
			if(marker > 0) environment.put(name.substring(0, marker), environment.get(name));
		}
	}

	private static void showAddress(Component parent, String target)
	{
		JTextField address = new JTextField(target);
		address.setEditable(false);
		JOptionPane.showMessageDialog(parent, new Object[] { "Open this in your browser or file manager:", address }, "Open", JOptionPane.INFORMATION_MESSAGE);
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
