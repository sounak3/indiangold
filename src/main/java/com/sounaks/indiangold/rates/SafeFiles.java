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
package com.sounaks.indiangold.rates;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Properties;

/**
 * Saves files so that a crash never leaves a truncated one: write a temp file, keep the old file as .bak, swap.
 * @author Sounak Choudhury
 */
public final class SafeFiles
{
	private SafeFiles()
	{
	}

	public static Path backupOf(Path file)
	{
		return file.resolveSibling(file.getFileName() + ".bak");
	}

	/**
	 * Writes properties safely. The previous version is kept as file.bak.
	 * @param target The file to write.
	 * @param content The properties.
	 * @param header A comment for the first line, or null.
	 * @throws IOException If the file cannot be written; the old file is then left as it was.
	 */
	public static void writeProperties(Path target, Properties content, String header) throws IOException
	{
		Files.createDirectories(target.toAbsolutePath().getParent());
		Path temp = Files.createTempFile(target.toAbsolutePath().getParent(), target.getFileName().toString(), ".tmp");
		try
		{
			try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp)))
			{
				content.store(out, header);
			}
			if(Files.exists(target)) Files.copy(target, backupOf(target), StandardCopyOption.REPLACE_EXISTING);
			try
			{
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			catch(AtomicMoveNotSupportedException e)
			{
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			Files.deleteIfExists(temp);
		}
	}

	/**
	 * Reads properties from a file, or from its .bak if the file is missing or unreadable.
	 * @param file The file.
	 * @return The properties, or empty if neither file can be read.
	 */
	public static Optional<Properties> readProperties(Path file)
	{
		for(Path candidate : new Path[] { file, backupOf(file) })
		{
			if(!Files.isRegularFile(candidate)) continue;
			try(InputStream in = new BufferedInputStream(Files.newInputStream(candidate)))
			{
				Properties properties = new Properties();
				properties.load(in);
				return Optional.of(properties);
			}
			catch(IOException | IllegalArgumentException e)
			{
				System.out.println("Cannot read " + candidate + ": " + e.getMessage());
			}
		}
		return Optional.empty();
	}
}
