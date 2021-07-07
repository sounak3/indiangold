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

import java.awt.Toolkit;
import java.awt.event.*;
import javax.swing.Timer;

/**
 *
 * @author Sounak Choudhury
 */
public abstract class ClickCountAdapter extends MouseAdapter implements ActionListener
{
    private final static int clickInterval = (Integer)Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");

    MouseEvent lastEvent;
    Timer timer;

    public ClickCountAdapter()
    {
        this(clickInterval);
    }

    public ClickCountAdapter(int delay)
    {
        timer = new Timer( delay, this);
    }

    @Override
    public void mouseClicked (MouseEvent e)
    {
        if (e.getClickCount() > 2) return;

        lastEvent = e;

        if (timer.isRunning())
        {
            timer.stop();
            doubleClick( lastEvent );
        }
        else
        {
            timer.restart();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        timer.stop();
        singleClick( lastEvent );
    }

    public abstract void singleClick(MouseEvent e);
    public abstract void doubleClick(MouseEvent e);

}
