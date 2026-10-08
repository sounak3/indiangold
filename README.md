<div align="center" id="top"> 
  <img src="./art/IndianGold-banner.png" alt="IndianGold" width=462 height=200 />

  &#xa0;

<a href="http://www.sounaks.com">Sounak Software</a>
</div>

<h1 align="center">IndianGold</h1>

<p align="center">
  <img alt="Github top language" src="https://img.shields.io/github/languages/top/sounak3/indiangold">

  <img alt="Github language count" src="https://img.shields.io/github/languages/count/sounak3/indiangold">

  <img alt="Repository size" src="https://img.shields.io/github/repo-size/sounak3/indiangold">

  <img alt="License" src="https://img.shields.io/github/license/sounak3/indiangold">

  <img alt="Github issues" src="https://img.shields.io/github/issues/sounak3/indiangold" />

  <img alt="Github forks" src="https://img.shields.io/github/forks/sounak3/indiangold" />

  <img alt="Github stars" src="https://img.shields.io/github/stars/sounak3/indiangold" />
</p>

<hr>

<p align="center">
  <a href="#dart-about">About</a> &#xa0; | &#xa0; 
  <a href="#sparkles-features">Features</a> &#xa0; | &#xa0;
  <a href="#shopping_cart-download">Download</a> &#xa0; | &#xa0;
  <a href="#computer-development">Development</a> &#xa0; | &#xa0;
  <a href="#white_check_mark-requirements">Requirements</a> &#xa0; | &#xa0;
  <a href="#checkered_flag-starting">Starting</a> &#xa0; | &#xa0;
  <a href="#memo-license">License</a> &#xa0; | &#xa0;
  <a href="https://github.com/sounak3" target="_blank">Author</a>
</p>

<br>

## :dart: About ##

IndianGold is a weight and price calculator for gold, silver and other metals, for Windows, Linux and macOS. Besides the standard units it converts the local Indian weight units such as ratti, tola and bhori, and shows the weight in every unit at once with a press of Tab or Enter, so units are easy to compare. A calculator below works out the price of the weight from the market rate, with making charge, discount and taxes. A rate bar shows the current market prices of gold, silver, platinum, palladium and base metals in your currency and units.

## :sparkles: Features ##

:heavy_check_mark: **`All Units at Once:`** Type a weight in any unit and see it in milligrams, grams, kilograms, carats, ratti, tola (standard, 10 g and old British), troy ounces, ounces, pounds and more, with up to 12 decimal places;\
:heavy_check_mark: **`Your Own Units:`** Add, hide or remove units, and choose how many rows and decimal places are shown;\
:heavy_check_mark: **`Price Calculator:`** Get the price of a weight from the rate, with making charge, discount and up to three named taxes;\
:heavy_check_mark: **`Live Market Rates:`** See gold (24K, 22K, 18K and the purities usual in your country), silver, platinum, palladium, copper, aluminium, nickel, zinc, lead and tin in your currency and units; click a rate to use it in the calculator;\
:heavy_check_mark: **`About 250 Countries:`** Choose your country at first start to set the currency, rate units, gold purities and taxes, and change any of them later;\
:heavy_check_mark: **`Customize Rates:`** Choose your own units for each group of metals; their rates are worked out from the market rate;\
:heavy_check_mark: **`Several Rate Sources:`** Gold-API.com and Currency-API (free, no sign-up), Westmetall for LME base metals, and Metals.Dev with your own free key; switch them on or off and set their order;\
:heavy_check_mark: **`Request Limits Respected:`** For sources with a monthly limit, updates are spread over the month at times you choose, and you are told how many are left before updating by hand;\
:heavy_check_mark: **`Manual Rates:`** Type in prices yourself, for example from your jeweller, with their date;\
:heavy_check_mark: **`Plug-ins:`** Add more rate sources as plug-in jars; see [docs/PLUGINS.md](docs/PLUGINS.md);\
:heavy_check_mark: **`Remembers Your Choices:`** Units, settings, the last calculation and the last fetched rates are saved for every user;

## :shopping_cart: Download ##

In case you want to install the latest release, please download the appropriate OS package and install it. The installers include their own Java runtime:

|  OS  | Download file | MD5 hash |
| ---  | ------------- | -------- |
| Windows | [IndianGold-5.0.msi](https://github.com/sounak3/indiangold/releases/latest/download/IndianGold-5.0.msi) | 9fc7cd7e11fe4a24cb4882d3907b204b |
| Ubuntu / Debian | [indiangold_5.0-release_amd64.deb](https://github.com/sounak3/indiangold/releases/latest/download/indiangold_5.0-release_amd64.deb) | 73ae8a3958aa1b08fd5c00d11ed5ad8f |
| Mac OS (Intel; runs on Apple Silicon through Rosetta) | [IndianGold-5.0.dmg](https://github.com/sounak3/indiangold/releases/latest/download/IndianGold-5.0.dmg) | 4c3227d377f05707b45ffc97e40667bb |

In case you're cloning this repository:
```bash
# Clone this project
$ git clone https://github.com/sounak3/indiangold

```

## :computer: Development ##

The following tools were used in this project for development:

<a href="https://git-scm.com/" target="_blank"><img src="https://img.shields.io/badge/GIT-black?style=for-the-badge&logo=GIT&logoColor=F05032"/></a> &nbsp; <a href="https://openjdk.org/" target="_blank"><img src="https://img.shields.io/badge/JAVA-black?style=for-the-badge&logo=openjdk&logoColor=F37626"/></a> &nbsp; <a href="https://maven.apache.org/index.html" target="_blank"><img src="https://img.shields.io/badge/maven-central?style=for-the-badge&logo=apachemaven&logoColor=violet&color=black"/></a> &nbsp; <a href="https://netbeans.apache.org/" target="_blank"><img src="https://img.shields.io/badge/NetBeans-black?style=for-the-badge&logo=apachenetbeanside&logoColor=1B6AC6"/></a> &nbsp; <a href="https://code.visualstudio.com/" target="_blank"><img src="https://img.shields.io/badge/VScode-logo?logo=xing&logoColor=skyblue&labelColor=black&color=black"/></a> &nbsp; <a href="https://www.jenkins.io/" target="_blank"><img src="https://img.shields.io/badge/Jenkins-black?style=for-the-badge&logo=jenkins&logoColor=D24939"/></a>

How the installers are built and tested with Jenkins is described in [BUILD.md](BUILD.md). How to write a rate source plug-in is described in [docs/PLUGINS.md](docs/PLUGINS.md).

## :white_check_mark: Requirements ##

Before starting :checkered_flag: :
- Need to have either of Windows / Linux / MacOS GUI desktop environment.
- An internet connection for market rates (the calculator works without one).

In case you're cloning this repository and running :
- Need to have [Git](https://git-scm.com), [Java SDK 21](https://openjdk.org/install/) or newer, and [Maven](https://maven.apache.org/download.cgi) installed.

## :checkered_flag: Starting ##

In case you've installed from releases:
- Ubuntu  &emsp;: Click Main/Start Menu --> Utility sub-menu --> IndianGold
- Windows &emsp;: Click Start menu --> IndianGold
- Mac OS  &emsp;: Launchpad --> IndianGold

In case you're cloning this repository and running:
```bash
# Go to cloned directory
$ cd indiangold

# Build the project and run its tests
$ mvn clean verify

# Run the project
$ java -jar target/indiangold.jar

```

IndianGold keeps your settings, units and the last fetched rates in `~/.indiangold/` (`%USERPROFILE%\.indiangold\` on Windows), so it can be started from any folder. A `units.dat` saved next to the jar by older versions is picked up automatically.

Market prices are for pure metal; jewellers add premiums, making charges and taxes. Reading web pages automatically may not be allowed by every site's terms, so IndianGold asks before using the Westmetall page and reads it at most once an hour.

## :memo: License ##

IndianGold 5.0 (Weight and price calculation application)\
Copyright (C) 2012-2026 Sounak Choudhury

This project is under the GNU General Public License v3. For more details, see the [LICENSE](LICENSE.md) file.


Made with :heart: by <a href="https://github.com/sounak3" target="_blank">Sounak Choudhury</a>

&#xa0;

<a href="#top">Back to top</a>
