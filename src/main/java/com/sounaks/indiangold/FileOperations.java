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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLDecoder;

/**
 *
 * @author Sounak Choudhury
 */
class FileOperations
{
	private Properties props, tmpProps;
	private FileOutputStream fout = null;
	private InputStream fin = null;
	private File propFile;
	private final String header;
	private Vector <String>allUnitNames;
	private Vector <String>allUnitValues;
	private Vector <String>allMetalNames;
	private Vector <String>allMetalRates;
	boolean modify;
	
        /**
         * This internal method is the one actually responsible to load the system properties file from the file system. This is called from the constructor.
         */
	private void loadData()
	{
		/*fout=null;
		fin=null;
		props=null;
		Runtime rtime=Runtime.getRuntime();
		rtime.runFinalization();
		rtime.gc();*/
		
		props = new Properties();
		tmpProps = new Properties();
		try
		{
                    if(propFile.getPath().contains(".jar!"))
                        fin = getClass().getResourceAsStream("/"+propFile.getName());
                    else
			fin = new FileInputStream(propFile);
		}
		catch(FileNotFoundException fe)
		{
                    System.out.println("Missing: "+propFile.getPath());
		}
		//load data1.
		try
		{
			if(fin != null)
			{
				props.load(fin);
				fin.close();
			}
		}
		catch(IOException ie)
		{
			System.out.println("Error reading file.");
		}
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
                if(allUnitNames.isEmpty())
                {
                    setValue("*troy ounce (oz t)", "3.215074656862798E-5");
                    setValue("*pound (lb)", "2.204619999998249E-6");
                    saveToFile();
                    loadUnitVectors();
                }
	}
	
        /**
         * The only constructor of this class responsible to load the software properties file from the file system, initialization of the software properties and the unit vectors.
         * @param file The file to be loaded as properties file.
         * @param hdr The header of the loaded file.
         */
	FileOperations(File file, String hdr)
	{
		// Getting general path for this jar file...
		String path="", createdPath="";
		try
		{
                    path = URLDecoder.decode(Thread.currentThread().getContextClassLoader().getResource(file.getName()).getPath(), "UTF-8");
                    if(path.contains(".jar!"))
                    {
                        String path1 = path.split(".jar!")[0].replace('\\', '/');
                        createdPath = path1.substring(0, path1.lastIndexOf('/'))+ "/" + file.getName();
		}
                    try
                    {
                        File test = new File((new java.net.URL(createdPath)).toURI());
                        if(test.exists())
                        {
                            path = test.getPath();
//                            System.out.println("Exists: "+path);
                        }
//                        else
//                            System.out.println("Jar only: "+path);
                    }
                    catch(MalformedURLException | URISyntaxException me)
                    {
//                        System.out.println("Jar only: "+path);
                    }
		}
		catch(UnsupportedEncodingException uee)
		{
			System.out.println("Unsupported encoding: UTF-8");
		}
		catch(NullPointerException ne)
		{
			System.out.println("Class loader or resource path not found for this jar file.");
			path = "";
		}

		if(path.equals(""))
		{
			try
			{
				path = URLDecoder.decode(FileOperations.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
				if(path.toLowerCase().endsWith(".jar"))
				{
					File tryfile = new File(path);
					path = URLDecoder.decode(tryfile.getParentFile().getPath(), "UTF-8");
				}
			}
			catch(UnsupportedEncodingException uee)
			{
				System.out.println("Unsupported encoding: UTF-8");
			}
			catch(NullPointerException ne)
			{
				System.out.println("Protection domain or code source for this jar file not found.");
			}
		}
//		System.out.println(path);

		propFile=new File(path.endsWith(file.getName())?path.replaceAll(file.getName(), ""):path, file.getName());
		header=hdr;
		allUnitNames=new Vector<String>();
		allUnitValues=new Vector<String>();
                allMetalNames=new Vector<String>();
                allMetalRates=new Vector<String>();
		loadData();
		modify = false;
	}
	
        /**
         * This method saves the properties file of this software in the file system.
         */
	public void saveToFile()
	{
		try
		{
                    if(propFile.getPath().contains(".jar!"))
                    {
                        String path0 = propFile.getPath().split(".jar!")[0].replace('\\', '/');
//                        System.out.println(path1);
                        propFile = new File(path0.substring(5, path0.lastIndexOf('/')), propFile.getName());
//                        System.out.println(propFile.getPath());
                        if(!propFile.exists()) propFile.createNewFile();
                    }
//                    System.out.println("Saving to: "+propFile.getPath());
			fout = new FileOutputStream(propFile);
			props.store(fout, header);
			fout.close();
		}
		catch(IOException e)
		{
                    System.out.println(e.getMessage());
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