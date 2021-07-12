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
public class AddRemoveBox extends JDialog
{
	public static final long serialVersionUID = 1L;
	private final JList unitEditableList;
	private final JButton buttonAdd,buttonEdit,buttonRemove,buttonSave,buttonCancel;
	private final JScrollPane unitListScrollPane;
        private final JLabel labelVisRows, labelComboCurrency, labelPreciousWeight, labelCurrencyPreciousWeight, labelPer1, labelClicks, labelMinutes, labelBaseWeight, labelCurrencyBaseWeight, labelPer2, labelVisDecimals;
        private final NumberField numPreciousWeight, numBaseWeight;
        private final JComboBox comboVisDecimals,comboCurrency,comboVisRows,comboPreciousWeightUnit,comboBaseWeightUnit,comboAutoFetchMinutes;
        private final JRadioButton rbCalculator, rbRateBar, rbBoth, rbManualFetchRates, rbAutoFetchRates, rbRateBarClickPolicy1, rbRateBarClickPolicy2;
	FileOperations fOps;
        MouseClicks clickAdapter;
        ActionAdapter actionAdapter;
        Vector <String>propData;
	Vector <String>propProp;
        Vector <String>weightList;
        private boolean ready = false;
	private final JDialog thisone;
        
	AddRemoveBox(JFrame parent, FileOperations file)
	{
		super(parent, "Settings...");
                thisone = this;
                JPanel pane=(JPanel)super.getContentPane();
		fOps=file;
		JPanel p11=new JPanel(new BorderLayout());
		JPanel p12=new JPanel();
		JPanel p1=new JPanel(new BorderLayout());
		JPanel leftPane=new JPanel(new BorderLayout());
		unitListScrollPane=new JScrollPane();
		unitEditableList=new JList();
		CheckboxListRenderer clr = new CheckboxListRenderer();
                actionAdapter = new ActionAdapter();
                clickAdapter = new MouseClicks();
		unitEditableList.setCellRenderer(clr);
		unitEditableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		unitEditableList.addMouseListener(clickAdapter);
		unitEditableList.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent ke)
                    {
                        if(!unitEditableList.isSelectionEmpty() && ke.getKeyChar() == KeyEvent.VK_SPACE)
                        {
                            int i = unitEditableList.getSelectedIndex();
                            if(i >= unitEditableList.getFirstVisibleIndex() && i <= unitEditableList.getLastVisibleIndex())
                            {
                                int x = unitEditableList.getSelectedIndex();
                                CheckableItem ci = (CheckableItem)unitEditableList.getModel().getElementAt(x);
                                String buff = fOps.getValue(ci.fullName(), "NONE");
                                if(!buff.equals("NONE"))
                                {
                                    fOps.removeValue(ci.fullName()); // full name before click
                                    ci.setSelected(!ci.isSelected());
                                    Rectangle rect = unitEditableList.getCellBounds(x,x);
                                    unitEditableList.repaint(rect);
                                    fOps.setValue(ci.fullName(), buff); // full name after click (this will add/remove the _ based on boolean code 2 lines up)
                                    //reload();		reload not done as no new item is added/removed and checking is already visible through input events
                                }
                            }
                        }
                    }
                });
		unitListScrollPane.setViewportView(unitEditableList);
		p11.add(unitListScrollPane);
		buttonAdd=new JButton("Add...");
                buttonAdd.setActionCommand("UNIT_ADD");
		buttonAdd.addActionListener(actionAdapter);
		buttonEdit=new JButton("Edit...");
                buttonEdit.setActionCommand("UNIT_EDIT");
		buttonEdit.addActionListener(actionAdapter);
		buttonRemove=new JButton("Remove");
                buttonRemove.setActionCommand("UNIT_REMOVE");
		buttonRemove.addActionListener(actionAdapter);
		buttonSave=new JButton("OK");
                buttonSave.setActionCommand("ALL_SAVE");
		buttonSave.addActionListener(actionAdapter);
		buttonCancel=new JButton("Cancel");
                buttonCancel.setActionCommand("ALL_NOSAVE");
		buttonCancel.addActionListener(actionAdapter);
		p12.add(buttonAdd);
		p12.add(buttonEdit);
		p12.add(buttonRemove);
		p1.add(p11, BorderLayout.NORTH);
		p1.add(p12, BorderLayout.CENTER);                
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Weight Unit List"));

                leftPane.add(p1,BorderLayout.CENTER);
                JPanel p2=new JPanel();
                p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
                p2.add(Box.createRigidArea(new Dimension(0,5)));
                rbCalculator = new JRadioButton("Show Indian Gold Calculators");
                rbCalculator.setActionCommand("RATE_BAR");
                rbCalculator.addActionListener(actionAdapter);
                rbRateBar = new JRadioButton("Show Metal Rates/Prices Bar");
                rbRateBar.setActionCommand("RATE_BAR");
                rbRateBar.addActionListener(actionAdapter);
                rbBoth = new JRadioButton("Show Both");
                rbBoth.setActionCommand("RATE_BAR");
                rbBoth.addActionListener(actionAdapter);
                ButtonGroup bg1=new ButtonGroup();
                bg1.add(rbCalculator);
                bg1.add(rbRateBar);
                bg1.add(rbBoth);
                p2.add(rbCalculator);
                p2.add(rbRateBar);
                p2.add(rbBoth);
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
                rbManualFetchRates=new JRadioButton("Fetch market rates of metals manually on");
                rbManualFetchRates.setActionCommand("RATE_BAR_AUTO");
                rbManualFetchRates.addActionListener(actionAdapter);
                labelClicks=new JLabel("left click");
                rp20.add(rbManualFetchRates);
                rp20.add(labelClicks);
                JPanel rp21=new JPanel();
                rp21.setLayout(new BoxLayout(rp21, BoxLayout.LINE_AXIS));
                rbAutoFetchRates=new JRadioButton("Automatically fetch market rates every");
                rbAutoFetchRates.setActionCommand("RATE_BAR_AUTO");
                rbAutoFetchRates.addActionListener(actionAdapter);
                comboAutoFetchMinutes=new JComboBox(mins);
                ButtonGroup bg2=new ButtonGroup();
                bg2.add(rbManualFetchRates);
                bg2.add(rbAutoFetchRates);
                labelMinutes=new JLabel(" minute(s)");
                rp21.add(rbAutoFetchRates);
                rp21.add(comboAutoFetchMinutes);
                rp21.add(labelMinutes);
                rp2.add(rp20);
                rp20.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                rp2.add(rp21);
                rp21.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                JPanel rp22=new JPanel();
                rp22.setLayout(new BoxLayout(rp22, BoxLayout.LINE_AXIS));
                labelComboCurrency=new JLabel("Select Your Currency : ");
                comboCurrency=new JComboBox(new CurrencyComboModel());
                comboCurrency.setUI(new CustomComboUI());
                comboCurrency.setActionCommand("CURR_CHANGED");
                comboCurrency.addActionListener(actionAdapter);
                rp22.add(labelComboCurrency);
                labelComboCurrency.setAlignmentX(LEFT_ALIGNMENT);
                rp22.add(comboCurrency);
                comboCurrency.setBorder(BorderFactory.createEtchedBorder());
                rp2.add(rp22);
                rp22.setAlignmentX(LEFT_ALIGNMENT);                
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                weightList=new Vector<String>();

                labelPreciousWeight=new JLabel("<html>Rate of <font color=red>Precious Metals</font> is measured in:</html>");
                rp2.add(labelPreciousWeight);
                labelPreciousWeight.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                JPanel rp23=new JPanel();
                rp23.setLayout(new BoxLayout(rp23, BoxLayout.LINE_AXIS));
                rp23.add(Box.createHorizontalStrut(50));
                labelCurrencyPreciousWeight=new JLabel(fOps.getValue("$currency", "USD"));
                labelCurrencyPreciousWeight.setBorder(BorderFactory.createEtchedBorder());
                rp23.add(labelCurrencyPreciousWeight);
                rp23.add(Box.createHorizontalStrut(5));
		labelPer1=new JLabel("per");
                rp23.add(labelPer1);
                rp23.add(Box.createHorizontalStrut(5));
		numPreciousWeight=new NumberField(5, false);
                rp23.add(numPreciousWeight);
                rp23.add(Box.createHorizontalStrut(5));
		comboPreciousWeightUnit=new JComboBox(weightList);
                comboPreciousWeightUnit.setActionCommand("WEIGHT_COMBO_CLICK");
                comboPreciousWeightUnit.addActionListener(actionAdapter);
                rp23.add(comboPreciousWeightUnit);
                rp2.add(rp23);
                rp23.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                
		labelBaseWeight=new JLabel("<html>Rate of <font color=blue>Base Metals</font> is measured in:</html>");
                rp2.add(labelBaseWeight);
                labelBaseWeight.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                JPanel rp24=new JPanel();
                rp24.setLayout(new BoxLayout(rp24, BoxLayout.LINE_AXIS));
                rp24.add(Box.createHorizontalStrut(50));
                labelCurrencyBaseWeight=new JLabel(fOps.getValue("$currency", "USD"));
                labelCurrencyBaseWeight.setBorder(BorderFactory.createEtchedBorder());
                rp24.add(labelCurrencyBaseWeight);
                rp24.add(Box.createHorizontalStrut(5));
		labelPer2=new JLabel("per");
                rp24.add(labelPer2);
                rp24.add(Box.createHorizontalStrut(5));
		numBaseWeight=new NumberField(5, false);
                rp24.add(numBaseWeight);
                rp24.add(Box.createHorizontalStrut(5));
		comboBaseWeightUnit=new JComboBox(weightList);
                comboBaseWeightUnit.setActionCommand("WEIGHT_COMBO_CLICK");
                comboBaseWeightUnit.addActionListener(actionAdapter);
                rp24.add(comboBaseWeightUnit);
                rp2.add(rp24);
                rp24.setAlignmentX(LEFT_ALIGNMENT);
                rp2.add(Box.createRigidArea(new Dimension(0,10)));
                rbRateBarClickPolicy1=new JRadioButton("Single click to fill the rate and Double click to show/hide calculator");
                rbRateBarClickPolicy1.setActionCommand("RATE_BAR_CLICK");
                rbRateBarClickPolicy1.addActionListener(actionAdapter);
                rbRateBarClickPolicy2=new JRadioButton("Double click to fill the rate and Right click to show/hide calculator");
                rbRateBarClickPolicy2.setActionCommand("RATE_BAR_CLICK");
                rbRateBarClickPolicy2.addActionListener(actionAdapter);
                ButtonGroup bg3=new ButtonGroup();
                bg3.add(rbRateBarClickPolicy1);
                bg3.add(rbRateBarClickPolicy2);
                rp2.add(rbRateBarClickPolicy1);
                rp2.add(rbRateBarClickPolicy2);

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
		labelVisDecimals=new JLabel("No. of decimal places: ");
                labelVisDecimals.setToolTipText("<html>Select the number of decimal places to display,<br>in the main window weight conversion table.</html>");
		String nums[]=new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12"};
		comboVisDecimals=new JComboBox(nums);
                rp31.add(Box.createRigidArea(new Dimension(10,0)));
		rp31.add(labelVisDecimals);
		rp31.add(comboVisDecimals);
                
                rp3.add(rp31);
                rp3.add(Box.createRigidArea(new Dimension(0,10)));
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
                bottomPane.add(buttonSave);
                bottomPane.add(buttonCancel);
                pane.add(centerPane, BorderLayout.CENTER);
                pane.add(bottomPane, BorderLayout.SOUTH);
                
                reload();
                comboVisRows.setSelectedItem(fOps.getValue("$numrows", "10"));
                comboVisDecimals.setSelectedItem(fOps.getValue("$numdecimals", "2"));
                comboCurrency.setSelectedItem((new CurrencyCode(fOps.getValue("$currency", "USD"))).getName());
		displayNumRows(9); //for the list box in AddRemoveBox
                rbManualFetchRates.setSelected(fOps.getValue("$rateauto", "0").equals("0")); //will depend on settings
                rbAutoFetchRates.setSelected(!fOps.getValue("$rateauto", "0").equals("0")); //will depend on settings
                boolean both=fOps.getValue("$calculator", "1").equals("1") && fOps.getValue("$ratebar", "1").equals("1");
                rbCalculator.setSelected(fOps.getValue("$calculator", "1").equals("1") && !both); //will depend on settings
                rbRateBar.setSelected(fOps.getValue("$ratebar", "1").equals("1") && !both); //will depend on settings
                rbBoth.setSelected(both); //will depend on settings
                comboAutoFetchMinutes.setSelectedItem(fOps.getValue("$rateauto", "2").equals("0")?"2":fOps.getValue("$rateauto", "2"));

                String tmp=fOps.getValue("$punit", "NONE"); // this and following lines for selecting punit and bunit combo boxes
                if(tmp.equals("NONE")) comboPreciousWeightUnit.setSelectedIndex(comboPreciousWeightUnit.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : comboPreciousWeightUnit.getSelectedIndex());
                else comboPreciousWeightUnit.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? comboPreciousWeightUnit.getItemAt(0) : tmp);
                tmp=fOps.getValue("$bunit", "NONE");
                if(tmp.equals("NONE")) comboBaseWeightUnit.setSelectedIndex(comboBaseWeightUnit.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : comboBaseWeightUnit.getSelectedIndex());
                else comboBaseWeightUnit.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? comboBaseWeightUnit.getItemAt(0) : tmp);
                numPreciousWeight.setText(fOps.getValue("$punitspercurrency", "1"));
                numBaseWeight.setText(fOps.getValue("$bunitspercurrency", "1"));

                setRateBarConfigEnabled(rbRateBar.isSelected() || rbBoth.isSelected());
                setCalculatorConfigEnabled(rbCalculator.isSelected() || rbBoth.isSelected());
                rbRateBarClickPolicy1.setSelected(fOps.getValue("$clickcondition", "1").equals("1"));
                rbRateBarClickPolicy2.setSelected(fOps.getValue("$clickcondition", "2").equals("2"));
                labelClicks.setText(rbRateBarClickPolicy1.isSelected()?"right click":"left click");
                init();
                ready = true;
	}
        
        private void init()
        {
		pack();
		Dimension dim = IndianGold.getScreenCenterLocation(thisone);
		setLocation(dim.width, dim.height);
		setModal(true);
		addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent we)
                    {
                            fOps.discard();
                            dispose();
                    }
                });
        }
        
        private void setRateBarConfigEnabled(boolean enabled)
        {
                if(enabled)
                {
                    labelCurrencyPreciousWeight.addMouseListener(clickAdapter);
                    labelCurrencyBaseWeight.addMouseListener(clickAdapter);
                    labelPreciousWeight.setText("<html>Rate of <font color=red>Precious Metals</font> is measured in:</html>");
                    labelBaseWeight.setText("<html>Rate of <font color=blue>Base Metals</font> is measured in:</html>");
                }
                else
                {
                    labelCurrencyPreciousWeight.removeMouseListener(clickAdapter);
                    labelCurrencyBaseWeight.removeMouseListener(clickAdapter);
                    labelPreciousWeight.setText("Rate of Precious Metals is measured in:");
                    labelBaseWeight.setText("Rate of Base Metals is measured in:");
                }
                rbManualFetchRates.setEnabled(enabled);
                labelClicks.setEnabled(enabled);
                rbAutoFetchRates.setEnabled(enabled);
                comboAutoFetchMinutes.setEnabled(enabled && rbAutoFetchRates.isSelected());
                labelMinutes.setEnabled(enabled);
                labelComboCurrency.setEnabled(enabled);
                comboCurrency.setEnabled(enabled);
                labelPreciousWeight.setEnabled(enabled); // no effect on html label
                labelCurrencyPreciousWeight.setEnabled(enabled);
                labelPer1.setEnabled(enabled); // for looks
                numPreciousWeight.setEnabled(enabled);
                comboPreciousWeightUnit.setEnabled(enabled);
                labelBaseWeight.setEnabled(enabled); // no effect on html label
                labelCurrencyBaseWeight.setEnabled(enabled);
                labelPer2.setEnabled(enabled); // for looks
                numBaseWeight.setEnabled(enabled);
                comboBaseWeightUnit.setEnabled(enabled);
                rbRateBarClickPolicy1.setEnabled(enabled);
                rbRateBarClickPolicy2.setEnabled(enabled);
        }

        private void setCalculatorConfigEnabled(boolean enabled)
        {
            labelVisRows.setEnabled(enabled);
            comboVisRows.setEnabled(enabled);
            labelVisDecimals.setEnabled(enabled);
            comboVisDecimals.setEnabled(enabled);
        }
        
	private void displayNumRows(int rows)
	{
		unitEditableList.setVisibleRowCount(rows);
		unitListScrollPane.setPreferredSize(unitEditableList.getPreferredScrollableViewportSize());
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
		unitEditableList.setListData(ci);
                labelCurrencyPreciousWeight.setText(comboCurrency.getSelectedItem().toString());
                labelCurrencyBaseWeight.setText(comboCurrency.getSelectedItem().toString());
                // after reload if this following values doesn't exixt in fOps then 1st item in the following lists get selected
                // this situation occurs at first run when rjcb1 and rjcb2 returns null selected items. And when AddRemoveBox is open and we remove the selected item in rjcb1/rjcb2 from the add remove list.
                tmp = (String)comboPreciousWeightUnit.getSelectedItem();
                if(fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) // if this condition satisfies, it also means tmp=null
                {
                    comboPreciousWeightUnit.setSelectedIndex(weightList.isEmpty()?-1:0);
                }
                tmp = (String)comboBaseWeightUnit.getSelectedItem();
                if(fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) // if this condition satisfies, it also means tmp=null
                {
                    comboBaseWeightUnit.setSelectedIndex(weightList.isEmpty()?-1:0);
                }
	}
	
        
    class ActionAdapter implements ActionListener
    {   
        @Override
	public void actionPerformed(ActionEvent ae)
	{
		//Object obj=ae.getSource();
                String actionCommand=ae.getActionCommand();
//		System.out.println(actionCommand);
		if(actionCommand.equals("UNIT_ADD"))
		{
			String qString[] = MetricAdder.getNewMetric(thisone, fOps.getAllUnitNames(), "");
			String newProp = qString[0];
			String newVal = qString[1];
			String oldProp = qString[2];
			String oldVal = qString[3];
                        //System.out.println(newProp+", "+newVal+", "+oldProp+", "+oldVal+".");
			if(newProp == null) //Verify if newProp exists. If 1 doesn't exist then all 4 doesn't exists.
			{	//do nothing.
			}
			else if(fOps.getValue(newProp.substring(1),"NONE").substring(1).equals("NONE")) //Execute if newProp not exists in propProp
			{
				addOperation(newProp, newVal, oldProp, oldVal);
			}
			else
			{
                            int con=JOptionPane.showConfirmDialog(thisone,"A metric with the same name " + newProp.substring(1) + " already exists.\nReplace it with this one ?");
                            switch (con) {
                                case JOptionPane.NO_OPTION:
                                    buttonAdd.doClick();
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
			if(propProp.size() <= 1) JOptionPane.showMessageDialog(thisone,"Last metric is used as a reference and cannot be edited.","Edit Error",JOptionPane.INFORMATION_MESSAGE);
			else if(unitEditableList.getSelectedValue() == null) JOptionPane.showMessageDialog(thisone,"Nothing is selected to be edited.","Edit Error",JOptionPane.INFORMATION_MESSAGE);
			else
			{
				CheckableItem ci = ((CheckableItem)unitEditableList.getSelectedValue());
				String qString[] = MetricAdder.getNewMetric(thisone, fOps.getAllUnitNames(), ci.toString());
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
			if(propProp.size() <= 1) JOptionPane.showMessageDialog(thisone,"Last metric is used as a reference and cannot be removed.","Remove Error",JOptionPane.INFORMATION_MESSAGE);
			else if(unitEditableList.getSelectedValue() == null) JOptionPane.showMessageDialog(thisone,"Nothing is selected to be removed.","Remove Error",JOptionPane.INFORMATION_MESSAGE);
			else
			{
				CheckableItem tmp=(CheckableItem)unitEditableList.getSelectedValue();
				fOps.removeValue(tmp.fullName());
				reload();
//				tmp=null;
			}
		}
		else if(actionCommand.equals("ALL_SAVE"))
		{
                        if(rbRateBar.isSelected() || rbBoth.isSelected()) // this code for setting rateauto, punit, $punitspercurrency, bunit and $bunitspercurrency if rate bar is activated
                        {
                            if(comboAutoFetchMinutes.isEnabled()) fOps.setValue("$rateauto", (String)comboAutoFetchMinutes.getSelectedItem());
                            else fOps.setValue("$rateauto", "0");

                            fOps.setValue("$punit", (String)comboPreciousWeightUnit.getSelectedItem());
                            fOps.setValue("$punitspercurrency", (numPreciousWeight.getText().equals("") || numPreciousWeight.getText().equals("0")) ? "1" : numPreciousWeight.getText());
                            fOps.setValue("$bunit", (String)comboBaseWeightUnit.getSelectedItem());
                            fOps.setValue("$bunitspercurrency", (numBaseWeight.getText().equals("") || numBaseWeight.getText().equals("0")) ? "1" : numBaseWeight.getText());
                        }
                        if(rbCalculator.isSelected() || rbBoth.isSelected()) // this code for setting numrows if calculator is activated
                        {
                            fOps.setValue("$numrows", (String)comboVisRows.getSelectedItem());
                            fOps.setValue("$numdecimals", (String)comboVisDecimals.getSelectedItem());
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
                    labelCurrencyPreciousWeight.setText(comboCurrency.getSelectedItem().toString());
                    labelCurrencyBaseWeight.setText(comboCurrency.getSelectedItem().toString());
                    fOps.setValue("$currency", (String)comboCurrency.getSelectedItem());
                }
                else if(actionCommand.equals("RATE_BAR_AUTO"))
                {
                    comboAutoFetchMinutes.setEnabled(rbAutoFetchRates.isSelected());
                }
                else if(actionCommand.equals("RATE_BAR"))
                {
                    setRateBarConfigEnabled(rbRateBar.isSelected() || rbBoth.isSelected());
                    setCalculatorConfigEnabled(rbCalculator.isSelected() || rbBoth.isSelected());
                    fOps.setValue("$ratebar", (rbRateBar.isSelected() || rbBoth.isSelected())?"1":"0");
                    fOps.setValue("$calculator", (rbCalculator.isSelected() || rbBoth.isSelected())?"1":"0");
                }
                else if(actionCommand.equals("RATE_BAR_CLICK"))
                {
                    fOps.setValue("$clickcondition", rbRateBarClickPolicy1.isSelected()?"1":"2");
                    labelClicks.setText(rbRateBarClickPolicy1.isSelected()?"right click":"left click");
                }
                else if(actionCommand.equals("WEIGHT_COMBO_CLICK") && ready)
                {
                    String sel1 = comboPreciousWeightUnit.getSelectedItem().toString();
                    String sel2 = comboBaseWeightUnit.getSelectedItem().toString();
                    for(int var=0; var < unitEditableList.getModel().getSize(); var++)
                    {
                        CheckableItem curElem = (CheckableItem)unitEditableList.getModel().getElementAt(var);
                        if(sel1.equals(curElem.toString()) && !curElem.isSelected() )
                        {
                            JOptionPane.showMessageDialog(thisone,curElem.toString()+" unit is not checked in the unit list. Please check enable it first.","Selection Error",JOptionPane.INFORMATION_MESSAGE);
                            String tmp=fOps.getValue("$punit", "NONE"); // this and following lines for selecting punit and bunit combo boxes
                            if(tmp.equals("NONE")) comboPreciousWeightUnit.setSelectedIndex(comboPreciousWeightUnit.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : comboPreciousWeightUnit.getSelectedIndex());
                            else comboPreciousWeightUnit.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? comboPreciousWeightUnit.getItemAt(0) : tmp);
                        }
                        else if(sel2.equals(curElem.toString()) && !curElem.isSelected())
                        {
                            JOptionPane.showMessageDialog(thisone,curElem.toString()+" unit is not checked in the unit list. Please check enable it first.","Selection Error",JOptionPane.INFORMATION_MESSAGE);
                            String tmp=fOps.getValue("$bunit", "NONE");
                            if(tmp.equals("NONE")) comboBaseWeightUnit.setSelectedIndex(comboBaseWeightUnit.getSelectedIndex()==-1 ? (weightList.isEmpty()?-1:0) : comboBaseWeightUnit.getSelectedIndex());
                            else comboBaseWeightUnit.setSelectedItem((fOps.getValue("*"+tmp, "NONE").equals("NONE") && fOps.getValue("_"+tmp, "NONE").equals("NONE")) ? comboBaseWeightUnit.getItemAt(0) : tmp);
                        }
                    }
                }
	}
    }

    class MouseClicks extends MouseAdapter
    {
        @Override
	public void mouseClicked(MouseEvent me)
	{
            Component src=me.getComponent();
            if(src.equals(unitEditableList))
            {
		int x = unitEditableList.locationToIndex(me.getPoint());
		CheckableItem ci = (CheckableItem)unitEditableList.getModel().getElementAt(x);
		String buff = fOps.getValue(ci.fullName(), "NONE");
		if(!buff.equals("NONE"))
		{
			fOps.removeValue(ci.fullName()); // full name before click
			ci.setSelected(!ci.isSelected());
			Rectangle rect = unitEditableList.getCellBounds(x,x);
			unitEditableList.repaint(rect);
			fOps.setValue(ci.fullName(), buff); // full name after click (this will add/remove the _ based on boolean code 2 lines up)
			//reload();		reload not done as no new item is added/removed and checking is already visible through input events
		}
            }
            else if(src.equals(labelCurrencyPreciousWeight) || src.equals(labelCurrencyBaseWeight))
            {
                comboCurrency.showPopup();
            }
	}
    }
	/*public static void main(String args[]) //for standalone testing
	{
		FileOperations flop=new FileOperations(new File("Myland.prop"),"Property");
		AddRemoveBox xx = new AddRemoveBox(new JFrame(), flop);
		xx.setVisible(true);
	}*/
}
