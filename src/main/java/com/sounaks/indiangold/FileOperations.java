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

import java.util.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads and saves the software properties file. The user's copy lives in ~/.indiangold, so saving works
 * wherever the jar is installed; a copy next to the jar or the one bundled in it only seeds the first start.
 * @author Sounak Choudhury
 */
class FileOperations
{
	static final String DATA_DIR_NAME = ".indiangold";
	static File jarDir; // Folder of the running jar; tests point it at a temp folder.

	private Properties props, tmpProps;
	private final String fileName;
	private final File userFile;
	private final String header;
	private Vector <String>allUnitNames;
	private Vector <String>allUnitValues;
	private Vector <String>allMetalNames;
	private Vector <String>allMetalRates;
	boolean modify;
	
        /**
         * Gets the per-user folder holding the properties file and its backup.
         * @return The folder ~/.indiangold, which may not exist yet.
         */
	static File getDataDir()
	{
		return new File(System.getProperty("user.home"), DATA_DIR_NAME);
	}

        /**
         * Gets the folder containing the running jar, or the classes folder when run from an IDE.
         * @return The folder, or null if it cannot be found.
         */
	static File getJarDir()
	{
		if(jarDir == null)
		{
			try
			{
				File location = new File(FileOperations.class.getProtectionDomain().getCodeSource().getLocation().toURI());
				jarDir = location.isDirectory() ? location : location.getParentFile();
			}
			catch(URISyntaxException | RuntimeException e)
			{
				System.out.println("Cannot find the folder of this jar file: " + e);
			}
		}
		return jarDir;
	}

	private static File backupOf(File file)
	{
		return new File(file.getParentFile(), file.getName() + ".bak");
	}

        /**
         * Reads a properties file, rejecting one that is malformed or has no units, as left behind by an interrupted save.
         * @return The properties, or null if the content is not usable.
         */
	private static Properties readUsable(InputStream in, String source) throws IOException
	{
		Properties loaded = new Properties();
		try
		{
			loaded.load(in);
		}
		catch(IllegalArgumentException e)
		{
			System.out.println("Cannot read " + source + ": " + e.getMessage());
			return null;
		}
		for(String key : loaded.stringPropertyNames())
		{
			if(key.startsWith("*") || key.startsWith("_")) return loaded;
		}
		System.out.println("Ignoring " + source + ": it has no units.");
		return null;
	}

        /**
         * Loads the first usable copy of the properties file: the user's file in ~/.indiangold, its .bak, a copy next to
         * the jar (older versions saved there), then the copy bundled in the jar. Loading never writes any file.
         */
	private void loadData()
	{
		props = new Properties();
		tmpProps = new Properties();
		Properties loaded = null;
		File jarFolder = getJarDir();
		File[] candidates = { userFile, backupOf(userFile), jarFolder == null ? null : new File(jarFolder, fileName) };
		for(File candidate : candidates)
		{
			if(candidate == null || !candidate.isFile()) continue;
			try(InputStream in = new BufferedInputStream(new FileInputStream(candidate)))
			{
				loaded = readUsable(in, candidate.getPath());
			}
			catch(IOException e)
			{
				System.out.println("Cannot read " + candidate + ": " + e);
			}
			if(loaded != null) break;
		}
		if(loaded == null)
		{
			try(InputStream in = FileOperations.class.getResourceAsStream("/" + fileName))
			{
				if(in != null) loaded = readUsable(in, "bundled " + fileName);
			}
			catch(IOException e)
			{
				System.out.println("Cannot read bundled " + fileName + ": " + e);
			}
		}
		if(loaded != null) props.putAll(loaded);
		loadUnitVectors();
                loadMetalAndRateVectors();
	}

        /**
         * This internal method fills the unit vectors namely data1 and allUnitValues from the software properties table.
         */
	private void loadUnitVectors()
	{
		allUnitNames.removeAllElements();
		allUnitValues.removeAllElements();
		Enumeration enum1=props.propertyNames();
		for(;enum1.hasMoreElements();)
		{
                    String tmp=enum1.nextElement().toString().toLowerCase();
                    if(tmp.startsWith("*") || tmp.startsWith("_"))
                    {
			String tmp1=getValue(tmp,"");
			allUnitNames.addElement(tmp);
			allUnitValues.addElement(tmp1);
                    }
		}
                if(allUnitNames.isEmpty()) // Built-in defaults; they reach the disk with the next save.
                {
                    setValue("*troy ounce (oz t)", "3.215074656862798E-5");
                    setValue("*pound (lb)", "2.204619999998249E-6");
                }
	}
	
        /**
         * The only constructor of this class responsible to load the software properties file from the file system, initialization of the software properties and the unit vectors.
         * @param file The file to be loaded as properties file.
         * @param hdr The header of the loaded file.
         */
	FileOperations(File file, String hdr)
	{
		fileName=file.getName();
		userFile=new File(getDataDir(), fileName);
		header=hdr;
		allUnitNames=new Vector<String>();
		allUnitValues=new Vector<String>();
                allMetalNames=new Vector<String>();
                allMetalRates=new Vector<String>();
		loadData();
		tmpProps.clear();
		modify = false;
	}

        /**
         * Gets the file the properties are saved to.
         * @return The properties file in ~/.indiangold.
         */
	File getUserFile()
	{
		return userFile;
	}

        /**
         * Writes the properties to a temp file and swaps it in, so a crash mid-save never leaves a truncated file.
         * The previous version is kept as file.bak.
         */
	static void writeProperties(File file, Properties content, String header) throws IOException
	{
		Path target = file.toPath();
		Files.createDirectories(target.getParent());
		Path temp = Files.createTempFile(target.getParent(), file.getName(), ".tmp");
		try
		{
			try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp)))
			{
				content.store(out, header);
			}
			if(Files.exists(target)) Files.copy(target, backupOf(file).toPath(), StandardCopyOption.REPLACE_EXISTING);
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
         * This method saves the properties file of this software in ~/.indiangold.
         * It is synchronized because the rate bar also saves from its timer thread.
         */
	public synchronized void saveToFile()
	{
		try
		{
			writeProperties(userFile, props, header);
		}
		catch(IOException e)
		{
                    System.out.println("Cannot save " + userFile + ": " + e);
                }
		tmpProps.clear();
		loadUnitVectors();
                loadMetalAndRateVectors();
		modify = false;
	}

        /**
         * This method is used to discard any changes made in the properties table used in this software.
         */
	public void discard()
	{
		if(modify)
		{
			props.clear();
			props.putAll(tmpProps);
			tmpProps.clear();
			loadUnitVectors();
			modify = false;
		}
	}

        /**
         * Gets the current set of properties table used by this software.
         * @return Properties object containing current data set.
         */
        public Properties getAllProperties()
	{
		return props;
	}
	
        /**
         * Gets a vector containing the list of only those unit names from the unit list which are checked.
         * @return A vector containing the list of only those unit names from the unit list which are checked.
         */
	public Vector<String> getCheckedUnitNames()
	{
		Vector<String> newvec = new Vector<String>(allUnitNames.size());
		for(int i=0; i<allUnitNames.size(); i++)
		{
			if(allUnitNames.elementAt(i).startsWith("*"))
                            newvec.addElement((String)allUnitNames.elementAt(i).substring(1));
		}
		newvec.trimToSize();
		return newvec;
	}

        /**
         * Gets a vector containing the list of all the unit names in the unit list.
         * @return A vector containing list of all the unit names in the unit list.
         */
	public Vector<String> getAllUnitNames()
	{
		return allUnitNames;
	}

        /**
         * Gets a vector containing the list of only those unit values from the unit list which are checked.
         * @return A vector containing the list of only those unit values from the unit list which are checked.
         */
	public Vector<String> getCheckedUnitValues()
	{
		Vector<String> newvec = new Vector<String>(allUnitValues.size());
		for(int i=0; i<allUnitValues.size(); i++)
		{
			if(allUnitNames.elementAt(i).startsWith("*"))
                            newvec.addElement(allUnitValues.elementAt(i));
		}
		newvec.trimToSize();
		return newvec;
	}

        /**
         * Gets a vector list containing values of all the units in the unit list.
         * @return A vector containing values of all the units in the unit list.
         */
	public Vector<String> getAllUnitValues()
	{
		return allUnitValues;
	}

        /**
         * Gets the unit value of the given unit. If the given unit is not present, then the given value is returned.
         * @param pName The name of the unit for which the value is to be found and returned.
         * @param pValue The value to be returned in case either the given unit is not found or no value for the given unit is found.
         * @return A string containing the value for the given unit.
         */
	public String getValue(String pName,String pValue)
	{
		return props.getProperty(pName, pValue);
	}

        /**
         * Checks whether a unit exists in the unit list, checked or not.
         * @param name The unit name without the * or _ prefix; case does not matter.
         * @return True if the unit is in the list.
         */
	boolean hasUnit(String name)
	{
		String key = name.toLowerCase();
		return props.containsKey("*" + key) || props.containsKey("_" + key);
	}
        
	/**
         * Sets i.e. adds the given unit if the unit is not present or updates if the given unit is already present in the unit list, with the given value.
         * @param pName The unit name to be added or updated.
         * @param pValue The value to be assigned to the given unit name.
         */
	public void setValue(String pName,String pValue)
	{
            if(!modify)
                tmpProps.putAll(props);
            pName=pName.toLowerCase();
            props.put(pName,pValue);
            loadUnitVectors();
            loadMetalAndRateVectors();
            modify = true;
	}
	
        /**
         * Removes the given unit from the property/unit list. Does nothing if the given unit is not found in the unit list.
         * @param pName Specifies the unit to be removed.
         */
	public void removeValue(String pName)
	{
            if(!modify)
                tmpProps.putAll(props);
            props.remove(pName);
            loadUnitVectors();
            loadMetalAndRateVectors();
            modify = true;
	}
        
        private void loadMetalAndRateVectors()
        {
		allMetalNames.removeAllElements();
		allMetalRates.removeAllElements();
		Enumeration enum1=props.propertyNames();
		for(;enum1.hasMoreElements();)
		{
                    String tmp=enum1.nextElement().toString().toLowerCase();
                    if(tmp.startsWith("@"))
                    {
			String tmp1=getValue(tmp,"");
			allMetalNames.addElement(tmp);
			allMetalRates.addElement(tmp1);
                    }
		}
        }

        /**
         * Gets a vector containing rates of all the metals obtained and saved from the web during last session.
         * @return A Vector containing rates of all the metals saved in the software properties file.
         */
        public Vector<String> getAllMetalRates()
        {
            return allMetalRates;
        }

        /**
         * Gets a vector containing all the metals for which rates are obtained or to be obtained.
         * @return A Vector containing names of all the metals saved in the software properties file.
         */
        public Vector<String> getAllMetalNames()
        {
            return allMetalNames;
        }
}