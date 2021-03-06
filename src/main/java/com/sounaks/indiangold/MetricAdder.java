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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.*;

/**
 *
 * @author Sounak Choudhury
 */
class MetricAdder extends JDialog
{
	public static final long serialVersionUID = 4L;
	private final JLabel l1, l2, l3, l4, l5;
	private final JButton addOp, cancelOp;	
	private final NumberField n1, n2;
	private final JTextField t1;
	private final JComboBox j1;
	private final String []newMetric;
        private final MetricActionAdapter mAdapter;
        private final JDialog thisdialog;
		
	MetricAdder(JDialog frame, Vector <String>listContents, String val) // enters edit mode if val = null
	{
		super(frame,"Enter Details...");
                thisdialog = this;
		JPanel pane=(JPanel)super.getContentPane();
                mAdapter = new MetricActionAdapter();
		Vector<String> contents = new Vector<String>(listContents.size());
		for(int i=0; i<listContents.size(); i++)
		{
			String tmp = listContents.elementAt(i);
                        if(tmp.startsWith("_") || tmp.startsWith("*"))
                            contents.addElement(tmp.substring(1));
		}
		l1=new JLabel("Number of Units");
		n1=new NumberField(10, false);
		l2=new JLabel("Unit Name");
		t1=new JTextField(10);
		l3=new JLabel("Amount");
		n2=new NumberField(10, false);
		j1=new JComboBox(contents);
		j1.setPreferredSize(t1.getPreferredSize());
		if(!(val == null || val.equals("")) && contents.contains(val))
		{
			j1.setSelectedItem(val);
			t1.setText(val);
			t1.setEnabled(false);
			n1.setText("1");
			n2.setText("1");
		}
		l4=new JLabel("In");
		JPanel p1=new JPanel(new GridLayout(2,2,5,5));
		t1.setPreferredSize(new Dimension(j1.getWidth(),j1.getHeight()));
		p1.add(l1);
		p1.add(l2);
		p1.add(n1);
		p1.add(t1);
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Unit Details"));
		JPanel p2=new JPanel(new GridLayout(2,2,5,5));
		p2.add(l3);
		p2.add(l4);
		p2.add(n2);
		p2.add(j1);
		p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Unit Value"));
		JPanel flowPane=new JPanel(); //new GridLayout(1,2,5,5));
		l5=new JLabel("=");
		l5.setFont(new Font(l5.getFont().getName(), Font.BOLD, l5.getFont().getSize()+4));
		flowPane.add(p1);
		flowPane.add(l5);
		flowPane.add(p2);

		addOp=new JButton("Add");
		addOp.addActionListener(mAdapter);
		cancelOp=new JButton("Cancel");
		cancelOp.addActionListener(mAdapter);
		JPanel p4=new JPanel();
		p4.add(addOp);
		p4.add(cancelOp);

		pane.add(flowPane,BorderLayout.CENTER);
		pane.add(p4,BorderLayout.SOUTH);
                init();
		newMetric=new String[4];
	}
        
        private void init()
        {
            pack();
            setModal(true);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
	
	public static String[] getNewMetric(JDialog parent, Vector <String>contents, String val)
	{
		MetricAdder adder=new MetricAdder(parent, contents, val);
		Dimension dim = IndianGold.getScreenCenterLocation(adder);
		adder.setLocation(dim.width, dim.height);
		adder.setVisible(true);
                if(adder.newMetric[0] != null) adder.newMetric[0]="*"+adder.newMetric[0]; //adds check mark to the new unit.
		return adder.newMetric;
	}

	private boolean isZero(String txt)
	{
            Double dbl;
            try
            {
                    dbl = Double.parseDouble(txt);
            }
            catch(NumberFormatException ne)
            {
                    return true;
            }
            return dbl==0;
	}

    private class MetricActionAdapter implements ActionListener
    {
        @Override
	public void actionPerformed(ActionEvent ae)
	{
		Object src=ae.getSource();
		if(src.equals(addOp))
		{
			String NAME=t1.getText();
			String VALUE1=n1.getText();
			String VALUE2=n2.getText();
			if(NAME.length()==0 || VALUE1.length()==0 || VALUE2.length()==0)
			{
				JOptionPane.showMessageDialog(thisdialog,"Please fill in the unit details and value fields.","Input Error",JOptionPane.INFORMATION_MESSAGE);
				if(VALUE1.length()==0) n1.requestFocus();
				else if(NAME.length()==0) t1.requestFocus();
				else if(VALUE2.length()==0) n2.requestFocus();
			}
			else if(isZero(VALUE1) || isZero(VALUE2))
			{
				JOptionPane.showMessageDialog(thisdialog,"Values cannot be \'0\' or left blank.","Input Error",JOptionPane.INFORMATION_MESSAGE);
				if(isZero(VALUE1)) n1.requestFocus();
				else if(isZero(VALUE2)) n2.requestFocus();
			}
			else
			{
				newMetric[0]=t1.getText();
				newMetric[1]=n1.getText();
				newMetric[2]=(String)j1.getSelectedItem();
				newMetric[3]=n2.getText();
				dispose();
			}
		}
		else if(src.equals(cancelOp))
		{
			dispose();
		}
	}
    }
    
	/*public static void main(String args[]) //for class standalone testing
	{
		FileOperations flop=new FileOperations(new File("Command.props"),"Co_propo");
		Vector <String>wl=flop.getPropertyNames();
		String ss[]=MetricAdder.getNewMetric(new JDialog(), wl, null);
		for(int i=0;i<ss.length;i++)
		{
			System.out.println(ss[i]);
		}
	}*/
}
