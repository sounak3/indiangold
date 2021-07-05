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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sounak
 */
class FileOperations
{
	private Properties props, tmpProps;
	private FileOutputStream fout = null;
	private InputStream fin = null;
	private File propFile;
	private String header;
	private Vector <String>data1;
	private Vector <String>data2;
	boolean modify;
	
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
		loadVectors();
	}

	private void loadVectors()
	{
		data1.removeAllElements();
		data2.removeAllElements();
		Enumeration enum1=props.propertyNames();
		for(;enum1.hasMoreElements();)
		{
			String tmp=enum1.nextElement().toString().toLowerCase();
			if(tmp.startsWith("default_")) continue;
			String tmp1=getValue(tmp,"");
			data1.addElement(tmp);
			data2.addElement(tmp1);
		}
	}
	
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

		propFile=new File(path.endsWith(file.getName())?path.replaceAll(file.getName(), ""):path, file.getName());
		header=hdr;
		data1=new Vector<String>();
		data2=new Vector<String>();
		loadData();
		modify = false;
	}
	
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
		catch(Exception e)
		{
                    e.printStackTrace();
                }
		tmpProps.clear();
		loadVectors();
		modify = false;
	}

	public void discard()
	{
		if(modify)
		{
			props.clear();
			props.putAll(tmpProps);
			tmpProps.clear();
			loadVectors();
			modify = false;
		}
	}

	public Properties getAllProperties()
	{
		return props;
	}
	
	public Vector<String> getPropertyNames()
	{
		Vector<String> newvec = new Vector<String>(data1.size());
		for(int i=0; i<data1.size(); i++)
		{
			if(data1.elementAt(i).startsWith("default_")) continue;
			newvec.addElement(data1.elementAt(i));
		}
		newvec.trimToSize();
		return newvec;
	}

	public Vector<String> getAllPropertyNames()
	{
		return data1;
	}

	public Vector<String> getPropertyValues()
	{
		Vector<String> newvec = new Vector<String>(data2.size());
		for(int i=0; i<data2.size(); i++)
		{
			if(data1.elementAt(i).startsWith("default_")) continue;
			newvec.addElement(data2.elementAt(i));
		}
		newvec.trimToSize();
		return newvec;
	}

	public Vector<String> getAllPropertyValues()
	{
		return data2;
	}

	public String getValue(String pName,String pValue)
	{
		return props.getProperty(pName, pValue);
	}
	
	public void setValue(String pName,String pValue, int place)
	{
		if(!modify) tmpProps.putAll(props);
		pName=pName.toLowerCase();
		boolean dflt = pName.startsWith("default_");
		if(!dflt)
		{
		}
		props.put(pName,pValue);
		loadVectors();
		modify = true;
	}
	
	public void removeValue(String pName)
	{
		if(!modify) tmpProps.putAll(props);
		props.remove(pName);
		loadVectors();
		modify = true;
	}
}