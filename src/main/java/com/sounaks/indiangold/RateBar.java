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

import java.awt.*;
import java.awt.event.MouseListener;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.Border;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

/**
 *
 * @author Sounak Choudhury
 */
public class RateBar extends JPanel
{
    JPanel centerP, rightP;
    JLabel top, bottom;
    RateLabel blocks[];
    private Vector<String> metals;
    private Vector<String> rates;
    static final String PRECIOUS_METALS_STRING = "goldsilverplatinumpalladiumrhodium";
    static final String BASE_METALS_STRING = "coppernickelaluminumzincleaduranium";
    private String preciousUnit, baseUnit;
    private final String timePattern1="'As on' EEE, d MMM";
    private final String timePattern2="yyyy HH:mm z";
    SimpleDateFormat sdf;
    java.util.Timer timer;
    TimerTask tt;
    FileOperations fileOps;
    Calendar cal;
    boolean fetchRatesInProgress=false;
    private Border borderType;
    // As kitco.com and kitcometals.com display in ounce and pounds by default
    final double ONE_MG_TO_TROY_OUNCE = 3.215074656862798E-5D;
    final double ONE_MG_TO_POUND = 2.204619999998249E-6D;
    
    RateBar(FileOperations fileOps, Border borderType)
    {
        this.fileOps=fileOps;
        this.borderType=borderType;
        metals = new Vector<String>();
        rates = new Vector<String>();
        centerP=new JPanel(new GridLayout(12,1));
        rightP=new JPanel(new GridLayout(2,1));
        setLayout(new BorderLayout());
        cal = Calendar.getInstance();
        sdf = new SimpleDateFormat(timePattern1);
        String tmpDate=sdf.format(cal.getTime());
        sdf.applyPattern(timePattern2);
        String tmpTime=sdf.format(cal.getTime());
        if(!metals.isEmpty()) blocks = new RateLabel[metals.size()+1];
        else
        {
            blocks = new RateLabel[12];
            for(int i=0; i<11; i++)
            {
                metals.addElement("N/A");
                rates.addElement("N/A");
            }
        }
        
        blocks[0]=new RateLabel(tmpDate, tmpTime);
        centerP.add(blocks[0]);

        for(int i=0; i<metals.size(); i++)
        {
            blocks[i+1]=new RateLabel(metals.elementAt(i), rates.elementAt(i));
            centerP.add(blocks[i+1]);
        }
        
        preciousUnit="USD/oz (troy ounce)";
        baseUnit="USD/lb";
        //top=new JLabel(verticalize(preciousUnit));
        top=new JLabel();
        top.setForeground(Color.red);
        top.setHorizontalAlignment(SwingConstants.CENTER);
        top.setBorder(borderType);
        //bottom=new JLabel(verticalize(baseUnit));
        bottom=new JLabel();
        bottom.setForeground(Color.blue);
        bottom.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.setBorder(borderType);
        rightP.add(top);
        rightP.add(bottom);
        
        add(centerP, BorderLayout.CENTER);
        add(rightP, BorderLayout.EAST);
        
        setSchedule(Integer.valueOf(fileOps.getValue("$rateauto", "0")));
    }
    
    /**
     * Sets the frequency in minutes at which the software fetches market rates for the metals.
     * @param minutes The frequency in minutes after which the software fetches market rates for the metals.
     */
    public final void setSchedule(int minutes)
    {
        if(tt!=null && tt.scheduledExecutionTime()>0)
        {
            //System.out.println("Scheduled time: "+tt.scheduledExecutionTime()+": cancelled");
            timer.cancel();
            timer.purge();
        }
        if(minutes > 0)
        {
            //System.out.println("rescheduling");
            timer = new java.util.Timer();
            tt = new TimerTask()
            {
                @Override
                public synchronized void run()
                {
                    try
                    {
                        while(fetchRatesInProgress)
                        {
                            wait();
                        }
                    }
                    catch(InterruptedException e)
                    {
                        System.out.println("Sorry, I failed to stop fetchRates again while it was fetching.");
                    }
                    fetchRates();
                }
            };
            timer.schedule(tt, 2000, minutes*60*1000); // If 1st run delay is 0 then 1 instance fetchrates occur, timer re-inits and again fetchrates causing ArrayIndexOutOfBoundsException
        }
    }
    
    @Override
    public void addMouseListener(MouseListener ml)
    {
        for (RateLabel block : blocks) {
            block.addMouseListener(ml);
        }
        top.addMouseListener(ml);
        bottom.addMouseListener(ml);
    }
    
    @Override
    public void removeMouseListener(MouseListener ml)
    {
        for (RateLabel block : blocks) {
            block.removeMouseListener(ml);
        }
        top.removeMouseListener(ml);
        bottom.removeMouseListener(ml);
    }
    
    /**
     * This private method parses a string containing an amount with currency symbol and gets only the amount out of it.
     * @param text The amount to be parsed.
     * @return Returns a double containing the amount.
     */
    private Double extractNumberFromCurrency(String text) throws NullPointerException
    {
        if(text==null) throw new NullPointerException("No currency amount specified.");
        boolean decimalFound=false;
        String tmp="";
        char seq[]=text.toCharArray();
        for(int i=seq.length-1; i>=0; i--)
        {
            char c = seq[i];
            if(c=='1' || c=='2' || c=='3' || c=='4' || c=='5' || c=='6' || c=='7' || c=='8' || c=='9' || c=='0')
            {
                tmp = c + tmp;
            }
            if(seq[i]=='.' && !decimalFound)
            {
                tmp = c + tmp;
                decimalFound=true;
            }
        }
        return Double.valueOf(tmp);
    }
    
    /**
     * Gets a HashMap containing names and rates of PRECIOUS_METALS_STRING metals from kitco.com
     * @return Returns a HashMap containing names of PRECIOUS_METALS_STRING metals and their rates from kitco.com
     */
    private HashMap<String, String> getRatesFromKitcoDotCom()
    {
        HashMap<String, String> hm = new HashMap<>();
        String tmpKey="", tmpVal="";
        boolean errorOccured=false;
        try
        {
            Connection.Response response = Jsoup.connect("http://www.kitco.com/market/?sitetype=fullsite")
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                    .timeout(10000).execute();
            int statusCode = response.statusCode();
            
            if(statusCode == 200)
            {
                Document doc = response.parse();
                Element table = doc.select("table").get(32); //select the 32 table for World spot price.
                Elements rows = table.select("tr[class=odd], tr[class=even]");
                for (Element row_precious_metal : rows)
                {
                    tmpKey = row_precious_metal.select("td").get(0).text().trim().toLowerCase();
                    tmpVal = row_precious_metal.select("td").get(3).text().trim();
                    hm.put(tmpKey, tmpVal);
                }
            }
            else
            {
                errorOccured=true;
                System.out.println("received error code : " + statusCode);
            }
        }
        catch (Exception ex)
        {
            errorOccured=true;
            System.out.println("Exception caught!");
            Logger.getLogger(RateBar.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(errorOccured) return new HashMap<String, String>();
        else return hm;
    }
    
    /**
     * Gets a HashMap containing names and rates of BASE_METALS_STRING metals from kitcometals.com
     * @return Returns a HashMap containing BASE_METALS_STRING metals and their rates from kitcometals.com
     */
    private HashMap<String, String> getRatesFromKitcometalsDotCom()
    {
        HashMap<String, String> hm = new HashMap<>();
        String tmpKey="", tmpVal="";
        boolean errorOccured=false;        
        try
        {
            Connection.Response response2 = Jsoup.connect("http://www.kitcometals.com/")
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                    .timeout(10000).execute();
            int statusCode = response2.statusCode();
            //System.out.println(statusCode);
            if(statusCode == 200)
            {
                Document doc = response2.parse();
                Elements element = doc.select("table[dwcopytype=CopyTableRow]");
                int ii = 1;
                boolean lastButNotTheLeast=false;
                String tmp="";
                for (Element urls : element)
                {
                        if(ii%2 == 1)
                        {
                            String tmp2 = urls.text().substring(0,urls.text().indexOf(' ')).trim();
                            if(tmp2.equalsIgnoreCase("uranium"))
                            {
                                lastButNotTheLeast=true;
                                tmp=urls.text().substring(urls.text().indexOf(':')+1).trim();
                            }
                            tmpKey = tmp2.toLowerCase();
                        }
                        else
                        {
                            if(lastButNotTheLeast)
                            {
                                tmpVal = tmp;
                                lastButNotTheLeast=false;
                                tmp="";
                            }
                            else
                            {
                                tmpVal = urls.text().substring(urls.text().indexOf(' '), urls.text().indexOf('-')).trim();
                            }
                        }
                        ii++;
                        hm.put(tmpKey, tmpVal);
                }
            }
            else
            {
                errorOccured=true;
                System.out.println("received error code : " + statusCode);
            }
        }
        catch (Exception ex)
        {
            errorOccured=true;
            System.out.println("Exception caught!");
            Logger.getLogger(RateBar.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(errorOccured) return new HashMap<String, String>();
        else return hm;
    }

    /**
     * This private method fetches the selected currency in the software and current market value of USD. It then returns the conversion factor required to convert from USD to selected currency.
     * @return Returns the conversion factor (USD to selected currency) in double.
     */
    private boolean getUsdToCurrentCurrencyConvFactor()
    {
        double oneUSDvalueOfGivenCurrency;
        String webSrc = "https://www.x-rates.com/calculator/?from=USD&to=" + fileOps.getValue("$currency", "USD") + "&amount=1";
//        String webSrc = "https://www.x-rates.com/calculator/?from=USD&to=INR&amount=1";
        try
        {
            Connection.Response response = Jsoup.connect(webSrc)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                    .timeout(10000).execute();
            int statusCode = response.statusCode();
            //System.out.println(statusCode);
            if(statusCode == 200)
            {
                oneUSDvalueOfGivenCurrency = extractNumberFromCurrency(response.parse().select("span[class=ccOutputRslt]").text());
                fileOps.setValue("$convfactor", String.valueOf(oneUSDvalueOfGivenCurrency));
                return true;
            }
            else
            {
                System.out.println("received error code : " + statusCode);
                return false;
            }
        }
        catch (Exception ex)
        {
            System.out.println("Exception caught!");
            Logger.getLogger(RateBar.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /**
     * This method fetches the metals and rates from the properties file and displays/updates them in the rate bar.
     * @param usdConvFactor This is the conversion factor from USD to the new currency.
     */
    public void updateMetalRates(double usdConvFactor)
    {
        DecimalFormat formatter = new DecimalFormat("###########0.00");
        String t1, t2;
        double punitsPerCurrency = Double.valueOf(fileOps.getValue("$punitspercurrency", "1"));
        double pUnitsInMg = getMilligramValueFor(fileOps.getValue("$punit", ":)"));
        if(pUnitsInMg==0D) pUnitsInMg = this.ONE_MG_TO_TROY_OUNCE;
        double bunitsPerCurrency = Double.valueOf(fileOps.getValue("$bunitspercurrency", "1"));
        double bUnitsInMg = getMilligramValueFor(fileOps.getValue("$bunit", ":)"));
        if(bUnitsInMg==0D) bUnitsInMg = this.ONE_MG_TO_POUND;
        metals.removeAllElements();
        rates.removeAllElements();
        metals.addAll(fileOps.getAllMetalNames());
        rates.addAll(fileOps.getAllMetalRates());
        cal = Calendar.getInstance();
        if(metals.size()>0)
        {
            cal.setTimeInMillis(Long.parseLong(fileOps.getValue("$ratetime", "0")));
            sdf.applyPattern(timePattern1);
            t1=sdf.format(cal.getTime());
            sdf.applyPattern(timePattern2);
            t2=sdf.format(cal.getTime());
            blocks[0].setText(t1, t2);

            int k=11-metals.size();
            for(int i=0; i<metals.size(); i++)
            {
                double rateUSD_PerOzt = Double.valueOf(rates.elementAt(i));
                double convertedRate;
                String tmpName=metals.elementAt(i).substring(1);
                if(PRECIOUS_METALS_STRING.contains(tmpName))
                {
                    convertedRate = usdPerOztOrLbToCurrencyPerUnit(rateUSD_PerOzt, usdConvFactor, punitsPerCurrency, pUnitsInMg, true);
                }
                else
                {
                    convertedRate = usdPerOztOrLbToCurrencyPerUnit(rateUSD_PerOzt, usdConvFactor, bunitsPerCurrency, bUnitsInMg, false);
                }
                if(tmpName.equals("gold")) blocks[1].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("silver")) blocks[2].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("platinum")) blocks[3].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("palladium")) blocks[4].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("rhodium")) blocks[5].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("copper")) blocks[6].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("nickel")) blocks[7].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("aluminum")) blocks[8].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("zinc")) blocks[9].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("lead")) blocks[10].setText(tmpName, formatter.format(convertedRate));
                if(tmpName.equals("uranium")) blocks[11].setText(tmpName, formatter.format(convertedRate));
            }
            if(k!=0)
            {
                for(int i=11;i>metals.size();i--)
                {
                    blocks[i].setText("N/A", "N/A");
                }
            }
        }
        else
        {
            sdf.applyPattern(timePattern1);
            t1=sdf.format(cal.getTime());
            sdf.applyPattern(timePattern2);
            t2=sdf.format(cal.getTime());
            blocks[0].setText(t1, t2);
            for(int i=0; i<11; i++)
            {
                blocks[i+1].setText("N/A", "N/A");
            }
        }
    }
    
    /**
     * This method fetches the rates from the web-sites and displays them in the rate bar.
     */
    public final synchronized void fetchRates()
    {
        //System.out.println("Fetching rates...");
        fetchRatesInProgress=true;
        boolean usdConvFactorSaved = getUsdToCurrentCurrencyConvFactor();
        metals.removeAllElements();
        rates.removeAllElements();
        if(metals.isEmpty())
        {
            for(int i=0; i<11; i++)
            {
                blocks[i+1].setText("Fetching..", "Please wait");
                blocks[i+1].paintImmediately(blocks[i+1].getVisibleRect());
            }
        }
        HashMap<String, String> preciousMap = getRatesFromKitcoDotCom();
        HashMap<String, String> baseMap = getRatesFromKitcometalsDotCom();
        System.out.println(preciousMap);
        saveMetalAndRates(preciousMap, baseMap);
        
        updateMetalRates(usdConvFactorSaved ? Double.valueOf(fileOps.getValue("$convfactor", "1D")) : 1D);
        fetchRatesInProgress=false;
        notifyAll();
//        System.out.println("Done.");
    }
    
    /**
      * This private method saves PRECIOUS_METALS_STRING and BASE_METALS_STRING metal names and rates given as parameters.
      * @param preciousMap A HashMap containing the names and rates of PRECIOUS_METALS_STRING metals.
      * @param baseMap A HashMap containing the names and rates of BASE_METALS_STRING metals.
      */
    private void saveMetalAndRates(HashMap<String, String> preciousMap, HashMap<String, String> baseMap)
    {
        if(preciousMap.size()==5 && baseMap.size()==6)
        {
            Set<String> keyset=preciousMap.keySet();
            Iterator<String> enum1=keyset.iterator();
            for(;enum1.hasNext();)
            {
                String temp=enum1.next();
                fileOps.setValue("@"+temp, preciousMap.get(temp));
            }
            
            keyset = baseMap.keySet();
            enum1 = keyset.iterator();
            for(;enum1.hasNext();)
            {
                String temp=enum1.next();
                fileOps.setValue("@"+temp, baseMap.get(temp));
            }
            cal.setTime(new Date());
            fileOps.setValue("$ratetime", String.valueOf(cal.getTimeInMillis()));
            fileOps.saveToFile();
//            System.out.println("Save metal and rates done...");
        }
    }
    
    /**
     * This private method converts the rate of precious metal from USD per ounce (as depicted in kitco.com) OR from USD per pound (as depicted in kitcometals.com), to the given units per given currency.
     * usdCostPerOztOrLb usdCostPerOzOrLb This is USD per ounce of precious metal OR USD per pound of base metal.
     * @param usdconvfactor This is the conversion factor to convert from USD to the new currency. This is calculated as 1 USD divided by number of units of the new currency per USD.
     * @param unitspercurrency This is number of weight units of the given weight unit, per one unit of the new currency.
     * @param unitmgvalue This is the value in milligrams of the given weight unit.
     * @param pMetal This should be set to true if this conversion is being done for a PRECIOUS_METALS_STRING metal, else false.
     * @return Returns the new rate in weight unit(s) per given currency.
     */
    private double usdPerOztOrLbToCurrencyPerUnit(double usdCostPerOztOrLb, double usdconvfactor, double unitspercurrency, double unitmgvalue, boolean pMetal)
    {
        double costPerMiligram = usdCostPerOztOrLb/(1/(pMetal?this.ONE_MG_TO_TROY_OUNCE:this.ONE_MG_TO_POUND));
        double noOfMiligrams = unitspercurrency/ unitmgvalue;
//        System.out.println(usdCostPerOzOrLb + String.format(", %.10f", costPerMiligram));
        if(new Double(costPerMiligram * noOfMiligrams).equals(Double.NaN))
            return 0.0D;
        else
            return costPerMiligram * noOfMiligrams * usdconvfactor;
    }
    
    /**
     * This private method searches the properties file for the given weight unit and gets the milligram value.
     * @param punit A String representing the weight unit to be searched in the properties file.
     * @return Returns the corresponding milligram value in Double for the given weight unit as parameter.
     */
    private double getMilligramValueFor(String punit)
    {
        String str;
        if(fileOps.getValue("*"+punit, "NONE").equals("NONE") && fileOps.getValue("_"+punit, "NONE").equals("NONE")) return 0D;
        else
        {
            if(fileOps.getValue("*"+punit, "NONE").equals("NONE"))
            {
                str = fileOps.getValue("_"+punit, "NONE");
            }
            else
            {
                str = fileOps.getValue("*"+punit, "NONE");
            }
            try
            {
                return Double.parseDouble(str);
            }
            catch(NumberFormatException ne)
            {
                System.out.println("RateBar.getMilligramValueFor: Can't parse mg value for given unit.");
                return 0D;
            }
        }
    }
    
    @Override
    public void setBorder(Border border)
    {
        if(blocks!=null && border!=null)
        {
            borderType=border;
            for (RateLabel block : blocks) {
                block.setBorder(border);
            }
            top.setBorder(border);
            bottom.setBorder(border);
        }
    }
    
    public void updateToolTips()
    {
        for(int i=1; i<blocks.length; i++)
        {
            if(fileOps.getValue("$clickcondition", "1").equals("1"))
                blocks[i].setToolTipText("<html>Single click to use this rate and<br>Double click to show/hide calculator</html>");
            else
                blocks[i].setToolTipText("<html>Double click to use this rate and<br>Right click to show/hide calculator</html>");
        }
    }
    
    /**
     * This method updates only those rate labels which displays the legend (amount in currency per weight unit) in the rate bar.
     */
    public void updateMetalUnitLabels()
    {
        String tmp=fileOps.getValue("$punitspercurrency", "1");
        String tmp2=fileOps.getValue("$punit", "oz t");
        TextIcon ti;
        RotatedIcon ri;
        if(tmp2.contains("(") && tmp2.contains(")") && tmp2.indexOf("(") < tmp2.indexOf(")"))
        {
            tmp2=tmp2.substring(tmp2.indexOf("(")+1, tmp2.indexOf(")"));
        }
        preciousUnit=fileOps.getValue("$currency", "USD")+" / "+(tmp.equals("1")?"":tmp+" ")+tmp2;
        ti=new TextIcon(top, preciousUnit);
        ri=new RotatedIcon(ti, RotatedIcon.Rotate.DOWN);
        top.setIcon(ri);

        tmp=fileOps.getValue("$bunitspercurrency", "1");
        tmp2=fileOps.getValue("$bunit", "lb");
        if(tmp2.contains("(") && tmp2.contains(")") && tmp2.indexOf("(") < tmp2.indexOf(")"))
        {
            tmp2=tmp2.substring(tmp2.indexOf("(")+1, tmp2.indexOf(")"));
        }
        baseUnit=fileOps.getValue("$currency", "USD")+" / "+(tmp.equals("1")?"":tmp+" ")+tmp2;
        ti=new TextIcon(bottom, baseUnit);
        ri=new RotatedIcon(ti, RotatedIcon.Rotate.DOWN);
        bottom.setIcon(ri);
    }
    
    class RateLabel extends JLabel
    {
        String name;
        String rate;
        final String nameColor1 = "<font color=red>";
        final String nameColor2 = "<font color=blue>";
        final String nameColor3 = "<font color=gray>";
        final String rateColor = "<font color=black>";
        
        RateLabel(String name, String rate)
        {
            this.name = name;
            this.rate = rate;
            setHorizontalAlignment(SwingConstants.CENTER);
            setText("<html><center>"+(PRECIOUS_METALS_STRING.contains(name)?nameColor1:(BASE_METALS_STRING.contains(name)?nameColor2:rateColor))+name.substring(0, 1).toUpperCase()+name.substring(1) +"</font><p>"+((PRECIOUS_METALS_STRING.contains(name)||BASE_METALS_STRING.contains(name))?rateColor:nameColor3)+rate+"</font></center></html>");
            setBorder(borderType);
        }
        
        void setText(String name, String rate)
        {
            this.name = name;
            this.rate = rate;
            setText("<html><center>"+(PRECIOUS_METALS_STRING.contains(name)?nameColor1:(BASE_METALS_STRING.contains(name)?nameColor2:rateColor))+name.substring(0, 1).toUpperCase()+name.substring(1) +"</font><p>"+((PRECIOUS_METALS_STRING.contains(name)||BASE_METALS_STRING.contains(name))?rateColor:nameColor3)+rate+"</font></center></html>");
        }
        
        public String getRate()
        {
            return this.rate;
        }
        
        @Override
        public String getName()
        {
            return this.name;
        }
    }
}
