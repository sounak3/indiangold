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

/**
 *
 * @author Sounak Choudhury
 */
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.JTextField;

public class NumberField extends JTextField
{
    boolean percentAllowed;
    public NumberField(int width, boolean percentAllowed)
    {
        super(width);
        super.setHorizontalAlignment(RIGHT );
        super.addKeyListener(new KeyAdapter() {
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
                    if(ss.contains(".") || (ss.contains("%") && percentAllowed))
                    {
                        getToolkit().beep();
                        e.consume();
                    }
                }
                else if((c == '%') && percentAllowed && (ss.length() > 0))
                {
                    if(ss.contains("%"))
                    {
                        getToolkit().beep();
                        e.consume();
                    }
                }
                else if (!((c >= '0') && (c <= '9') 
                        || (c == KeyEvent.VK_BACK_SPACE) 
                        || (c == KeyEvent.VK_DELETE) 
                        || (c == KeyEvent.VK_ENTER))
                        || ss.endsWith("%"))
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
            
        });
        this.percentAllowed = percentAllowed;
    }

    /**
     * Private method to parse the number out of the number text fields. 
     * @return Parsed double number from the text field.
     */
    public double getNumberInput() throws NumberFormatException
    {
        double input;
        String fullText = this.getText();
        if(fullText == null || fullText.equals(""))
            fullText = "0.0";
        if(fullText.contains("%") && !fullText.endsWith("%"))
            throw new NumberFormatException("Percent sign in middle of number.");
        else if(fullText.endsWith("%"))
            input=Double.parseDouble(fullText.substring(0, fullText.length()-1));
        else
            input=Double.parseDouble(fullText);
        return input;
    }

    /**
     * Private method to check the percent sign in the number text fields. 
     * @return True if the text field has a percent sign, else will return false.
     */
    public boolean hasPercentSign()
    {
        return this.getText().contains("%");
    }
}
