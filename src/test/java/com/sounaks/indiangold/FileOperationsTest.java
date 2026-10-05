package com.sounaks.indiangold;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileOperationsTest
{
	private static final String FILE_NAME = "units.dat";

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

	private static FileOperations open()
	{
		return new FileOperations(new File(FILE_NAME), "test");
	}

	private Path userFile()
	{
		return home.resolve(".indiangold").resolve(FILE_NAME);
	}

	private Path backupFile()
	{
		return home.resolve(".indiangold").resolve(FILE_NAME + ".bak");
	}

	private static FileOperations saveCurrency(String currency)
	{
		FileOperations ops = open();
		ops.setValue("$currency", currency);
		ops.saveToFile();
		return ops;
	}

	@Test
	void savesToUserHomeWhenNoFileSitsNextToTheJar()
	{
		saveCurrency("INR");

		assertTrue(Files.isRegularFile(userFile()));
		assertEquals("INR", open().getValue("$currency", "?"));
		assertFalse(Files.exists(jarDir.resolve(FILE_NAME)), "must never write next to the jar");
	}

	@Test
	void firstStartUsesTheUnitsBundledInTheJar()
	{
		FileOperations ops = open();

		assertTrue(ops.getAllUnitNames().contains("*gram (g)"));
		assertTrue(ops.getAllUnitNames().size() > 2);
	}

	@Test
	void loadingNeverWritesFiles()
	{
		open();

		assertFalse(Files.exists(home.resolve(".indiangold")));
	}

	@Test
	void usesFileNextToTheJarUntilTheUserSavesOwnSettings() throws IOException
	{
		Files.writeString(jarDir.resolve(FILE_NAME), "*tola=1.0E-4\n$currency=EUR\n");
		assertEquals("EUR", open().getValue("$currency", "?"));

		saveCurrency("JPY");

		assertEquals("JPY", open().getValue("$currency", "?"));
		assertEquals("*tola=1.0E-4\n$currency=EUR\n", Files.readString(jarDir.resolve(FILE_NAME)), "the seed is never modified");
	}

	@Test
	void roundTripsUnitsRatesAndSettings()
	{
		FileOperations ops = open();
		ops.setValue("*Vori (vr)", "8.57E-5");
		ops.setValue("@gold", "2400.50");
		ops.setValue("$taxes", "SGST |1.5|CGST |1.5");
		ops.removeValue("*carat (ct)");
		ops.saveToFile();

		FileOperations reloaded = open();
		assertTrue(reloaded.getAllUnitNames().contains("*vori (vr)"));
		assertEquals("8.57E-5", reloaded.getValue("*vori (vr)", "?"));
		assertEquals("2400.50", reloaded.getValue("@gold", "?"));
		assertEquals("SGST |1.5|CGST |1.5", reloaded.getValue("$taxes", "?"));
		assertFalse(reloaded.getAllUnitNames().contains("*carat (ct)"));
	}

	@Test
	void keepsPreviousVersionAsBackupAndRecoversFromMalformedFile() throws IOException
	{
		saveCurrency("AUD");
		saveCurrency("CAD");
		assertTrue(Files.isRegularFile(backupFile()));

		Files.writeString(userFile(), "*gram=0.001\n$currency=\\uZZZZ\n");

		assertEquals("AUD", open().getValue("$currency", "?"));
	}

	@Test
	void fileWithoutUnitsFallsBackToBackupAndIsNotOverwrittenByLoading() throws IOException
	{
		saveCurrency("AUD");
		saveCurrency("CAD");
		String truncated = "#test\n$currency=CAD\n";
		Files.writeString(userFile(), truncated);

		FileOperations ops = open();

		assertEquals("AUD", ops.getValue("$currency", "?"));
		assertTrue(ops.getAllUnitNames().size() > 2, "units come from the backup, not the two built-in defaults");
		assertEquals(truncated, Files.readString(userFile()));
	}

	@Test
	void leavesNoTempFilesBehind()
	{
		saveCurrency("USD");
		saveCurrency("USD");

		String[] temps = userFile().getParent().toFile().list((dir, name) -> name.endsWith(".tmp"));
		assertEquals(0, temps.length);
	}

	@Test
	void discardRestoresValuesFromBeforeTheChanges()
	{
		FileOperations ops = saveCurrency("INR");
		ops.setValue("$currency", "GBP");
		ops.removeValue("*gram (g)");

		ops.discard();

		assertEquals("INR", ops.getValue("$currency", "?"));
		assertTrue(ops.getAllUnitNames().contains("*gram (g)"));
	}
}
