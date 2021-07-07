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
import java.util.Currency;
import java.util.Locale;

public class CurrencyCode
{
	private final String names[] = {"Albania Lek", "Afghanistan Afghani", "Argentina Peso", "Aruba Guilder", "Australia Dollar", "Azerbaijan New Manat", "Bahamas Dollar", "Barbados Dollar", "Belarus Ruble", "Belize Dollar", "Bermuda Dollar", "Bolivia Boliviano", "Bosnia and Herzegovina Convertible Marka", "Botswana Pula", "Bulgaria Lev", "Brazil Real", "Brunei Darussalam Dollar", "Cambodia Riel", "Canada Dollar", "Cayman Islands Dollar", "Chile Peso", "China Yuan Renminbi", "Colombia Peso", "Costa Rica Colon", "Croatia Kuna", "Cuba Peso", "Czech Republic Koruna", "Denmark Krone", "Dominican Republic Peso", "East Caribbean Dollar", "Egypt Pound", "El Salvador Colon", "Estonia Kroon", "Euro Member Countries", "Falkland Islands (Malvinas) Pound", "Fiji Dollar", "Ghana Cedis", "Gibraltar Pound", "Guatemala Quetzal", "Guernsey Pound", "Guyana Dollar", "Honduras Lempira", "Hong Kong Dollar", "Hungary Forint", "Iceland Krona", "India Rupee", "Indonesia Rupiah", "Iran Rial", "Isle of Man Pound", "Israel Shekel", "Jamaica Dollar", "Japan Yen", "Jersey Pound", "Kazakhstan Tenge", "Korea (North) Won", "Korea (South) Won", "Kyrgyzstan Som", "Laos Kip", "Latvia Lat", "Lebanon Pound", "Liberia Dollar", "Lithuania Litas", "Macedonia Denar", "Malaysia Ringgit", "Mauritius Rupee", "Mexico Peso", "Mongolia Tughrik", "Mozambique Metical", "Namibia Dollar", "Nepal Rupee", "Netherlands Antilles Guilder", "New Zealand Dollar", "Nicaragua Cordoba", "Nigeria Naira", "Korea (North) Won", "Norway Krone", "Oman Rial", "Pakistan Rupee", "Panama Balboa", "Paraguay Guarani", "Peru Nuevo Sol", "Philippines Peso", "Poland Zloty", "Qatar Riyal", "Romania New Leu", "Russia Ruble", "Saint Helena Pound", "Saudi Arabia Riyal", "Serbia Dinar", "Seychelles Rupee", "Singapore Dollar", "Solomon Islands Dollar", "Somalia Shilling", "South Africa Rand", "Korea (South) Won", "Sri Lanka Rupee", "Sweden Krona", "Switzerland Franc", "Suriname Dollar", "Syria Pound", "Taiwan New Dollar", "Thailand Baht", "Trinidad and Tobago Dollar", "Turkey Lira", "Turkey Lira", "Tuvalu Dollar", "Ukraine Hryvna", "United Kingdom Pound", "United States Dollar", "Uruguay Peso", "Uzbekistan Som", "Venezuela Bolivar Fuerte", "Viet Nam Dong", "Yemen Rial", "Zimbabwe Dollar"};
	private final String codeList[] = {"ALL", "AFN", "ARS", "AWG", "AUD", "AZN", "BSD", "BBD", "BYR", "BZD", "BMD", "BOB", "BAM", "BWP", "BGN", "BRL", "BND", "KHR", "CAD", "KYD", "CLP", "CNY", "COP", "CRC", "HRK", "CUP", "CZK", "DKK", "DOP", "XCD", "EGP", "SVC", "EEK", "EUR", "FKP", "FJD", "GHC", "GIP", "GTQ", "GGP", "GYD", "HNL", "HKD", "HUF", "ISK", "INR", "IDR", "IRR", "IMP", "ILS", "JMD", "JPY", "JEP", "KZT", "KPW", "KRW", "KGS", "LAK", "LVL", "LBP", "LRD", "LTL", "MKD", "MYR", "MUR", "MXN", "MNT", "MZN", "NAD", "NPR", "ANG", "NZD", "NIO", "NGN", "KPW", "NOK", "OMR", "PKR", "PAB", "PYG", "PEN", "PHP", "PLN", "QAR", "RON", "RUB", "SHP", "SAR", "RSD", "SCR", "SGD", "SBD", "SOS", "ZAR", "KRW", "LKR", "SEK", "CHF", "SRD", "SYP", "TWD", "THB", "TTD", "TRY", "TRL", "TVD", "UAH", "GBP", "USD", "UYU", "UZS", "VEF", "VND", "YER", "ZWD"};
	String code;
	String name;

	public CurrencyCode(String code)
	{
		this.code = code;
		for(int i=0; i<codeList.length; i++)
		{
			if(codeList[i].equals(code)) this.name = names[i];
		}
	}

	public CurrencyCode()
	{
		Locale locale = Locale.getDefault();
		Currency cur = Currency.getInstance(locale);
		this.code = cur.getCurrencyCode();
		for(int i=0; i<codeList.length; i++)
		{
			if(codeList[i].equals(code)) this.name = names[i];
		}
	}

	public String largestString()
	{
		int len=0;
		String lstr="";
		for(int i=0; i<names.length; i++)
		{
			if(names[i].length()>len)
			{
				len = names[i].length();
				lstr = names[i];
			}
		}
		return lstr;
	}
	
	public void setCode(String code)
	{
		this.code = code;
		for(int i=0; i<codeList.length; i++)
		{
			if(codeList[i].equals(code)) this.name = names[i];
		}
	}

	public String getCode()
	{
		return code;
	}

	public void setName(String name)
	{
		this.name = name;
		for(int i=0; i<names.length; i++)
		{
			if(names[i].equals(name)) this.code = codeList[i];
		}
	}

	public String getName()
	{
		for(int i=0; i<codeList.length; i++)
		{
			if(codeList[i].equals(code)) return names[i];
		}
		return null;
	}

	public String getName(String code)
	{
		for(int i=0; i<codeList.length; i++)
		{
			if(codeList[i].equals(code)) return names[i];
		}
		return null;
	}

	public String[] getCodeList()
	{
		return codeList;
	}

	public String[] getCurrencyList()
	{
		return names;
	}

	public int size()
	{
		return codeList.length;
	}
}