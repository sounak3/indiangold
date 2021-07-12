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

/**
 *
 * @author Sounak Choudhury
 */
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.plaf.basic.*;

public class CustomComboUI extends BasicComboBoxUI
{	
	public CustomComboUI()
	{
		super();
	}

    @Override
	protected JButton createArrowButton()
	{
            
		return new ArrowButtonImpl();
	}

    @Override
	protected ComboPopup createPopup()
	{
		BasicComboPopup mypopup = new BasicComboPopup(comboBox)
		{
            @Override
			protected Rectangle computePopupBounds(int px,int py,int pw,int ph)
			{
				return super.computePopupBounds(px,py,Math.max(300,pw),ph);
			}
		};
		mypopup.getAccessibleContext().setAccessibleParent(comboBox);
		return mypopup;
	}

    private static class ArrowButtonImpl extends JButton
    {
        public ArrowButtonImpl()
        {
            BasicArrowButton bab=new BasicArrowButton(SwingConstants.SOUTH, super.getBackground(), Color.GRAY, Color.DARK_GRAY, Color.LIGHT_GRAY);
            int width=bab.getPreferredSize().width;
            int height=bab.getPreferredSize().height;
            //System.out.println("Width="+width+", Height="+height);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int size = Math.min((height - 4)/3, (width - 4)/3);
            size = Math.max(size, 2);
            bab.paintTriangle(image.getGraphics(), (width-size)/2, (height-size)/2, size, SwingConstants.SOUTH, true);
            super.setIcon(new ImageIcon(image.getScaledInstance(16, 16, Image.SCALE_AREA_AVERAGING)));
        }

        /*@Override
        public int getWidth()
        {
                return 0;
        }*/
    }
}
