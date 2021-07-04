package com.sounaks.indiangold;

//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.text.*;
//import javax.swing.text.*;
import javax.swing.table.*;

	public class PlainTableModel extends DefaultTableModel
	{
		private Object data[][] = new Object[12][2];
                @Override
		public int getColumnCount()
		{
			return 2;
		}

                @Override
		public int getRowCount()
		{
			return 12;
		}

                @Override
		public void setValueAt(Object value, int row, int col)
		{
			data[row][col] = value;
			fireTableCellUpdated(row, col);
		}

                @Override
		public Object getValueAt(int row, int col)
		{
			return data[row][col];
		}
	}