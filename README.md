# indiangold
_____________________________________________________
INTRODUCTION

IndianGold is not just another weight conversion tool. It is focussed at converting some local indian weight units such as ratti, tola, bhori, etc. in addition to standard units. It has a simple convenient user interface which converts to multiple units with press of tab or enter key. All converted units are displayed at once so that user don't have to select a particular output unit; this also helps in comparing units with respect to each other. It supports precision upto 12 decimal places for higher precision. An additional feature constitutes the bottom panel which supports calculation of price of the given weight. The standard market rate can be put to get the price of the required unit. The user interface is intuitive and almost any user can begin using it without help.

_____________________________________________________
END USER LICENSE AGREEMENT (DISCLAIMER)

    IndianGold v3.0 (Weight and price calculation application)
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

_____________________________________________________
REQUIREMENTS

The installers (Windows .msi, Linux .deb and macOS .dmg) bundle their own Java runtime, so nothing else is needed.
To run the plain jar file, Java 21 or newer is required.

Settings, units and the last fetched rates are saved per user in the folder .indiangold in your home folder
(%USERPROFILE%\.indiangold on Windows). A units.dat saved next to the jar by older versions is picked up automatically.

_____________________________________________________
BUILDING FROM SOURCE

1. Install JDK 21 and Maven, and clone this repository (or open it in NetBeans with File -> Open Project).
2. Build with: mvn package
3. Run with: java -jar target/indiangold.jar

How the installers are built and released with Jenkins is described in BUILD.md.
