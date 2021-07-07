/*
    CheckableItem.java : Part of IndianGold weight calculation software application.
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
public class CheckableItem
{
        private String  str;
        private boolean isSelected;

        public CheckableItem(String name)
        {
                if(name.startsWith("_"))
                {
                        str = name.substring(name.indexOf("_")+1);
                        isSelected = false;
                }
                else if(name.startsWith("*"))
                {
                        str = name.substring(name.indexOf("*")+1);
                        isSelected = true;
                }
        }

        public void setSelected(boolean b) 
        {
                if(b)
                        isSelected = true;
                else
                        isSelected = false;
        }

        public boolean isSelected() 
        {
            return isSelected;
        }

    @Override
        public String toString() //this will anyway return str without _ or *
        {
                if(str.startsWith("_")) return str.substring(str.indexOf("_")+1);
                else if(str.startsWith("*")) return str.substring(str.indexOf("*")+1);
                else return str;
        }

        public String fullName() //this will return actual str
        {
            if(isSelected)
            {
                return "*"+str;
            }
            else
            {
                return "_"+str;
            }
        }
}
