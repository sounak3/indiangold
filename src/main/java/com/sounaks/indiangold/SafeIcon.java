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

/**
 * Some ui-icons misbehave in that they unconditionally class-cast to the 
 * component type they are mostly painted on. Consequently they blow up if 
 * we are trying to paint them anywhere else (f.i. in a renderer).  
 * 
 * This Icon is an adaption of a cool trick by Darryl Burke/Rob Camick found at
 * http://tips4java.wordpress.com/2008/12/18/icon-table-cell-renderer/#comment-120
 * 
 * The base idea is to instantiate a component of the type expected by the icon, 
 * let it paint into the graphics of a bufferedImage and create an ImageIcon from it.
 * In subsequent calls the ImageIcon is used. 
 * 
 */

package com.sounaks.indiangold;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class SafeIcon implements Icon {

    private Icon wrappee;
    private Icon standIn;

    public SafeIcon(Icon wrappee) {
        this.wrappee = wrappee;
    }

    @Override
    public int getIconHeight() {
        return wrappee.getIconHeight();
    }

    @Override
    public int getIconWidth() {
        return wrappee.getIconWidth();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (standIn == this) {
            paintFallback(c, g, x, y);
        } else if (standIn != null) {
            standIn.paintIcon(c, g, x, y);
        } else {
            try {
               wrappee.paintIcon(c, g, x, y); 
            } catch (ClassCastException e) {
                createStandIn(e, x, y);
                standIn.paintIcon(c, g, x, y);
            }
        }
    }

    /**
     * @param e
     */
    private void createStandIn(ClassCastException e, int x, int y) {
        try {
            Class<?> clazz = getClass(e);
            JComponent standInComponent = getSubstitute(clazz);
            standIn = createImageIcon(standInComponent, x, y);
        } catch (Exception e1) {
            // something went wrong - fallback to this painting
            standIn = this;
        } 
    }

    private Icon createImageIcon(JComponent standInComponent, int x, int y) {
        BufferedImage image = new BufferedImage(getIconWidth(),
                getIconHeight(), BufferedImage.TYPE_INT_ARGB);
          Graphics g = image.createGraphics();
          try {
              wrappee.paintIcon(standInComponent, g, 0, 0);
              return new ImageIcon(image);
          } finally {
              g.dispose();
          }
    }

    /**
     * @param clazz
     * @throws IllegalAccessException 
     */
    private JComponent getSubstitute(Class<?> clazz) throws IllegalAccessException {
        JComponent standInComponent;
        try {
            standInComponent = (JComponent) clazz.newInstance();
        } catch (InstantiationException e) {
            standInComponent = new AbstractButton() {

            };
            ((AbstractButton) standInComponent).setModel(new DefaultButtonModel());
        } 
        return standInComponent;
    }

    private Class<?> getClass(ClassCastException e) throws ClassNotFoundException {
        String className = e.getMessage();
        className = className.substring(className.lastIndexOf(" ") + 1);
        return Class.forName(className);

    }

    private void paintFallback(Component c, Graphics g, int x, int y) {
        g.drawRect(x, y, getIconWidth(), getIconHeight());
        g.drawLine(x, y, x + getIconWidth(), y + getIconHeight());
        g.drawLine(x + getIconWidth(), y, x, y + getIconHeight());
    }

}