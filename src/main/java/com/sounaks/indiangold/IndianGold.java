
/*
    IndianGold.java : Part of IndianGold weight calculation software application.
    Copyright (C) 2012  Sounak Choudhury

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License, as published by
    the Free Software Foundation, version 3.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

My contact e-mail: sounak3@gmail.com, phone: +91-9595949401.
*/
package com.sounaks.indiangold;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.text.*;
import javax.swing.table.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

public class IndianGold extends JFrame implements ActionListener, FocusListener
{
	//String weightList[]=new String[]{"miligrams (mg)","grams (g)","kilograms (kg)","ratti (rt)","grains (gr)","carats (ct)","ounces (oz)","pounds (lb)","cents (pts)","bengal bhori (old tola)","tola (per 10 grams)", "gujrati tola"};
	//Double mgValue[]=new Double[]{1.000000000000,0.001000000000,0.000001000000,0.005494510000,0.015432400000,0.005000000000,0.000035274000,0.000002204620,0.500000000000,0.000085733882,0.000100000000,0.000083333333};
	Vector <String>weightList;
	Vector <String>mgValue;
	FileOperations fOps;
	AddRemoveBox box;
	JLabel l1, l2, l3;
	JTextPane costArea;
	NumberField text1, text2, text3;
	JTable table1;
	JComboBox comb1, comb2, comb3;
	DecimalFormat formatter;
	DefaultTableModel model;
	JButton abtButton, setButton;
	JScrollPane spane;
	Currency currency;

	private String getCostString()
	{
		String costString = "Price of ";
		if(text1.getText().equals("") || text1.getText().equals(null)) costString = costString + localeZero();
		else costString = costString + text1.getText();
		costString = costString + " " + (String)comb1.getSelectedItem() + " will be "+currency.getSymbol();
		return costString;
	}

	private String getDecimalFormatString()
	{
		int input=0;
		String format="###########0";
		try
		{
			input=Integer.parseInt((String)comb2.getSelectedItem());
		}
		catch(NumberFormatException ne)
		{
			input = currency.getDefaultFractionDigits();
		}
		if(input != 0) format=format+".";
		for(int i=0;i<input;i++)
		{
			format=format+"0";
		}
		return format+"  ";
	}

	private double getNumberInput(NumberField source)
	{
		double input=0.00;
		try
		{
			input=Double.parseDouble(source.getText());
		}
		catch(NumberFormatException ne)
		{
			input=0.00;
		}
		return input;
	}

	private double getWeightSelectionMgValue(JComboBox source)
	{
		String selection=(String)source.getSelectedItem();
		for(int i=0; i<weightList.size(); i++)
		{
			if(weightList.elementAt(i).equals(selection))
			{
				try
				{
					return Double.parseDouble(mgValue.elementAt(i));
				}
				catch(NumberFormatException ne)
				{
					System.out.println("Error parsing weight value: "+ne.toString());
					return 0;
				}
			}
		}
		return new Double(0);
	}
	
	private void calculateWeights()
	{
		formatter = new DecimalFormat(getDecimalFormatString());
		double weightVal=0.00;
		model.setRowCount(weightList.size());
		for(int i=0; i<weightList.size(); i++)
		{
			try
			{
				weightVal = Double.parseDouble(mgValue.elementAt(i));
			}
			catch(NumberFormatException ne)
			{
				System.out.println("Error parsing weight value: "+ne.toString());
				weightVal = 0;
			}
			model.setValueAt(formatter.format((getNumberInput(text1) / getWeightSelectionMgValue(comb1)) * weightVal), i, 0);
			model.setValueAt(weightList.elementAt(i), i, 1);
		}
	}

	private void calculateCost()
	{
		String fmtr = "###########0.";
		for(int i=0;i<currency.getDefaultFractionDigits();i++)
		{
			fmtr=fmtr+"0";
		}
		formatter = new DecimalFormat(fmtr);

		double costPerMiligram = getNumberInput(text2)/(getNumberInput(text3)/getWeightSelectionMgValue(comb3));
		double noOfMiligrams = getNumberInput(text1)/ getWeightSelectionMgValue(comb1);

		if(new Double(costPerMiligram * noOfMiligrams).equals(Double.NaN))
		costArea.setText(getCostString()+localeZero());
		else
		costArea.setText(getCostString()+formatter.format(costPerMiligram * noOfMiligrams));
	}

	private String localeZero()
	{
		String fmtr = "0.";
		for(int i=0;i<currency.getDefaultFractionDigits();i++)
		{
			fmtr=fmtr+"0";
		}
		return fmtr;
	}

	private void resetUIData()
	{
		weightList.clear();
		weightList.addAll(fOps.getPropertyNames());
		mgValue.clear();
		mgValue.addAll(fOps.getPropertyValues());
		text2.setText(text2.getText().equals("") ? localeZero() : text2.getText());
		text3.setText(text3.getText().equals("") ? "10" : text3.getText());
		if(weightList==null) System.out.println("got");
		comb1.setSelectedItem(weightList.contains(comb1.getSelectedItem()) ? comb1.getSelectedItem() : weightList.isEmpty()?"":weightList.elementAt(0));
		comb2.setSelectedItem(((String)comb2.getSelectedItem()).equals("0") ? String.valueOf(currency.getDefaultFractionDigits()) : (String)comb2.getSelectedItem());
		comb3.setSelectedItem(weightList.contains(comb3.getSelectedItem()) ? comb3.getSelectedItem() : weightList.isEmpty()?"":weightList.elementAt(0));
	}

	public void actionPerformed(ActionEvent ae)
	{
		Object src = ae.getSource();
		if(src.equals(abtButton))
		{
			String s1 = "<html>Created and Developed by : Sounak Choudhury<p>E-mail Address : <a href='mailto:contact@sounaks.com'>contact@sounaks.com</a><p>The software, information and documentation<p>is provided \"AS IS\" without warranty of any<p>kind, either expressed or implied. The Readme.txt<p>file containing EULA must be read before use.<p>Suggestions and credits are Welcomed.</html>";
                        ImageIcon imageicon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("duke.gif"));
			JOptionPane.showMessageDialog(new Frame(), s1, "About IndianGold...", 1, imageicon);
		}
		else if(src.equals(setButton))
		{
			box = new AddRemoveBox(this, fOps);
			box.setVisible(true);
			resetUIData();
		}
		calculateWeights();
		calculateCost();
	}

        @Override
	public void focusLost(FocusEvent fe)
	{
		calculateWeights();
		calculateCost();
	}

        @Override
	public void focusGained(FocusEvent fe)
	{
		if(fe.getSource() instanceof NumberField)
		{
			/*SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{*/
					((NumberField)fe.getSource()).selectAll();              
				/*}
			});*/
		}
	}

	public IndianGold()
	{
		super("Indian Gold v3.0");

		fOps=new FileOperations(new File("units.dat"),"IndianGold3.0");
		weightList=new Vector<String>(); //fOps.getPropertyNames();
		mgValue=new Vector<String>(); //fOps.getPropertyValues();
		Locale locale = Locale.getDefault();
		currency = Currency.getInstance(locale);
		text1=new NumberField(14);
		text1.addActionListener(this);
		text1.addFocusListener(this);
		comb1=new JComboBox(weightList);
		comb1.addActionListener(this);
		Dimension goodDimension=new Dimension(text1.getPreferredSize().width, comb1.getPreferredSize().height);
		text1.setPreferredSize(goodDimension);
		comb1.setPreferredSize(goodDimension);

		JPanel p11=new JPanel();
		p11.add(text1);
		p11.add(comb1);
		String nums[]=new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12"};
		comb2=new JComboBox(nums);
		comb2.addActionListener(this);
		//java.net.URL url1 = this.getClass().getResource("info.gif");
		//java.net.URL url2 = this.getClass().getResource("gears.gif");
		Icon about = getResizedIcon(UIManager.getIcon("OptionPane.informationIcon"),16,16); //new ImageIcon(url1);
		Icon settings = getResizedIcon(UIManager.getIcon("FileChooser.detailsViewIcon"),16,16); //new ImageIcon(url2);
		abtButton=new JButton(about);
		abtButton.addActionListener(this);
		setButton=new JButton(settings);
		setButton.addActionListener(this);

		l1=new JLabel("Number of decimal places: ");
		JPanel p12=new JPanel();
		p12.add(l1);
		p12.add(comb2);
		p12.add(abtButton);
		p12.add(setButton);

		JPanel p1=new JPanel(new GridLayout(2,1));
		p1.add(p11);
		p1.add(p12);
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Weight Calculations"));

		String columnNames[]=new String[]{"Value","Unit"};
		model = new DefaultTableModel(columnNames,weightList.size());
		table1=new JTable(model);
		table1.getColumnModel().getColumn(0).setPreferredWidth(190);
		table1.getColumnModel().getColumn(1).setPreferredWidth(150);
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(JLabel.RIGHT );
		table1.getColumnModel().getColumn(0).setCellRenderer( rightRenderer );
		table1.setFocusable(false);
		spane =new JScrollPane(table1);
		displayNumRows(8);
		spane.setBorder(BorderFactory.createLoweredBevelBorder());

		JPanel p2=new JPanel();
		p2.add(spane);
		p2.setBorder(BorderFactory.createEtchedBorder());

		l2=new JLabel("Rate : "+currency.getSymbol());
		text2=new NumberField(5);
		text2.addActionListener(this);
		text2.addFocusListener(this);
		l3=new JLabel("per");
		text3=new NumberField(2);
		text3.addActionListener(this);
		text3.addFocusListener(this);
		comb3=new JComboBox(weightList);
		comb3.setPreferredSize(goodDimension);
		comb3.addActionListener(this);
		JPanel p31=new JPanel();
		p31.add(l2);
		p31.add(text2);
		p31.add(l3);
		p31.add(text3);
		p31.add(comb3);

		costArea=new JTextPane();
		costArea.setPreferredSize(new Dimension(340,40));
		SimpleAttributeSet attribs = new SimpleAttributeSet();
		StyleConstants.setAlignment(attribs , StyleConstants.ALIGN_CENTER);  
		StyleConstants.setBold(attribs , true);
		StyleConstants.setForeground(attribs , Color.red);
		costArea.setParagraphAttributes(attribs,true);  
		costArea.setText(getCostString()+localeZero());
		costArea.setEditable(false);
		costArea.setFocusable(false);
		costArea.setBorder(BorderFactory.createEtchedBorder());
		JPanel p32=new JPanel();
		p32.add(costArea);

		JPanel p3=new JPanel(new GridLayout(2,1));
		p3.add(p31);
		p3.add(p32);
		p3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Price Calculations"));

		setLayout(new BorderLayout());
		add(p1,BorderLayout.NORTH);
		add(p2,BorderLayout.CENTER);
		add(p3,BorderLayout.SOUTH);

		try
		{
			java.net.URL url1 = Thread.currentThread().getContextClassLoader().getResource("igcircle.gif");
			Image icon = ImageIO.read(url1);
			setIconImage(icon);
		}
		catch(IOException e)
		{
			System.out.println("Icon not found.");
		}
		resetUIData();
	}

	public void displayNumRows(int num) //method for controlling number of rows to dispaly in the table
	{
		int hh=table1.getRowMargin()+table1.getRowHeight(0);
		spane.setPreferredSize(new Dimension(340,num*hh+hh));
	}

	public Icon getResizedIcon(Icon icon, int width, int height)
	{
		SafeIcon ico = new SafeIcon(icon);
		BufferedImage image = new BufferedImage(ico.getIconWidth(), ico.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		ico.paintIcon(new JButton(), image.getGraphics(), 0, 0);
		Image tmpImg = image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
		return new ImageIcon(tmpImg);
	}

	public static Dimension getScreenCenterLocation(Container cont)
	{
		Dimension window = cont.getSize();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int X = (screen.width / 2) - (window.width / 2); // Center horizontally.
		int Y = (screen.height / 2) - (window.height / 2); // Center vertically.
		return new Dimension(X,Y);
	}

	public static void main(String args[])
	{
		IndianGold mm=new IndianGold();
		mm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mm.pack();
		Dimension loc=getScreenCenterLocation(mm);
		mm.setLocation(loc.width,loc.height);
		mm.setResizable(false);
		mm.setVisible(true);
	}
}