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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
        private final JLabel labelVisRows, labelVisDecimals;
        private final MarketSettings marketSettings;
        private final com.sounaks.indiangold.rates.RateService rateService;
        private final MarketRatesPanel marketPanel;
        private final JComboBox<CountryDefaults.Country> countryBox;
        private final JComboBox<CurrencyCatalog.Choice> currencyBox;
        private final JComboBox comboVisDecimals,comboVisRows;
        private final JRadioButton rbCalculator, rbRateBar, rbBoth, rbRateBarClickPolicy1, rbRateBarClickPolicy2;
	FileOperations fOps;
        MouseClicks clickAdapter;
        ActionAdapter actionAdapter;
        Vector <String>propData;
	Vector <String>propProp;
        private boolean ready = false;
	private final JDialog thisone;
        
	AddRemoveBox(JFrame parent, FileOperations file, MarketSettings marketSettings, com.sounaks.indiangold.rates.RateService rateService)
	{
		super(parent, "Settings...");
		this.marketSettings = marketSettings;
		this.rateService = rateService;
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
                            toggleUnit(unitEditableList.getSelectedIndex());
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
		p1.add(p11, BorderLayout.CENTER); // the list takes the free space
		p1.add(p12, BorderLayout.SOUTH);
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


                // General tab: country, currency, what to show and how clicks on the rate bar work.
                CountryDefaults countryTable = CountryDefaults.load();
                countryBox = CountryDialog.countryBox(countryTable, marketSettings.country().orElse(CountryDefaults.systemCountry()));
                countryBox.addActionListener(e -> fOps.setValue("$country", ((CountryDefaults.Country)countryBox.getSelectedItem()).code()));
                JButton applyCountry = new JButton("Apply country defaults...");
                applyCountry.setToolTipText("Sets the currency, the units rates are shown in, the gold rows and the taxes usual in this country");
                currencyBox = new JComboBox<>(CurrencyCatalog.choices(rateService.current().fx(), marketSettings.currency()).toArray(CurrencyCatalog.Choice[]::new));
                currencyBox.setMaximumRowCount(20);
                selectCurrency(marketSettings.currency());
                marketPanel = new MarketRatesPanel(this, fOps, marketSettings, rateService);
                currencyBox.addActionListener(e -> {
                    CurrencyCatalog.Choice choice = (CurrencyCatalog.Choice)currencyBox.getSelectedItem();
                    if(choice != null) marketSettings.setCurrency(choice.currency().getCurrencyCode());
                    marketPanel.refreshSummary();
                });
                applyCountry.addActionListener(e -> {
                    CountryDefaults.Country country = (CountryDefaults.Country)countryBox.getSelectedItem();
                    int answer = JOptionPane.showConfirmDialog(thisone, "Set the currency, rate units, gold rows and taxes usual in " + country.name() + "?\n"
                            + "Your own choices for these will be replaced.", "Apply country defaults", JOptionPane.OK_CANCEL_OPTION);
                    if(answer != JOptionPane.OK_OPTION) return;
                    marketSettings.applyCountry(countryTable.forCountry(country.code()));
                    selectCurrency(marketSettings.currency());
                    marketPanel.refreshSummary();
                    reload();
                });
                JPanel place = new JPanel(new GridBagLayout());
                place.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Country and currency"));
                GridBagConstraints gc = new GridBagConstraints();
                gc.insets = new Insets(4, 4, 4, 4);
                gc.anchor = GridBagConstraints.LINE_START;
                gc.gridx = 0; gc.gridy = 0;
                place.add(new JLabel("Country:"), gc);
                gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
                place.add(countryBox, gc);
                gc.gridx = 2; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
                place.add(applyCountry, gc);
                gc.gridx = 0; gc.gridy = 1;
                place.add(new JLabel("Currency:"), gc);
                gc.gridx = 1; gc.gridwidth = 2; gc.fill = GridBagConstraints.HORIZONTAL;
                place.add(currencyBox, gc);

                rbRateBarClickPolicy1=new JRadioButton("Single click fills the rate, double click shows/hides the calculator, right click updates");
                rbRateBarClickPolicy1.setActionCommand("RATE_BAR_CLICK");
                rbRateBarClickPolicy1.addActionListener(actionAdapter);
                rbRateBarClickPolicy2=new JRadioButton("Double click fills the rate, right click shows/hides the calculator, single click updates");
                rbRateBarClickPolicy2.setActionCommand("RATE_BAR_CLICK");
                rbRateBarClickPolicy2.addActionListener(actionAdapter);
                ButtonGroup bg3=new ButtonGroup();
                bg3.add(rbRateBarClickPolicy1);
                bg3.add(rbRateBarClickPolicy2);
                JPanel clicks = new JPanel(new GridLayout(2, 1));
                clicks.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Clicks on the rate bar"));
                clicks.add(rbRateBarClickPolicy1);
                clicks.add(rbRateBarClickPolicy2);

                // One tab for units and the calculator: the unit list on the left, everything else on the right.
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
                rp31.add(Box.createHorizontalGlue());
                comboVisRows.setMaximumSize(comboVisRows.getPreferredSize());
                comboVisDecimals.setMaximumSize(comboVisDecimals.getPreferredSize());
                
                rp3.add(rp31);
                rp3.add(Box.createRigidArea(new Dimension(0,10)));
                rp3.add(Box.createRigidArea(new Dimension(0,5)));
                rp3.setBorder(BorderFactory.createTitledBorder(
                                  BorderFactory.createEtchedBorder(),
                                  "IndianGold Calculator",
                                  TitledBorder.TRAILING,
                                  TitledBorder.DEFAULT_POSITION));
                JPanel options = new JPanel();
                options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));
                for(JComponent part : new JComponent[] { place, p2, clicks, rp3 })
                {
                    part.setAlignmentX(LEFT_ALIGNMENT);
                    part.setMaximumSize(new Dimension(Integer.MAX_VALUE, part.getPreferredSize().height));
                    options.add(part);
                    options.add(Box.createVerticalStrut(6));
                }
                JPanel unitsTab = new JPanel(new BorderLayout(6, 6));
                unitsTab.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                unitsTab.add(leftPane, BorderLayout.CENTER);
                unitsTab.add(options, BorderLayout.EAST);

                JTabbedPane tabs = new JTabbedPane();
                tabs.addTab("Units & Calculator", unitsTab);
                tabs.addTab("Market rates", marketPanel);
                JPanel bottomPane=new JPanel();
                bottomPane.setLayout(new FlowLayout(FlowLayout.TRAILING));
                bottomPane.add(buttonSave);
                bottomPane.add(buttonCancel);
                pane.add(tabs, BorderLayout.CENTER);
                pane.add(bottomPane, BorderLayout.SOUTH);
                
                reload();
                comboVisRows.setSelectedItem(fOps.getValue("$numrows", "10"));
                comboVisDecimals.setSelectedItem(fOps.getValue("$numdecimals", "2"));
		displayNumRows(9); //for the list box in AddRemoveBox
                boolean both=fOps.getValue("$calculator", "1").equals("1") && fOps.getValue("$ratebar", "1").equals("1");
                rbCalculator.setSelected(fOps.getValue("$calculator", "1").equals("1") && !both); //will depend on settings
                rbRateBar.setSelected(fOps.getValue("$ratebar", "1").equals("1") && !both); //will depend on settings
                rbBoth.setSelected(both); //will depend on settings


                setRateBarConfigEnabled(rbRateBar.isSelected() || rbBoth.isSelected());
                setCalculatorConfigEnabled(rbCalculator.isSelected() || rbBoth.isSelected());
                rbRateBarClickPolicy1.setSelected(fOps.getValue("$clickcondition", "1").equals("1"));
                rbRateBarClickPolicy2.setSelected(fOps.getValue("$clickcondition", "1").equals("2"));
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
                rbRateBarClickPolicy1.setEnabled(enabled);
                rbRateBarClickPolicy2.setEnabled(enabled);
        }

        private void selectCurrency(String code)
        {
                for(int i = 0; i < currencyBox.getItemCount(); i++)
                        if(currencyBox.getItemAt(i).currency().getCurrencyCode().equals(code)) currencyBox.setSelectedIndex(i);
        }

        /** Whether manual prices were entered, so they should be read after OK. */
        boolean manualPricesChanged()
        {
                return marketPanel.manualPricesChanged();
        }

        private void setCalculatorConfigEnabled(boolean enabled)
        {
            labelVisRows.setEnabled(enabled);
            comboVisRows.setEnabled(enabled);
            labelVisDecimals.setEnabled(enabled);
            comboVisDecimals.setEnabled(enabled);
        }
        
        /**
         * Units used to show market rates must stay in the calculator, so clicking a rate can fill it in.
         * @param unitName The unit about to be unchecked or removed.
         * @return True if no metal group uses it; otherwise the user is told where to change it.
         */
        private boolean allowedToDrop(String unitName)
        {
                String group = marketSettings.groupUsing(unitName);
                if(group == null) return true;
                JOptionPane.showMessageDialog(thisone, "\"" + unitName + "\" is used to show the " + group + " rates.\nChoose another unit for them with \"Customize rates...\" first.",
                        "Unit in use", JOptionPane.INFORMATION_MESSAGE);
                return false;
        }

        /**
         * Checks or unchecks the unit at the given list index.
         * @param index The list index of the unit.
         */
        private void toggleUnit(int index)
        {
                if(index < 0) return;
                CheckableItem ci = (CheckableItem)unitEditableList.getModel().getElementAt(index);
                if(ci.isSelected() && !allowedToDrop(ci.toString())) return;
                String buff = fOps.getValue(ci.fullName(), "NONE");
                if(!buff.equals("NONE"))
                {
                        fOps.removeValue(ci.fullName()); // full name before click
                        ci.setSelected(!ci.isSelected());
                        unitEditableList.repaint(unitEditableList.getCellBounds(index, index));
                        fOps.setValue(ci.fullName(), buff); // full name after click (adds/removes the _ or *)
                }
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
		propProp=fOps.getAllUnitNames();
		propData=fOps.getAllUnitValues();
		CheckableItem ci[] = new CheckableItem[propProp.size()];
		for(int i=0; i<propProp.size(); i++)
		{
			ci[i] = new CheckableItem(propProp.elementAt(i));
		}
		unitEditableList.setListData(ci);
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
			else if(!fOps.hasUnit(newProp.substring(1))) //Execute if newProp not exists in propProp
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
				if(!allowedToDrop(tmp.toString())) return;
				fOps.removeValue(tmp.fullName());
				reload();
//				tmp=null;
			}
		}
		else if(actionCommand.equals("ALL_SAVE"))
		{
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
                // Only a click on the check box toggles a unit; a click on its name just selects it,
                // and a double click on the name edits it.
                if(CheckboxListRenderer.isOnCheckBox(unitEditableList, me.getPoint()))
                    toggleUnit(unitEditableList.locationToIndex(me.getPoint()));
                else if(me.getClickCount() == 2)
                {
                    int index = unitEditableList.locationToIndex(me.getPoint());
                    Rectangle cell = index < 0 ? null : unitEditableList.getCellBounds(index, index);
                    if(cell != null && cell.contains(me.getPoint())) buttonEdit.doClick();
                }
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
