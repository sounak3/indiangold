/*
    NumberField.java : Part of IndianGold weight calculation software application.
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
    public void keyReleased(KeyEvent e)
    {
        char c = e.getKeyChar();
        if((c == KeyEvent.VK_TAB) || (c == KeyEvent.VK_ENTER))
        {
            if(getText().startsWith(".")) setText("0"+getText());
        }
    }
}