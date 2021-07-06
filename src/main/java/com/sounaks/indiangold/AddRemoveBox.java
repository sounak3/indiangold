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
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 *
 * @author Sounak
 */
public class AddRemoveBox extends JDialog implements ActionListener, MouseListener, KeyListener, WindowListener
{
	public static final long serialVersionUID = 1L;
	private JList jl1;
	private JButton add,edit,remove,save,cancel;
	private JScrollPane jsp;
	FileOperations fOps;
	Vector <String>propData;
	Vector <String>propProp;
	
	AddRemoveBox(JFrame parent, FileOperations file)
	{
		super(parent, "Add / Remove Metric...");
		fOps=file;
		JPanel p1=new JPanel(new BorderLayout());
		JPanel p21=new JPanel();
		JPanel p22=new JPanel();
		JPanel p2=new JPanel(new BorderLayout());
		JPanel pane=(JPanel)getContentPane();
		jsp=new JScrollPane();
		jl1=new JList();
		CheckboxListRenderer clr = new CheckboxListRenderer();
		jl1.setCellRenderer(clr);
		jl1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jl1.addMouseListener(this);
		jl1.addKeyListener(this);
		jsp.setViewportView(jl1);
		p1.add(jsp);
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Unit List"));
		add=new JButton("Add...");
		add.addActionListener(this);
		edit=new JButton("Edit...");
		edit.addActionListener(this);
		remove=new JButton("Remove");
		remove.addActionListener(this);
		save=new JButton("OK");
		save.addActionListener(this);
		cancel=new JButton("Cancel");
		cancel.addActionListener(this);
		p21.add(add);
		p21.add(edit);
		p21.add(remove);
		p22.add(save);
		p22.add(cancel);
		p2.add(p21, BorderLayout.NORTH);
		p2.add(p22, BorderLayout.CENTER);
		pane.add(p1,BorderLayout.CENTER);
		pane.add(p2,BorderLayout.SOUTH);
		reload();
		displayNumRows(8);
		pack();
		Dimension dim = IndianGold.getScreenCenterLocation(this);
		setLocation(dim.width, dim.height);
		setModal(true);
		addWindowListener(this);
	}

	public void displayNumRows(int rows)
	{
		jl1.setVisibleRowCount(rows);
		jsp.setPreferredSize(jl1.getPreferredScrollableViewportSize());
	}
	
	public void addOperation(String newProp, String newVal, String oldProp, String oldVal)
	{
		Double dbl = 0.00, dbl2=0.00, noOfMgs=0.00, dbl3=0.00;
		for(int i=0; i<propProp.size(); i++)
		{
                    String curProp = propProp.elementAt(i).replace("default_", "");
                    if(curProp.equals(oldProp)) //Execute when oldProp equals to propProp element
                    {
                            try
                            {
                                    dbl = Double.parseDouble(propData.elementAt(i)); //oldProp miligram value
                                    dbl2 = Double.parseDouble(oldVal); //no of oldProp units
                                    dbl3 = Double.parseDouble(newVal); //no of new units
                            }
                            catch(NumberFormatException ne)
                            {}
                            noOfMgs = dbl2/dbl; //no of miligrams which is equal to the new unit
                            break;
                    }
		}
		String newData = String.valueOf(dbl3/noOfMgs); // 1mg will have this no. of the new unit
		fOps.setValue(newProp,newData);
		reload();
	}

	public void reload()
	{
		propProp=fOps.getAllPropertyNames();
		propData=fOps.getAllPropertyValues();
		CheckableItem ci[] = new CheckableItem[propProp.size()];
		for(int i=0; i<propProp.size(); i++)
		{
			ci[i] = new CheckableItem(propProp.elementAt(i).toString());
		}
		jl1.setListData(ci);
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		Object obj=ae.getSource();
		boolean editMode = obj.equals(edit);
		if(obj.equals(add))
		{
			String qString[] = MetricAdder.getNewMetric(this, fOps.getAllPropertyNames(), "");
			String newProp = qString[0];
			String newVal = qString[1];
			String oldProp = qString[2];
			String oldVal = qString[3];
                        System.out.println(Arrays.toString(qString));
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
				if(con==JOptionPane.NO_OPTION)
				{
					add.doClick();
				}
				else if(con==JOptionPane.YES_OPTION)
				{
					addOperation(newProp, newVal, oldProp, oldVal);
				}
				else if(con==JOptionPane.CANCEL_OPTION || con==JOptionPane.CLOSED_OPTION)
				{
					// Do nothing.
				}
			}
		}
		else if(obj.equals(edit))
		{
			if(propProp.size() <= 1) JOptionPane.showMessageDialog(this,"Last metric is used as a reference and cannot be edited.","Edit Error",JOptionPane.INFORMATION_MESSAGE);
			else if(jl1.getSelectedValue() == null) JOptionPane.showMessageDialog(this,"Nothing is selected to be edited.","Edit Error",JOptionPane.INFORMATION_MESSAGE);
			else
			{
				CheckableItem ci = ((CheckableItem)jl1.getSelectedValue());
				String qString[] = MetricAdder.getNewMetric(this, fOps.getAllPropertyNames(), ci.toString());
				String newProp = ci.fullName();
				String newVal = qString[1];
				String oldProp = ("default_"+qString[2]).equals(ci.fullName()) ? ci.fullName() : qString[2];
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
		else if(obj.equals(remove))
		{
			if(propProp.size() <= 1) JOptionPane.showMessageDialog(this,"Last metric is used as a reference and cannot be removed.","Remove Error",JOptionPane.INFORMATION_MESSAGE);
			else if(jl1.getSelectedValue() == null) JOptionPane.showMessageDialog(this,"Nothing is selected to be removed.","Remove Error",JOptionPane.INFORMATION_MESSAGE);
			else
			{
				CheckableItem tmp=(CheckableItem)jl1.getSelectedValue();
				fOps.removeValue(tmp.fullName());
				reload();
				tmp=null;
			}
		}
		else if(obj.equals(save))
		{
			fOps.saveToFile();
			reload();
			dispose();
		}
		else if(obj.equals(cancel))
		{
			fOps.discard();
			dispose();
		}
	}

	public void mousePressed(MouseEvent me)
	{
	}

	public void mouseReleased(MouseEvent me)
	{
	}

	public void mouseEntered(MouseEvent me)
	{
	}

	public void mouseExited(MouseEvent me)
	{
	}

	public void mouseClicked(MouseEvent me)
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
			fOps.setValue(ci.fullName(), buff); // full name after click
			//reload();		reload not done as no new item is added/removed and checking is already visible through input events
		}
	}

	public void keyPressed(KeyEvent ke)
	{
	}

	public void keyReleased(KeyEvent ke)
	{
	}

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
					fOps.setValue(ci.fullName(), buff); // full name after click
					//reload();		reload not done as no new item is added/removed and checking is already visible through input events
				}
			}
		}
	}

	public void windowOpened(WindowEvent we)
	{
	}

	public void windowClosed(WindowEvent we)
	{
	}

	public void windowDeiconified(WindowEvent we)
	{
	}

	public void windowIconified(WindowEvent we)
	{
	}

	public void windowClosing(WindowEvent we)
	{
		fOps.discard();
		dispose();
	}

	public void windowActivated(WindowEvent we)
	{
	}

	public void windowDeactivated(WindowEvent we)
	{
	}

	public static void main(String args[])
	{
		FileOperations flop=new FileOperations(new File("Myland.prop"),"Property");
		AddRemoveBox xx = new AddRemoveBox(new JFrame(), flop);
		xx.setVisible(true);
	}
}
