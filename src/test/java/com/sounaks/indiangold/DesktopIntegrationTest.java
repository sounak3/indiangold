package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Opening links without crashing, and the desktop entry that lets docks show the app. */
class DesktopIntegrationTest
{
	@Test
	void snapLibrariesAreKeptAwayFromProgramsTheAppStarts()
	{
		// What a snap-packaged IDE leaves behind; GTK programs then load the snap's libraries and crash.
		Map<String, String> environment = new HashMap<>(Map.of(
				"GTK_PATH", "/snap/code/267/usr/lib/x86_64-linux-gnu/gtk-3.0",
				"GIO_MODULE_DIR", "/home/u/snap/code/common/.cache/gio-modules",
				"LOCPATH", "/snap/code/267/usr/lib/locale",
				"XDG_DATA_DIRS", "/home/u/snap/code/267/.local/share:/usr/share",
				"XDG_DATA_DIRS_VSCODE_SNAP_ORIG", "/usr/share/ubuntu:/usr/share",
				"GTK_MODULES", "gail:atk-bridge",
				"HOME", "/home/u"));

		Links.cleanSnapEnvironment(environment);

		assertFalse(environment.containsKey("GTK_PATH"));
		assertFalse(environment.containsKey("GIO_MODULE_DIR"));
		assertFalse(environment.containsKey("LOCPATH"));
		assertEquals("/usr/share/ubuntu:/usr/share", environment.get("XDG_DATA_DIRS"), "the value before the snap changed it");
		assertEquals("gail:atk-bridge", environment.get("GTK_MODULES"), "values that don't point into a snap are kept");
		assertEquals("/home/u", environment.get("HOME"));
	}

	@Test
	void desktopEntryArgumentsAreQuotedWhenNeeded()
	{
		assertEquals("/usr/bin/java", DesktopIntegration.quote("/usr/bin/java"));
		assertEquals("\"/home/u/My Apps/indiangold.jar\"", DesktopIntegration.quote("/home/u/My Apps/indiangold.jar"));
		assertEquals("\"/a/\\$b\"", DesktopIntegration.quote("/a/$b"));
	}

	@Test
	void windowIconsComeInSeveralSizes()
	{
		assertEquals(6, DesktopIntegration.icons().size());
	}
}
