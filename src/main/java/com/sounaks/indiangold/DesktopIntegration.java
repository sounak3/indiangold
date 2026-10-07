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

import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Makes the app show up in the dock or taskbar with its own name and icon.
 * <p>
 * Linux docks show a window only when they can match it to a .desktop file, by the window's class. Java names the
 * class after the main class ("com-sounaks-indiangold-IndianGold"), which matches nothing, so the window was missing
 * from the dock even when minimized. The class is set to "IndianGold" instead, the installed .desktop file says
 * StartupWMClass=IndianGold, and when the plain jar runs the app writes such a .desktop file for itself.
 * @author Sounak Choudhury
 */
final class DesktopIntegration
{
	static final String WINDOW_CLASS = "IndianGold";
	private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
	private static final boolean LINUX = OS.contains("linux");
	private static final boolean MAC = OS.contains("mac");
	private static final int[] ICON_SIZES = { 16, 32, 48, 64, 128, 256 };

	private DesktopIntegration()
	{
	}

	/**
	 * Names the app's windows "IndianGold" for Linux docks. Call before the first window is created. Needs
	 * java.desktop/sun.awt.X11 opened (the jar's manifest does that; the installed app passes --add-opens).
	 */
	static void setWindowClass()
	{
		if(!LINUX) return;
		try
		{
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Field field = toolkit.getClass().getDeclaredField("awtAppClassName");
			field.setAccessible(true);
			field.set(null, WINDOW_CLASS);
		}
		catch(ReflectiveOperationException | RuntimeException e)
		{
			// Not opened (e.g. started from an IDE): the window keeps Java's class name.
			System.out.println("Cannot set the window class for the dock: " + e.getMessage());
		}
	}

	/** The app icon in several sizes, for title bars, task switchers and docks. */
	static List<Image> icons()
	{
		List<Image> icons = new ArrayList<>();
		for(int size : ICON_SIZES)
		{
			try(InputStream in = DesktopIntegration.class.getResourceAsStream("/icons/indiangold-" + size + ".png"))
			{
				if(in != null) icons.add(ImageIO.read(in));
			}
			catch(IOException e)
			{
				// skipped; other sizes still work
			}
		}
		return icons;
	}

	/**
	 * Gives a window the app icon; on macOS also the dock icon. (java.awt.Taskbar is not used on Linux: like
	 * java.awt.Desktop it loads GTK into the app, which can crash it; see {@link Links}.)
	 */
	static void applyIcons(Window window)
	{
		List<Image> icons = icons();
		if(icons.isEmpty()) return;
		window.setIconImages(icons);
		if(MAC && Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE))
			Taskbar.getTaskbar().setIconImage(icons.get(icons.size() - 1));
	}

	/**
	 * When the plain jar runs on Linux, writes ~/.local/share/applications/indiangold.desktop so the dock can show the
	 * window (and the app appears among the applications). Installed packages have their own .desktop file.
	 */
	static void registerJarLauncher()
	{
		if(!LINUX) return;
		try
		{
			Path jar = Path.of(DesktopIntegration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			String location = jar.toString();
			if(!location.endsWith(".jar") || location.contains("/lib/app/")) return; // an IDE build, or an installed package
			Path java = ProcessHandle.current().info().command().map(Path::of).orElse(null);
			if(java == null) return;
			Path home = Path.of(System.getProperty("user.home"));
			Path icon = home.resolve(FileOperations.DATA_DIR_NAME).resolve("indiangold.png");
			if(!Files.isRegularFile(icon))
			{
				try(InputStream in = DesktopIntegration.class.getResourceAsStream("/icons/indiangold-256.png"))
				{
					if(in == null) return;
					Files.createDirectories(icon.getParent());
					Files.copy(in, icon, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			String entry = "[Desktop Entry]\n"
					+ "Type=Application\n"
					+ "Name=IndianGold\n"
					+ "Comment=Weight and price calculator for Indian units (ratti, tola, bhori) with metal market rates\n"
					+ "Exec=" + quote(java.toString()) + " -jar " + quote(location) + "\n"
					+ "Icon=" + icon + "\n"
					+ "Terminal=false\n"
					+ "Categories=Utility;Calculator;\n"
					+ "StartupWMClass=" + WINDOW_CLASS + "\n";
			Path file = home.resolve(".local/share/applications/indiangold.desktop");
			if(Files.isRegularFile(file) && Files.readString(file, StandardCharsets.UTF_8).equals(entry)) return;
			Files.createDirectories(file.getParent());
			Files.writeString(file, entry, StandardCharsets.UTF_8);
		}
		catch(IOException | URISyntaxException | RuntimeException e)
		{
			System.out.println("Cannot add IndianGold to the applications: " + e.getMessage());
		}
	}

	/** Quotes an Exec argument as the desktop entry specification asks. */
	static String quote(String argument)
	{
		if(argument.chars().noneMatch(c -> " \t\n\"'\\><~|&;$*?#()`".indexOf(c) >= 0)) return argument;
		return "\"" + argument.replace("\\", "\\\\\\\\").replace("\"", "\\\"").replace("`", "\\`").replace("$", "\\$") + "\"";
	}
}
