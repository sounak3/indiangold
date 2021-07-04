package com.sounaks.indiangold;

import javax.swing.*;
import java.awt.event.*;

	public class NumberField extends JTextField implements KeyListener
	{
		public NumberField(int width)
		{
			super(width);
			super.setHorizontalAlignment(RIGHT );
			super.addKeyListener(this);
		}

                @Override
		public void keyTyped(KeyEvent e)
		{
      		char c = e.getKeyChar();				
			String ss= getText();
			if((c == '.') && (ss.length()== 0))
			{
				setText("0");
			}
			else if((c == '.') && (ss.length() > 0))
			{
				for(int i=0; i<ss.length(); i++)
				{
					if(ss.substring(i,i+1).equals("."))
					{
						getToolkit().beep();
						e.consume();
					}
				}
			}
			else if (!((c >= '0') && (c <= '9') || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE) || (c == KeyEvent.VK_ENTER)))
			{
				getToolkit().beep();
				e.consume();
			}
		}

                @Override
		public void keyPressed(KeyEvent e) {}
                @Override
		public void keyReleased(KeyEvent e) {}
	}