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
import java.util.regex.Pattern;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class NumberField extends JTextField
{
    private static final Pattern NUMBER = Pattern.compile("\\d*(\\.\\d*)?");
    private static final Pattern NUMBER_OR_PERCENT = Pattern.compile("\\d*(\\.\\d*)?%?");
    boolean percentAllowed;

    public NumberField(int width, boolean percentAllowed)
    {
        super(width);
        super.setHorizontalAlignment(RIGHT );
        this.percentAllowed = percentAllowed;
        // A document filter sees typing, pasting and setText alike; a key listener missed pasted text.
        ((AbstractDocument)getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException
            {
                replace(fb, offset, 0, text, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
            {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String result = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
                if(isAllowed(result)) fb.replace(offset, length, text, attrs);
                else UIManager.getLookAndFeel().provideErrorFeedback(NumberField.this);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException
            {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                if(isAllowed(current.substring(0, offset) + current.substring(offset + length))) fb.remove(offset, length);
                else UIManager.getLookAndFeel().provideErrorFeedback(NumberField.this);
            }
        });
    }

    private boolean isAllowed(String text)
    {
        return (percentAllowed ? NUMBER_OR_PERCENT : NUMBER).matcher(text).matches();
    }

    /**
     * Private method to parse the number out of the number text fields.
     * @return Parsed double number from the text field; an empty or incomplete entry such as "." counts as 0.
     */
    public double getNumberInput()
    {
        String number = this.getText();
        if(number.endsWith("%")) number = number.substring(0, number.length()-1);
        if(number.isEmpty() || number.equals(".")) return 0.0;
        return Double.parseDouble(number);
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
