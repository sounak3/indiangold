package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Path;

import javax.swing.JList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Inputs of the settings dialog that used to misbehave. Runs headless: no window is shown. */
class SettingsDialogInputTest
{
	@TempDir Path home;
	@TempDir Path jarDir;
	private String originalHome;

	@BeforeEach
	void isolateFromRealUser()
	{
		originalHome = System.getProperty("user.home");
		System.setProperty("user.home", home.toString());
		FileOperations.jarDir = jarDir.toFile();
	}

	@AfterEach
	void restore()
	{
		System.setProperty("user.home", originalHome);
		FileOperations.jarDir = null;
	}

	@Test
	void newUnitIsNotReportedAsExisting()
	{
		// The Add button used to ask "already exists, replace?" for every new unit.
		FileOperations ops = new FileOperations(new File("units.dat"), "test");

		assertFalse(ops.hasUnit("vori (vr)"));
		assertTrue(ops.hasUnit("gram (g)"));
		assertTrue(ops.hasUnit("Gram (G)"), "unit names are stored in lower case");

		ops.setValue("_vori (vr)", "8.57E-5");
		assertTrue(ops.hasUnit("vori (vr)"), "unchecked units count too");
	}

	@Test
	void onlyClicksOnTheCheckBoxToggleAUnit()
	{
		JList<CheckableItem> list = new JList<>(new CheckableItem[] { new CheckableItem("*gram (g)"), new CheckableItem("_tola") });
		list.setCellRenderer(new CheckboxListRenderer());
		list.setSize(200, 200);
		Rectangle first = list.getCellBounds(0, 0);

		assertTrue(CheckboxListRenderer.isOnCheckBox(list, new Point(first.x + 3, first.y + first.height / 2)));
		assertFalse(CheckboxListRenderer.isOnCheckBox(list, new Point(first.x + 150, first.y + first.height / 2)),
				"a click on the unit name only selects it");
		Rectangle last = list.getCellBounds(1, 1);
		assertFalse(CheckboxListRenderer.isOnCheckBox(list, new Point(last.x + 3, last.y + last.height + 20)),
				"a click below the last unit must not toggle it");
	}

	@Test
	void numberFieldRejectsTextThatIsNotANumber()
	{
		NumberField plain = new NumberField(5, false);
		plain.setText("12.5");
		plain.selectAll();
		plain.replaceSelection("abc");  // what a paste does
		assertEquals("12.5", plain.getText());

		plain.setText("1.2.3");
		assertEquals("12.5", plain.getText());
		plain.setText("7%");
		assertEquals("12.5", plain.getText(), "percent only where allowed");

		NumberField percent = new NumberField(5, true);
		percent.setText("1.5%");
		assertEquals(1.5, percent.getNumberInput(), 1e-9);
		assertTrue(percent.hasPercentSign());
	}

	@Test
	void incompleteNumbersCountAsZero()
	{
		NumberField field = new NumberField(5, true);
		for (String text : new String[] { "", ".", "%", ".%" }) {
			field.setText(text);
			assertEquals(0.0, field.getNumberInput(), 1e-9, "'" + text + "'");
		}
		field.setText(".5");
		assertEquals(0.5, field.getNumberInput(), 1e-9);
	}
}
