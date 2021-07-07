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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Sounak Choudhury
 */
public class AddRemoveBox extends JDialog implements ActionListener, MouseListener, KeyListener, WindowListener
{
	public static final long serialVersionUID = 1L;
	private final JList jl1;
	private final JButton add,edit,remove,save,cancel;
	private final JScrollPane jsp;
        private final JLabel labelVisRows, r3, r4, r5, r6, rl0, rl1, rl2, rl3, rl4, rl5, rl6, rl7, r18, r19, r20, r21, r22;
        private final NumberField text1, text2, text3, text4, text5, text6;
        private final JTextField text19, text20, text21, text22;
        private final JComboBox comboVisDecimals,ljcb2,comboVisRows,rjcb1,rjcb2,rjcb4;
        private final JRadioButton jrb0, jrb1, jrb2, jrb3, jrb4, jrb5, jrb6;
	FileOperations fOps;
        Vector <String>propData;
	Vector <String>propProp;
        Vector <String>weightList;
        private boolean ready = false;
        private final JPanel rp32;
        private final CardLayout cards;
        boolean nowcard = true;
	
	AddRemoveBox(JFrame parent, FileOperations file)
	{
		super(parent, "Settings...");
		fOps=file;
		JPanel p11=new JPanel(new BorderLayout());
		JPanel p12=new JPanel();
		JPanel p1=new JPanel(new BorderLayout());
                JPanel pane=(JPanel)getContentPane();
		JPanel leftPane=new JPanel(new BorderLayout());
		jsp=new JScrollPane();
		jl1=new JList();
		CheckboxListRenderer clr = new CheckboxListRenderer();
		jl1.setCellRenderer(clr);
		jl1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jl1.addMouseListener(this);
		jl1.addKeyListener(this);
		jsp.setViewportView(jl1);
		p11.add(jsp);
		add=new JButton("Add...");
                add.setActionCommand("UNIT_ADD");
		add.addActionListener(this);
		edit=new JButton("Edit...");
                edit.setActionCommand("UNIT_EDIT");
		edit.addActionListener(this);
		remove=new JButton("Remove");
                remove.setActionCommand("UNIT_REMOVE");
		remove.addActionListener(this);
		save=new JButton("OK");
                save.setActionCommand("ALL_SAVE");
		save.addActionListener(this);
		cancel=new JButton("Cancel");
                cancel.setActionCommand("ALL_NOSAVE");
		cancel.addActionListener(this);
		p12.add(add);
		p12.add(edit);
		p12.add(remove);
		p1.add(p11, BorderLayout.NORTH);
		p1.add(p12, BorderLayout.CENTER);                
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Weight Unit List"));

                leftPane.add(p1,BorderLayout.CENTER);
                JPanel p2=new JPanel();
                p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
                p2.add(Box.createRigidArea(new Dimension(0,5)));
                jrb0 = new JRadioButton("Show Indian Gold Calculators");
                jrb0.setActionCommand("RATE_BAR");
                jrb0.addActionListener(this);
                jrb1 = new JRadioButton("Show Metal Rates/Prices Bar");
                jrb1.setActionCommand("RATE_BAR");
                jrb1.addActionListener(this);
                jrb2 = new JRadioButton("Show Both");
                jrb2.setActionCommand("RATE_BAR");
                jrb2.addActionListener(this);
                ButtonGroup bg1=new ButtonGroup();
                bg1.add(jrb0);
                bg1.add(jrb1);
                bg1.add(jrb2);
                p2.add(jrb0);
                p2.add(jrb1);
                p2.add(jrb2);
		p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Show/Hide Interface"));

		leftPane.add(p2,BorderLayout.SOUTH);
		JPanel rightPane=new JPanel();
                rightPane.setLayout(new BorderLayout());
                
                String mins[]=new String[60/2];
                for(int i=0;i<60/2;i++)
                    mins[i]=String.valueOf((i+1)*2);
                JPanel rp2 = new JPanel();
                rp2.setLayout(new BoxLayout(rp2, BoxLayout.PAGE_AXIS));
                JPanel rp20=new JPanel();
                rp20.setLayout(new BoxLayout(rp20, BoxLayout.LINE_AXIS));
                jrb3=new JRadioButton("Fetch market rates of metals manually ");
                jrb3.setActionCommand("RATE_BAR_AUTO");
                jrb3.addActionListener(this);
                rp20.add(jrb3);
                JPanel rp21=new JPanel();
                rp21.setLayout(new BoxLayout(rp21, BoxLayout.LINE_AXIS));
                jrb4=new JRadioButton("Automatically fetch market rates every");
                jrb4.setActionCommand("RATE_BAR_AUTO");
                jrb4.addActionListener(this);
                rjcb4=new JComboBox(mins);
                ButtonGroup bg2=new ButtonGroup();
                bg2.add(jrb3);
                bg2.add(jrb4);
                rl4=new JLabel(" minute(s)");
                rp21.add(jrb4);
                rp21.add(rjcb4);
                rp21.add(rl4);
                rp2.add(rp20);
                rp20.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                rp2.add(rp21);
                rp21.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                JPanel rp22=new JPanel();
                rp22.setLayout(new BoxLayout(rp22, BoxLayout.LINE_AXIS));
                rl0=new JLabel("Select Your Currency : ");
                ljcb2=new JComboBox(new CurrencyComboModel());
                ljcb2.setUI(new CustomComboUI());
                ljcb2.setActionCommand("CURR_CHANGED");
                ljcb2.addActionListener(this);
//                rlsym=new JLabel();
                rp22.add(rl0);
                rl0.setAlignmentX(LEFT_ALIGNMENT);
                rp22.add(ljcb2);
                ljcb2.setBorder(BorderFactory.createEtchedBorder());
                rp2.add(rp22);
                rp22.setAlignmentX(LEFT_ALIGNMENT);                
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
//                JPanel currencyAligner=new JPanel();
//                currencyAligner.setLayout(new BoxLayout(currencyAligner, BoxLayout.LINE_AXIS));
//                currencyAligner.add(Box.createHorizontalGlue());
//                currencyAligner.add(rlsym);
//                currencyAligner.add(Box.createHorizontalGlue());
//                rlsym.setHorizontalAlignment(SwingConstants.CENTER);
//                currencyAligner.setBorder(BorderFactory.createEtchedBorder());
//                currencyAligner.setAlignmentX(LEFT_ALIGNMENT);
//                rp2.add(currencyAligner);
                weightList=new Vector<String>();

                rl1=new JLabel("<html>Rate of <font color=red>Precious Metals</font> is measured in:</html>");
                rp2.add(rl1);
                rl1.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                JPanel rp23=new JPanel();
                rp23.setLayout(new BoxLayout(rp23, BoxLayout.LINE_AXIS));
                rp23.add(Box.createHorizontalStrut(50));
                rl2=new JLabel(fOps.getValue("$currency", "USD"));
                rl2.setBorder(BorderFactory.createEtchedBorder());
                rp23.add(rl2);
                rp23.add(Box.createHorizontalStrut(5));
		rl3=new JLabel("per");
                rp23.add(rl3);
                rp23.add(Box.createHorizontalStrut(5));
		text1=new NumberField(5, false);
                rp23.add(text1);
                rp23.add(Box.createHorizontalStrut(5));
		rjcb1=new JComboBox(weightList);
                rjcb1.setActionCommand("WEIGHT_COMBO_CLICK");
                rjcb1.addActionListener(this);
                rp23.add(rjcb1);
                rp2.add(rp23);
                rp23.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                
		rl5=new JLabel("<html>Rate of <font color=blue>Base Metals</font> is measured in:</html>");
                rp2.add(rl5);
                rl5.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                JPanel rp24=new JPanel();
                rp24.setLayout(new BoxLayout(rp24, BoxLayout.LINE_AXIS));
                rp24.add(Box.createHorizontalStrut(50));
                rl6=new JLabel(fOps.getValue("$currency", "USD"));
                rl6.setBorder(BorderFactory.createEtchedBorder());
                rp24.add(rl6);
                rp24.add(Box.createHorizontalStrut(5));
		rl7=new JLabel("per");
                rp24.add(rl7);
                rp24.add(Box.createHorizontalStrut(5));
		text2=new NumberField(5, false);
                rp24.add(text2);
                rp24.add(Box.createHorizontalStrut(5));
		rjcb2=new JComboBox(weightList);
                rjcb2.setActionCommand("WEIGHT_COMBO_CLICK");
                rjcb2.addActionListener(this);
                rp24.add(rjcb2);
                rp2.add(rp24);
                rp24.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                jrb5=new JRadioButton("Single click to fill the rate and Double click to show/hide calculator");
                jrb5.setActionCommand("RATE_BAR_CLICK");
                jrb5.addActionListener(this);
                jrb6=new JRadioButton("Double click to fill the rate and Right click to show/hide calculator");
                jrb6.setActionCommand("RATE_BAR_CLICK");
                jrb6.addActionListener(this);
                ButtonGroup bg3=new ButtonGroup();
                bg3.add(jrb5);
                bg3.add(jrb6);
                rp2.add(jrb5);
                rp2.add(jrb6);

                rp2.setBorder(BorderFactory.createTitledBorder(
                                  BorderFactory.createEtchedBorder(),
                                  "Market Rates/Prices Bar",
                                  TitledBorder.TRAILING,
                                  TitledBorder.DEFAULT_POSITION));
                rightPane.add(rp2, BorderLayout.CENTER);

                JPanel rp3=new JPanel();
                rp3.setLayout(new BoxLayout(rp3, BoxLayout.PAGE_AXIS));
                JPanel rp31=new JPanel();
                rp31.setLayout(new BoxLayout(rp31, BoxLayout.LINE_AXIS));
                labelVisRows=new JLabel("Visible rows at a time: ");
                labelVisRows.setToolTipText("<html>Select the number of rows to display at a time,<br>in the main window weight conversion table.</html>");
                comboVisRows=new JComboBox(new String[]{"8","9","10","11","12","13","14"});
                rp31.add(labelVisRows);
                labelVisRows.setAlignmentX(LEFT_ALIGNMENT);
                rp31.add(comboVisRows);
		r18=new JLabel("No. of decimal places: ");
                r18.setToolTipText("<html>Select the number of decimal places to display,<br>in the main window weight conversion table.</html>");
		String nums[]=new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12"};
		comboVisDecimals=new JComboBox(nums);
                rp31.add(Box.createRigidArea(new Dimension(10,0)));
		rp31.add(r18);
		rp31.add(comboVisDecimals);
                
                JPanel rp321=new JPanel(new GridLayout(0,8));
                JPanel rp322=new JPanel(new GridLayout(0,8));
                r19 = new JLabel("TAX-1 ", JLabel.RIGHT);
                text19 = new JTextField(3);
                text3 = new NumberField(3, true);
                r3 = new JLabel("0", JLabel.CENTER);
                r20 = new JLabel("TAX-2 ", JLabel.RIGHT);
                text20 = new JTextField(3);
                text4 = new NumberField(3, true);
                r4 = new JLabel("0", JLabel.CENTER);
                r21 = new JLabel("TAX-3 ", JLabel.RIGHT);
                text21 = new JTextField(3);
                text5 = new NumberField(3, true);
                r5 = new JLabel("0", JLabel.CENTER);
                r22 = new JLabel("TAX-4 ", JLabel.RIGHT);
                text22 = new JTextField(3);
                text6 = new NumberField(3, true);
                r6 = new JLabel("0", JLabel.CENTER);
                cards = new CardLayout();
                rp32 = new JPanel(cards);
                rp321.add(r19);
                rp321.add(text3);
                rp321.add(r20);
                rp321.add(text4);
                rp321.add(r21);
                rp321.add(text5);
                rp321.add(r22);
                rp321.add(text6);
                // ###
                rp322.add(text19);
                rp322.add(r3);
                rp322.add(text20);
                rp322.add(r4);
                rp322.add(text21);
                rp322.add(r5);
                rp322.add(text22);
                rp322.add(r6);
                rp32.add(rp321, "TRUE");
                rp32.add(rp322, "FALSE");
                rp32.addMouseListener(this);

                rp3.add(rp31);
                rp3.add(Box.createRigidArea(new Dimension(0,10)));
                rp3.add(rp32);
                rp3.add(Box.createRigidArea(new Dimension(0,5)));
                rp3.setBorder(BorderFactory.createTitledBorder(
                                  BorderFactory.createEtchedBorder(),
                                  "IndianGold Calculator",
                                  TitledBorder.TRAILING,
                                  TitledBorder.DEFAULT_POSITION));
                rightPane.add(rp3, BorderLayout.SOUTH);

                JPanel centerPane=new JPanel();
                centerPane.setLayout(new BorderLayout());
                centerPane.add(leftPane, BorderLayout.LINE_START);
                centerPane.add(rightPane, BorderLayout.LINE_END);
                JPanel bottomPane=new JPanel();
                bottomPane.setLayout(new FlowLayout(FlowLayout.TRAILING));
                bottomPane.add(save);
                bottomPane.add(cancel);
                pane.add(centerPane, BorderLayout.CENTER);
                pane.add(bottomPane, BorderLayout.SOUTH);
                
                reload();
                comboVisRows.setSelectedItem(fOps.getValue("$numrows", "10"));
                comboVisDecimals.setSelectedItem(fOps.getValue("$numdecimals", "2"));
                ljcb2.setSelectedItem((new CurrencyCode(fOps.getValue("$currency", "USD"))).getName());
		displayNumRows(9); //for the list box in AddRemoveBox
                jrb3.setSelected(fOps.getValue("$rateauto", "0").equals("0")); //will depend on settings
                jrb4.setSelected(!fOps.getValue("$rateauto", "0").equals("0")); //will depend on settings
                boolean both=fOps.getValue("$calculator", "1").equals("1") && fOps.getValue("$ratebar", "1").equals("1");
                jrb0.setSelected(fOps.getValue("$calculator", "1").equals("1") && !both); //will depend on settings
                jrb1.setSelected(fOps.getValue("$ratebar", "1").equals("1") && !both); //will depend on settings
                jrb2.setSelected(both); //will depend on settings
                rjcb4.setSelectedItem(fOps.getValue("$rateauto", "2").equals("0")?"2":fOps.getValue("$rateauto", "2"));

                String tmp=fOps.getValue("$punit", "NONE"); // this and following lines for selecting punit and bunit combo boxes
                if(tmp.equals("NONE")) rjcb1.setSelectedIndex(rjcb1.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : rjcb1.getSelectedIndex());
                else rjcb1.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? rjcb1.getItemAt(0) : tmp);
                tmp=fOps.getValue("$bunit", "NONE");
                if(tmp.equals("NONE")) rjcb2.setSelectedIndex(rjcb2.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : rjcb2.getSelectedIndex());
                else rjcb2.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? rjcb2.getItemAt(0) : tmp);
                text1.setText(fOps.getValue("$punitspercurrency", "1"));
                text2.setText(fOps.getValue("$bunitspercurrency", "1"));

                setRateBarConfigEnabled(jrb1.isSelected() || jrb2.isSelected());
                setCalculatorConfigEnabled(jrb0.isSelected() || jrb2.isSelected());
                jrb5.setSelected(fOps.getValue("$clickcondition", "1").equals("1"));
                jrb6.setSelected(fOps.getValue("$clickcondition", "2").equals("2"));
		pack();
		Dimension dim = IndianGold.getScreenCenterLocation(this);
		setLocation(dim.width, dim.height);
		setModal(true);
		addWindowListener(this);
                ready = true;
	}
        
        private void setRateBarConfigEnabled(boolean enabled)
        {
                if(enabled)
                {
//                    rlsym.addMouseListener(this);
                    rl2.addMouseListener(this);
                    rl6.addMouseListener(this);
                    rl1.setText("<html>Rate of <font color=red>Precious Metals</font> is measured in:</html>");
                    rl5.setText("<html>Rate of <font color=blue>Base Metals</font> is measured in:</html>");
                }
                else
                {
//                    rlsym.removeMouseListener(this);
                    rl2.removeMouseListener(this);
                    rl6.removeMouseListener(this);
                    rl1.setText("Rate of Precious Metals is measured in:");
                    rl5.setText("Rate of Base Metals is measured in:");
                }
                jrb3.setEnabled(enabled);
                jrb4.setEnabled(enabled);
                rjcb4.setEnabled(enabled && jrb4.isSelected());
                rl4.setEnabled(enabled);
                rl0.setEnabled(enabled);
                ljcb2.setEnabled(enabled);
//                rlsym.setEnabled(enabled);
                rl1.setEnabled(enabled); // no effect on html label
                rl2.setEnabled(enabled);
                rl3.setEnabled(enabled); // for looks
                text1.setEnabled(enabled);
                rjcb1.setEnabled(enabled);
                rl5.setEnabled(enabled); // no effect on html label
                rl6.setEnabled(enabled);
                rl7.setEnabled(enabled); // for looks
                text2.setEnabled(enabled);
                rjcb2.setEnabled(enabled);
                jrb5.setEnabled(enabled);
                jrb6.setEnabled(enabled);
        }

        private void setCalculatorConfigEnabled(boolean enabled)
        {
            labelVisRows.setEnabled(enabled);
            comboVisRows.setEnabled(enabled);
        }
        
	private void displayNumRows(int rows)
	{
		jl1.setVisibleRowCount(rows);
		jsp.setPreferredSize(jl1.getPreferredScrollableViewportSize());
	}
	
	public void addOperation(String newProp, String newVal, String oldProp, String oldVal)
	{
		Double dbl = 0.00, dbl2=0.00, noOfMilligrams=0.00, dbl3=0.00;
		for(int i=0; i<propProp.size(); i++)
		{
			if((propProp.elementAt(i).startsWith("*") || propProp.elementAt(i).startsWith("_")) &&
                            propProp.elementAt(i).substring(1).equals(oldProp)) //Execute when oldProp equals to propProp element
			{
				try
				{
					dbl = Double.parseDouble(propData.elementAt(i)); //oldProp miligram value
					dbl2 = Double.parseDouble(oldVal); //no of oldProp units
					dbl3 = Double.parseDouble(newVal); //no of new units
				}
				catch(NumberFormatException ne)
				{}
				noOfMilligrams = dbl2/dbl; //no of miligrams which is equal to the new unit
				break;
			}
		}
		String newData = String.valueOf(dbl3/noOfMilligrams); // 1 mg will have this no. of the new unit
		fOps.setValue(newProp,newData);
		reload();
	}

	private void reload()
	{
                String tmp;
		propProp=fOps.getAllUnitNames();
		propData=fOps.getAllUnitValues();
                weightList.removeAllElements();
		CheckableItem ci[] = new CheckableItem[propProp.size()];
		for(int i=0; i<propProp.size(); i++)
		{
			ci[i] = new CheckableItem(propProp.elementAt(i));
                        tmp=propProp.elementAt(i);
                        if(tmp.startsWith("*") || tmp.startsWith("_"))
                            weightList.addElement(tmp.substring(1));
		}
		jl1.setListData(ci);
                rl2.setText(ljcb2.getSelectedItem().toString());
                rl6.setText(ljcb2.getSelectedItem().toString());
                // after reload if this following values doesn't exixt in fOps then 1st item in the following lists get selected
                // this situation occurs at first run when rjcb1 and rjcb2 returns null selected items. And when AddRemoveBox is open and we remove the selected item in rjcb1/rjcb2 from the add remove list.
                tmp = (String)rjcb1.getSelectedItem();
                if(fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) // if this condition satisfies, it also means tmp=null
                {
                    rjcb1.setSelectedIndex(weightList.isEmpty()?-1:0);
                }
                tmp = (String)rjcb2.getSelectedItem();
                if(fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) // if this condition satisfies, it also means tmp=null
                {
                    rjcb2.setSelectedIndex(weightList.isEmpty()?-1:0);
                }
                String taxes[] = fOps.getValue("$taxes", "Tax-1|0.0|Tax-2|0.0|Tax-3|0.0|Tax-4|0.0").split("\\|");
                r19.setText(taxes[0]);
                text3.setText(taxes[1]);
                r20.setText(taxes[2]);
                text4.setText(taxes[3]);
                r21.setText(taxes[4]);
                text5.setText(taxes[5]);
                r22.setText(taxes[6]);
                text6.setText(taxes[7]);
                cards.show(rp32, Boolean.toString(nowcard).toUpperCase());
	}
	
    @Override
	public void actionPerformed(ActionEvent ae)
	{
		//Object obj=ae.getSource();
                String actionCommand=ae.getActionCommand();
//		System.out.println(actionCommand);
		if(actionCommand.equals("UNIT_ADD"))
		{
			String qString[] = MetricAdder.getNewMetric(this, fOps.getAllUnitNames(), "");
			String newProp = qString[0];
			String newVal = qString[1];
			String oldProp = qString[2];
			String oldVal = qString[3];
                        //System.out.println(newProp+", "+newVal+", "+oldProp+", "+oldVal+".");
			if(newProp == null) //Verify if newProp exists. If 1 doesn't exist then all 4 doesn't exists.
			{	//do nothing.
			}
			else if(fOps.getValue(newProp,"NONE").equals("NONE")) //Execute if newProp not exists in propProp
			{
				addOperation(newProp, newVal, oldProp, oldVal);
			}
			else
			{
                            int con=JOptionPane.showConfirmDialog(this,"A metric with the same name already exists.\nReplace it with this one ?");
                            switch (con) {
                                case JOptionPane.NO_OPTION:
                                    add.doClick();
                                    break;
                                case JOptionPane.YES_OPTION:
                                    addOperation(newProp, newVal, oldProp, oldVal);
                                    break;
                            // Do nothing.
                                case JOptionPane.CANCEL_OPTION:
                                case JOptionPane.CLOSED_OPTION:
                                    break;
                                default:
                                    break;
                            }
			}
		}
		else if(actionCommand.equals("UNIT_EDIT"))
		{
			if(propProp.size() <= 1) JOptionPane.showMessageDialog(this,"Last metric is used as a reference and cannot be edited.","Edit Error",JOptionPane.INFORMATION_MESSAGE);
			else if(jl1.getSelectedValue() == null) JOptionPane.showMessageDialog(this,"Nothing is selected to be edited.","Edit Error",JOptionPane.INFORMATION_MESSAGE);
			else
			{
				CheckableItem ci = ((CheckableItem)jl1.getSelectedValue());
				String qString[] = MetricAdder.getNewMetric(this, fOps.getAllUnitNames(), ci.toString());
				String newProp = ci.fullName();
				String newVal = qString[1];
				String oldProp = ("_"+qString[2]).equals(ci.fullName()) ? ci.fullName() : qString[2];
				String oldVal = qString[3];

				if(newVal == null) // if 1 is null then all 4 are null.
				{
					// Do nothing.
				}
				else
				{
					addOperation(newProp, newVal, oldProp, oldVal);
				}
			}
		}
		else if(actionCommand.equals("UNIT_REMOVE"))
		{
			if(propProp.size() <= 1) JOptionPane.showMessageDialog(this,"Last metric is used as a reference and cannot be removed.","Remove Error",JOptionPane.INFORMATION_MESSAGE);
			else if(jl1.getSelectedValue() == null) JOptionPane.showMessageDialog(this,"Nothing is selected to be removed.","Remove Error",JOptionPane.INFORMATION_MESSAGE);
			else
			{
				CheckableItem tmp=(CheckableItem)jl1.getSelectedValue();
				fOps.removeValue(tmp.fullName());
				reload();
//				tmp=null;
			}
		}
		else if(actionCommand.equals("ALL_SAVE"))
		{
                        if(jrb1.isSelected() || jrb2.isSelected()) // this code for setting rateauto, punit, $punitspercurrency, bunit and $bunitspercurrency if rate bar is activated
                        {
                            if(rjcb4.isEnabled()) fOps.setValue("$rateauto", (String)rjcb4.getSelectedItem());
                            else fOps.setValue("$rateauto", "0");

                            fOps.setValue("$punit", (String)rjcb1.getSelectedItem());
                            fOps.setValue("$punitspercurrency", (text1.getText().equals("") || text1.getText().equals("0")) ? "1" : text1.getText());
                            fOps.setValue("$bunit", (String)rjcb2.getSelectedItem());
                            fOps.setValue("$bunitspercurrency", (text2.getText().equals("") || text2.getText().equals("0")) ? "1" : text2.getText());
                        }
                        if(jrb0.isSelected() || jrb2.isSelected()) // this code for setting numrows if calculator is activated
                        {
                            fOps.setValue("$numrows", (String)comboVisRows.getSelectedItem());
                            fOps.setValue("$numdecimals", (String)comboVisDecimals.getSelectedItem());
                        }
                        if(nowcard)
                        {
                            String taxes = r19.getText() + "|" + text3.getText() + "|"
                                           + r20.getText() + "|" + text4.getText() + "|"
                                           + r21.getText() + "|" + text5.getText() + "|"
                                           + r22.getText() + "|" + text6.getText();
                            fOps.setValue("$taxes", taxes);
                        }
			fOps.saveToFile();
			reload();
			dispose();
		}
		else if(actionCommand.equals("ALL_NOSAVE"))
		{
			fOps.discard();
			dispose();
		}
                else if(actionCommand.equals("CURR_CHANGED"))
                {
//                    String tmp=(new CurrencyCode(ljcb2.getSelectedItem().toString())).getName();
//                    rlsym.setText(tmp);
                    rl2.setText(ljcb2.getSelectedItem().toString());
                    rl6.setText(ljcb2.getSelectedItem().toString());
                    fOps.setValue("$currency", (String)ljcb2.getSelectedItem());
                }
                else if(actionCommand.equals("RATE_BAR_AUTO"))
                {
                    rjcb4.setEnabled(jrb4.isSelected());
                }
                else if(actionCommand.equals("RATE_BAR"))
                {
                    setRateBarConfigEnabled(jrb1.isSelected() || jrb2.isSelected());
                    setCalculatorConfigEnabled(jrb0.isSelected() || jrb2.isSelected());
                    fOps.setValue("$ratebar", (jrb1.isSelected() || jrb2.isSelected())?"1":"0");
                    fOps.setValue("$calculator", (jrb0.isSelected() || jrb2.isSelected())?"1":"0");
                }
                else if(actionCommand.equals("RATE_BAR_CLICK"))
                {
                    fOps.setValue("$clickcondition", jrb5.isSelected()?"1":"2");
                }
                else if(actionCommand.equals("WEIGHT_COMBO_CLICK") && ready)
                {
                    String sel1 = rjcb1.getSelectedItem().toString();
                    String sel2 = rjcb2.getSelectedItem().toString();
                    for(int var=0; var < jl1.getModel().getSize(); var++)
                    {
                        CheckableItem curElem = (CheckableItem)jl1.getModel().getElementAt(var);
                        if(sel1.equals(curElem.toString()) && !curElem.isSelected() )
                        {
                            JOptionPane.showMessageDialog(this,curElem.toString()+" unit is not checked in the unit list. Please check enable it first.","Selection Error",JOptionPane.INFORMATION_MESSAGE);
                            String tmp=fOps.getValue("$punit", "NONE"); // this and following lines for selecting punit and bunit combo boxes
                            if(tmp.equals("NONE")) rjcb1.setSelectedIndex(rjcb1.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : rjcb1.getSelectedIndex());
                            else rjcb1.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? rjcb1.getItemAt(0) : tmp);
                        }
                        else if(sel2.equals(curElem.toString()) && !curElem.isSelected())
                        {
                            JOptionPane.showMessageDialog(this,curElem.toString()+" unit is not checked in the unit list. Please check enable it first.","Selection Error",JOptionPane.INFORMATION_MESSAGE);
                            String tmp=fOps.getValue("$bunit", "NONE");
                            if(tmp.equals("NONE")) rjcb2.setSelectedIndex(rjcb2.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : rjcb2.getSelectedIndex());
                            else rjcb2.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? rjcb2.getItemAt(0) : tmp);
                        }
                    }
                }
	}

    @Override
	public void mousePressed(MouseEvent me)
	{
	}

    @Override
	public void mouseReleased(MouseEvent me)
	{
	}

    @Override
	public void mouseEntered(MouseEvent me)
	{
	}

    @Override
	public void mouseExited(MouseEvent me)
	{
	}

    @Override
	public void mouseClicked(MouseEvent me)
	{
            Component src=me.getComponent();
            if(src.equals(jl1))
            {
		int x = jl1.locationToIndex(me.getPoint());
		CheckableItem ci = (CheckableItem)jl1.getModel().getElementAt(x);
		String buff = fOps.getValue(ci.fullName(), "NONE");
		if(!buff.equals("NONE"))
		{
			fOps.removeValue(ci.fullName()); // full name before click
			ci.setSelected(!ci.isSelected());
			Rectangle rect = jl1.getCellBounds(x,x);
			jl1.repaint(rect);
			fOps.setValue(ci.fullName(), buff); // full name after click (this will add/remove the _ based on boolean code 2 lines up)
			//reload();		reload not done as no new item is added/removed and checking is already visible through input events
		}
            }
            else if(src.equals(rl2) || src.equals(rl6))
            {
                ljcb2.showPopup();
            }
            else if(src.equals(rp32))
            {
                nowcard = !nowcard;
                if(nowcard)
                {
                    r19.setText(text19.getText());
                    r20.setText(text20.getText());
                    r21.setText(text21.getText());
                    r22.setText(text22.getText());
                    text3.setText(r3.getText());
                    text4.setText(r4.getText());
                    text5.setText(r5.getText());
                    text6.setText(r6.getText());
                }
                else
                {
                    text19.setText(r19.getText());
                    text20.setText(r20.getText());
                    text21.setText(r21.getText());
                    text22.setText(r22.getText());
                    r3.setText(text3.getText());
                    r4.setText(text4.getText());
                    r5.setText(text5.getText());
                    r6.setText(text6.getText());
                }
                cards.show(rp32, Boolean.toString(nowcard).toUpperCase());
            }
	}

    @Override
	public void keyPressed(KeyEvent ke)
	{
	}

    @Override
	public void keyReleased(KeyEvent ke)
	{
	}

    @Override
	public void keyTyped(KeyEvent ke)
	{
		if(!jl1.isSelectionEmpty() && ke.getKeyChar() == KeyEvent.VK_SPACE)
		{
			int i = jl1.getSelectedIndex();
			if(i >= jl1.getFirstVisibleIndex() && i <= jl1.getLastVisibleIndex())
			{
				int x = jl1.getSelectedIndex();
				CheckableItem ci = (CheckableItem)jl1.getModel().getElementAt(x);
				String buff = fOps.getValue(ci.fullName(), "NONE");
				if(!buff.equals("NONE"))
				{
					fOps.removeValue(ci.fullName()); // full name before click
					ci.setSelected(!ci.isSelected());
					Rectangle rect = jl1.getCellBounds(x,x);
					jl1.repaint(rect);
					fOps.setValue(ci.fullName(), buff); // full name after click (this will add/remove the _ based on boolean code 2 lines up)
					//reload();		reload not done as no new item is added/removed and checking is already visible through input events
				}
			}
		}
	}

    @Override
	public void windowOpened(WindowEvent we)
	{
	}

    @Override
	public void windowClosed(WindowEvent we)
	{
	}

    @Override
	public void windowDeiconified(WindowEvent we)
	{
	}

    @Override
	public void windowIconified(WindowEvent we)
	{
	}

    @Override
	public void windowClosing(WindowEvent we)
	{
		fOps.discard();
		dispose();
	}

    @Override
	public void windowActivated(WindowEvent we)
	{
	}

    @Override
	public void windowDeactivated(WindowEvent we)
	{
	}

	/*public static void main(String args[]) //for standalone testing
	{
		FileOperations flop=new FileOperations(new File("Myland.prop"),"Property");
		AddRemoveBox xx = new AddRemoveBox(new JFrame(), flop);
		xx.setVisible(true);
	}*/
}
