package com.sounaks.indiangold;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.text.*;
import javax.swing.table.*;

public class IndianGold extends JFrame implements ActionListener, FocusListener
{
	String weightList[]=new String[]{"miligrams (mg)","grams (g)","kilograms (kg)","ratti (rt)","grains (gr)","carats (ct)","ounces (oz)","pounds (lb)","cents (pts)","bengal bhori (old tola)","tola (per 10 grams)", "gujrati tola"};
	Double mgValue[]=new Double[]{1.000000000000,0.001000000000,0.000001000000,0.005494510000,0.015432400000,0.005000000000,0.000035274000,0.000002204620,0.500000000000,0.000085733882,0.000100000000,0.000083333333};
	JLabel l1, l2, l3;
	JTextPane costArea;
	NumberField text1, text2, text3;
	JTable table1;
	JComboBox comb1, comb2, comb3;
	DecimalFormat formatter;
	PlainTableModel model;
	JButton abtButton;

	private String getCostString()
	{
		String costString = "Price of ";
		if(text1.getText().equals("") || text1.getText() == null) costString = costString + "0.00";
		else costString = costString + text1.getText();
		costString = costString + " " + (String)comb1.getSelectedItem() + " will be Rs.";
		return costString;
	}

	private String getDecimalFormatString()
	{
		int input;
		String format="###########0";
		try
		{
			input=Integer.parseInt((String)comb2.getSelectedItem());
		}
		catch(NumberFormatException ne)
		{
			input=2;
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
		double input;
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
		for(int i=0; i<weightList.length; i++)
		{
			if(weightList[i].equals(selection))
			{
				return mgValue[i];
			}
		}
		return new Double(0);
	}
	
	private void calculateWeights()
	{
		formatter = new DecimalFormat(getDecimalFormatString());
		for(int i=0; i<weightList.length; i++)
		{
			model.setValueAt(formatter.format((getNumberInput(text1) / getWeightSelectionMgValue(comb1)) * mgValue[i]), i, 0);
			model.setValueAt(weightList[i], i, 1);
		}
	}

	private void calculateCost()
	{
		formatter = new DecimalFormat("###########0.00");

		double costPerMiligram = getNumberInput(text2)/(getNumberInput(text3)/getWeightSelectionMgValue(comb3));
		double noOfMiligrams = getNumberInput(text1)/ getWeightSelectionMgValue(comb1);

		if(new Double(costPerMiligram * noOfMiligrams).equals(Double.NaN))
		costArea.setText(getCostString()+"0.00");
		else
		costArea.setText(getCostString()+formatter.format(costPerMiligram * noOfMiligrams));
	}

        @Override
	public void actionPerformed(ActionEvent ae)
	{
		if(ae.getSource().equals(abtButton))
		{
			String s1 = "<html>Created and Developed by : Sounak Choudhury<p>E-mail Address : <a href='mailto:sounak_s@rediffmail.com'>sounak_s@rediffmail.com</a><p>The software, information and documentation<p>is provided \"AS IS\" without warranty of any<p>kind, either express or implied. The Readme.txt<p>file containing EULA must be read before use.<p>Suggestions and credits are Welcomed.</html>";
                        ImageIcon imageicon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("duke.gif"));
			JOptionPane.showMessageDialog(new Frame(), s1, "About IndianGold...", 1, imageicon);

		}
		else
		{
			calculateWeights();
			calculateCost();
		}
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
		super("Indian Gold v1.0");

		text1=new NumberField(15);
		text1.addActionListener(this);
		text1.addFocusListener(this);
		comb1=new JComboBox(weightList);
		comb1.addActionListener(this);

		JPanel p11=new JPanel();
		p11.add(text1);
		p11.add(comb1);
		String nums[]=new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12"};
		comb2=new JComboBox(nums);
		comb2.addActionListener(this);
		abtButton=new JButton("About IndianGold");
		abtButton.addActionListener(this);

		l1=new JLabel("Number of decimal places: ");
		JPanel p12=new JPanel();
		p12.add(l1);
		p12.add(comb2);
		p12.add(abtButton);

		JPanel p1=new JPanel(new GridLayout(2,1));
		p1.add(p11);
		p1.add(p12);
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Weight Calculations"));

		model  = new PlainTableModel();
		table1=new JTable(model);
		table1.getColumnModel().getColumn(0).setPreferredWidth(190);
		table1.getColumnModel().getColumn(1).setPreferredWidth(150);
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(JLabel.RIGHT );
		table1.getColumnModel().getColumn(0).setCellRenderer( rightRenderer );
		table1.setFocusable(false);
		table1.setBorder(BorderFactory.createLoweredBevelBorder());

		JPanel p2=new JPanel();
		p2.add(table1);
		p2.setBorder(BorderFactory.createEtchedBorder());

		l2=new JLabel("Rate : Rs.");
		text2=new NumberField(5);
		text2.addActionListener(this);
		text2.addFocusListener(this);
		l3=new JLabel("per");
		text3=new NumberField(2);
		text3.addActionListener(this);
		text3.addFocusListener(this);
		comb3=new JComboBox(weightList);
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
		StyleConstants.setBackground(attribs , Color.red);
		costArea.setParagraphAttributes(attribs,true);  
		costArea.setText(getCostString()+"0.00");
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

		text2.setText("0.00");
		text3.setText("10");
		comb1.setSelectedItem(weightList[1]);
		comb2.setSelectedItem("2");
		comb3.setSelectedItem(weightList[1]);
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