
/*
    IndianGold.java : Part of IndianGold weight calculation software application.
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
import com.sounaks.indiangold.RateBar.RateLabel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * 
 * @author Sounak Choudhury
 */
public class IndianGold extends JFrame
{
    Vector <String>weightList;
    Vector <String>mgValue;
    FileOperations fOps;
    AddRemoveBox box;
    JLabel labelWt, labelRate1, labelRate2, labelWeightUnit, labelMakingChrg, labelDiscount1, labelDiscount2, numTax1fix, numTax2fix, numTax3fix, labelTax1, labelTax2, labelTax3;
    JTextPane costArea;
    NumberField weightField, rateField, noOfUnitsField, makingChargeField, discountField, numTax1Field, numTax2Field, numTax3Field;
    JTextField labelTax1EditField, labelTax2EditField, labelTax3EditField;
    JTable table1;
    JComboBox weightUnitCombo1, weightUnitCombo2, discountOnCombo3;
    DecimalFormat formatter;
    DefaultTableModel model;
    JButton abtButton, setButton, taxButton, rateBarButton, ok1, ok2;
    JScrollPane spane;
    RateBar ratePane;
    Currency currency;
    ShowHideAdapter shAdapter;
    CardAdapter cAdapter;
    NumberFieldFocusAdapter fAdapter;
    PrivateActionAdapter aAdapter;
    private final JPanel mainPane, p12, p32, rp320, rp321, rp322;
    private final CardLayout cards;
    boolean nowcard = true;
    boolean taxBoxActivated = false;
    public static final String NAME_STRING_FULL = "Indian Gold v4.0";
    public static  final String NAME_STRING_MEDIUM = "IndianGold4.0";
    public static  final String NAME_STRING_SHORT = "IGv4";

    /**
     * Internal method to make the sentence to be displayed in the price panel.
     * @return A string to be displayed in the price panel.
     */
    private String getCostString(String cost, String mkCharge, String discount, String gstPercent, String gst, String total)
    {
        String currSymbol = currency.getSymbol();
        String costString = "Total: " + currSymbol + " " + total + ". (Base price: " + currSymbol + " " 
                + cost + ", Making charge: " + currSymbol + " " + mkCharge + ", Discount: " + currSymbol 
                + " " + discount + " and Taxes " + gstPercent + "%: " + currSymbol + " " + gst + ")";
        
        return costString;
    }
    
    private String getCostString()
    {
        return getCostString(localeZero(), localeZero(), localeZero(), localeZero(), localeZero(), localeZero());
    }

    /**
     * Internal method to create a number format string based on the number of decimal places input given. This is used in updating the weight table. 
     * @return A string having the number format either according to the number of decimal places input given OR as the default locale.
     */
    private String getDecimalFormatString()
    {
        int input;
        String format="###########0";
        try
        {
            input=Integer.parseInt((String)fOps.getValue("$numdecimals", "2"));
        }
        catch(NumberFormatException ne)
        {
            input = currency.getDefaultFractionDigits();
        }
        if(input != 0) format=format+".";
        for(int i=0;i<input;i++)
        {
            format=format+"0";
        }
        return format+"  ";
    }

    /**
     * Internal method to parse and get the milligram value of the unit selected in the given combo box.
     * @param source Represents the JComboBox to be parsed.
     * @return The milligram value of the selected unit.
     */
    private double getWeightSelectionMgValue(JComboBox source)
    {
        String selection=(String)source.getSelectedItem();
        for(int i=0; i<weightList.size(); i++)
        {
            if(weightList.elementAt(i).equals(selection))
            {
                try
                {
                    return Double.parseDouble(mgValue.elementAt(i));
                }
                catch(NumberFormatException ne)
                {
                    System.out.println("Error parsing weight value: "+ne.toString());
                    return 0;
                }
            }
        }
        return new Double(0);
    }
	
    /**
     * Internal method which calculates and updates the table in the GUI to show the converted weight values.
     */
    private void calculateWeights()
    {
        formatter = new DecimalFormat(getDecimalFormatString());
        double weightVal;
        model.setRowCount(weightList.size());
        for(int i=0; i<weightList.size(); i++)
        {
            try
            {
                weightVal = Double.parseDouble(mgValue.elementAt(i));
            }
            catch(NumberFormatException ne)
            {
                System.out.println("Error parsing weight value: "+ne.toString());
                weightVal = 0.00D;
            }
            model.setValueAt(formatter.format(weightField.getNumberInput() / getWeightSelectionMgValue(weightUnitCombo1) * weightVal), i, 0);
            model.setValueAt(weightList.elementAt(i), i, 1);
        }
    }

    /**
     * Internal method to calculate and update the price panel with the cost of the given weight, based on the rate input given.
     */
    private void calculateCost()
    {
        String fmtr = "###########0.";
        for(int i=0;i<currency.getDefaultFractionDigits();i++)
        {
            fmtr=fmtr+"0";
        }
        formatter = new DecimalFormat(fmtr);
        double noOfMiligrams = weightField.getNumberInput() / getWeightSelectionMgValue(weightUnitCombo1);

        // Price of one unit = RATE of N units / N units
        double priceOfOneUnit = rateField.getNumberInput() / noOfUnitsField.getNumberInput();
        // Price of one miligram
        double pricePerMiligram = priceOfOneUnit * getWeightSelectionMgValue(weightUnitCombo2);
        // Price
        double price = pricePerMiligram * noOfMiligrams;
        String priceStr = formatter.format(price);

        // Making charge of one unit = Making charge % of Rate of one unit. Or making charge if directly given
        double makingChargeOneUnit = makingChargeField.hasPercentSign() ? makingChargeField.getNumberInput()/100 * priceOfOneUnit : makingChargeField.getNumberInput();
        // Making charge one miligram
        double makingChargePerMiligram = makingChargeOneUnit * getWeightSelectionMgValue(weightUnitCombo2);
        // Making charge
        double makingCharge = makingChargePerMiligram * noOfMiligrams;
        String makingChargeStr = formatter.format(makingCharge);

        // Discount
        double discount = 0.00D;
        switch(discountOnCombo3.getSelectedItem().toString())
        {
            case "Price": // Discount on price
                discount = discountField.hasPercentSign() ? discountField.getNumberInput()/100 * price : discountField.getNumberInput();
                break;
            case "Making": // Discount on making charge
                discount = discountField.hasPercentSign() ? discountField.getNumberInput()/100 * makingCharge : discountField.getNumberInput();
                break;
            case "Total": // Discount on price + making charge
                discount = discountField.hasPercentSign() ? discountField.getNumberInput()/100 * (price + makingCharge) : discountField.getNumberInput();
                break;
        }
        String discountStr = formatter.format(discount);

        // GST
        double gst;
        String allGst[] = fOps.getValue("$taxes", "Tax-1|0.0|Tax-2|0.0|Tax-3|0.0").split("\\|");
        for(int i=0; i<allGst.length; i++)
        {
            if(allGst[i]==null && i%2 != 0) allGst[i] = "0.0";
            if(i%2 != 0 && allGst[i].endsWith("%")) allGst[i] = allGst[i].substring(0, allGst[i].length()-1);
        }
        double gstPercent = Double.parseDouble(allGst[1]) + Double.parseDouble(allGst[3]) + Double.parseDouble(allGst[5]);
        gst = (price + makingCharge - discount) * gstPercent/100;
        String gstStr = formatter.format(gst);
        String strGstPercent = formatter.format(gstPercent);

        // Total
        double total = price + makingCharge - discount + gst;
        String totalStr = formatter.format(total);

        if(new Double(pricePerMiligram * noOfMiligrams).equals(Double.NaN))
            costArea.setText(getCostString());
        else
            costArea.setText(getCostString(priceStr, makingChargeStr, discountStr, strGstPercent, gstStr, totalStr));
    }

    /**
     * Internal method to create a format string to include the number of decimal places according to the default locale.
     * @return A string representing the number format according to default locale.
     */
    private String localeZero()
    {
        String fmtr = "0.";
        for(int i=0;i<currency.getDefaultFractionDigits();i++)
        {
            fmtr=fmtr+"0";
        }
        return fmtr;
    }

    /**
     * Internal method which updates the UI after settings change or initialization.
     */
    private void resetUIData()
    {
        weightList.clear();
        weightList.addAll(fOps.getCheckedUnitNames());
        mgValue.clear();
        mgValue.addAll(fOps.getCheckedUnitValues());
        rateField.setText(rateField.getText().equals("") ? localeZero() : rateField.getText());
        noOfUnitsField.setText(noOfUnitsField.getText().equals("") ? "10" : noOfUnitsField.getText());
        ratePane.removeMouseListener(shAdapter); // removed because it will be re-created if ratebar is already present
        shAdapter=null;                          // and clickcondition settings also may have changed
        if(weightList==null) System.out.println("Got a blank weightlist while resetUIData");
        displayNumRows(Integer.valueOf(fOps.getValue("$numrows", "10")));
        weightUnitCombo1.setSelectedItem(weightList.contains((String)weightUnitCombo1.getSelectedItem()) ? weightUnitCombo1.getSelectedItem() : weightList.isEmpty()?"":weightList.elementAt(0));
        weightUnitCombo2.setSelectedItem(weightList.contains((String)weightUnitCombo2.getSelectedItem()) ? weightUnitCombo2.getSelectedItem() : weightList.isEmpty()?"":weightList.elementAt(0));
        String unit=(String) weightUnitCombo2.getSelectedItem();
        if(unit.contains("(") && unit.contains(")") && unit.indexOf("(") < unit.indexOf(")"))
            labelWeightUnit.setText(unit.substring(unit.indexOf("(")+1, unit.indexOf(")")));
        currency=Currency.getInstance(fOps.getValue("$currency", "USD"));
        costArea.setText(getCostString());
        labelRate1.setText("Rate : "+currency.getSymbol());
        showRateBar(fOps.getValue("$ratebar", "1").equals("1"));
        showMainPane(fOps.getValue("$calculator", "0").equals("1"), false);
        String taxes[] = fOps.getValue("$taxes", "Tax-1|0.0|Tax-2|0.0|Tax-3|0.0").split("\\|");
        labelTax1.setText(taxes[0]);
        numTax1Field.setText(taxes[1]);
        labelTax2.setText(taxes[2]);
        numTax2Field.setText(taxes[3]);
        labelTax3.setText(taxes[4]);
        numTax3Field.setText(taxes[5]);
        cards.show(p32, "MAIN");
    }
        
    public void showMainPane(boolean show, boolean invokedByRateBar)
    {
        if(invokedByRateBar)
        {
            if(show)
                fOps.setValue("$calculator", "1");
            else
                fOps.setValue("$calculator", "0");
            fOps.saveToFile();
        }
        Rectangle rect=getBounds();
        int ii=rect.x+rect.width;
        int jj=rect.y+rect.height;
        mainPane.setVisible(show);
        ratePane.setBorder(fOps.getValue("$calculator", "1").equals("1")?BorderFactory.createEtchedBorder():BorderFactory.createRaisedBevelBorder());
        pack();
        if(show) setTitle(NAME_STRING_FULL);
        else setTitle(NAME_STRING_SHORT);
        if(invokedByRateBar)
        {
            rect=getBounds();
            setLocation(ii-rect.width,jj-rect.height);
        }
    }
    
    public void showRateBar(boolean show)
    {
        if(show)
        {
            shAdapter=new ShowHideAdapter(); // clickcondition settings also may have changed
            ratePane.addMouseListener(shAdapter);
            ratePane.setVisible(true);
            ratePane.updateMetalUnitLabels();
            ratePane.updateMetalRates(Double.valueOf(fOps.getValue("$convfactor", "1D"))); // this will also save the above value since it saves metal rates.
            ratePane.setBorder(fOps.getValue("$calculator", "1").equals("1")?BorderFactory.createEtchedBorder():BorderFactory.createRaisedBevelBorder());
            ratePane.updateToolTips();
            pack(); // this and above code is here as without the ratebar showing, updateMetalRates method is of no use
            if(fOps.getValue("$clickcondition", "1").equals("1"))
            {
                p12.setToolTipText("Click on a rate on the rate list to update here");
                for(Component cmp : p12.getComponents())
                    if(cmp instanceof JComponent) ((JComponent)cmp).setToolTipText("Click on a rate on the rate list to update here");
            }
            else
            {
                p12.setToolTipText("Double-click on a rate on the rate list to update here");
                for(Component cmp : p12.getComponents())
                    if(cmp instanceof JComponent) ((JComponent)cmp).setToolTipText("Double-click on a rate on the rate list to update here");
            }
            fOps.setValue("$ratebar", "1");        }
        else
        {
            ratePane.removeMouseListener(shAdapter); // removed because it will be re-created if ratebar is already present
            shAdapter=null;                          // and clickcondition settings also may have changed
            ratePane.setVisible(false);
            pack();
            p12.setToolTipText(null);
            for(Component cmp : p12.getComponents())
                if(cmp instanceof JComponent) ((JComponent)cmp).setToolTipText(null);
            fOps.setValue("$ratebar", "0");
        }
        fOps.saveToFile();
    }
    
    void settingsProc() {
        box = new AddRemoveBox(this, fOps);
        box.setVisible(true);
        resetUIData();
        ratePane.setSchedule(Integer.valueOf(fOps.getValue("$rateauto", "2")));
    }

    void updateRate(RateLabel label) {
        if(!ratePane.fetchRatesInProgress) {
            if(RateBar.PRECIOUS_METALS_STRING.contains(label.getName()) || RateBar.BASE_METALS_STRING.contains(label.getName()))
                rateField.setText(label.getRate());
            if(RateBar.PRECIOUS_METALS_STRING.contains(label.getName().toLowerCase()))
            {
                noOfUnitsField.setText(fOps.getValue("$punitspercurrency", "1"));
                weightUnitCombo2.setSelectedItem(fOps.getValue("$punit", weightList.elementAt(1)));
            }
            else if(RateBar.BASE_METALS_STRING.contains(label.getName().toLowerCase()))
            {
                noOfUnitsField.setText(fOps.getValue("$bunitspercurrency", "1"));
                weightUnitCombo2.setSelectedItem(fOps.getValue("$bunit", weightList.elementAt(1)));
            }
            String unit=(String) weightUnitCombo2.getSelectedItem();
            if(unit.contains("(") && unit.contains(")") && unit.indexOf("(") < unit.indexOf(")"))
                labelWeightUnit.setText(unit.substring(unit.indexOf("(")+1, unit.indexOf(")")));
        }
    }

    private class PrivateActionAdapter implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent ae)
        {
            Object src = ae.getSource();
            if(src.equals(abtButton))
            {
                String s1 = "<html>Created and Developed by : Sounak Choudhury<p>E-mail Address : <a href='mailto:contact@sounaks.com'>contact@sounaks.com</a><p>The software, information and documentation<p>is provided \"AS IS\" without warranty of any<p>kind, either expressed or implied. The Readme.txt<p>file containing EULA must be read before use.<p>Suggestions and credits are Welcomed.</html>";
                ImageIcon imageicon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("duke.gif"));
                JOptionPane.showMessageDialog(new Frame(), s1, "About IndianGold...", 1, imageicon);
            }
            else if(src.equals(setButton))
            {
                settingsProc();
            }
            else if(src.equals(taxButton))
            {
                nowcard = true;
                cards.show(p32, "TRUE");
            }
            else if(src.equals(rateBarButton))
            {
                showRateBar(!fOps.getValue("$ratebar", "1").equals("1"));
            }
            else if(src.equals(ok1))
            {
                nowcard = false;
                cards.show(p32, "MAIN");
            }
            else if(src.equals(ok2))
            {
                labelTax1.setText(labelTax1EditField.getText());
                labelTax2.setText(labelTax2EditField.getText());
                labelTax3.setText(labelTax3EditField.getText());
                numTax1Field.setText(numTax1fix.getText());
                numTax2Field.setText(numTax2fix.getText());
                numTax3Field.setText(numTax3fix.getText());
                cards.show(p32, "TRUE");
                nowcard = true;
                numTax1Field.requestFocus();
            }
            calculateWeights();
            calculateCost();
        }
    }

    private class NumberFieldFocusAdapter extends FocusAdapter
    {
        @Override
        public void focusLost(FocusEvent fe)
        {
            if(!(fe.getOppositeComponent() instanceof NumberField) && nowcard && taxBoxActivated)
            {
//                System.out.println("Save triggered from focus lost!");
                String taxes = labelTax1.getText() + "|" + numTax1Field.getText() + "|"
                               + labelTax2.getText() + "|" + numTax2Field.getText() + "|"
                               + labelTax3.getText() + "|" + numTax3Field.getText();
                fOps.setValue("$taxes", taxes);
                fOps.saveToFile();
                taxBoxActivated = false;
                cards.show(p32, "MAIN");
            }
            calculateWeights();
            calculateCost();
        }

        @Override
        public void focusGained(FocusEvent fe)
        {
            if(numTax1Field.isFocusOwner()) taxBoxActivated = true;
            else if(numTax2Field.isFocusOwner()) taxBoxActivated = true;
            else if(numTax3Field.isFocusOwner()) taxBoxActivated = true;
            else
            {
                if(nowcard && taxBoxActivated)
                {
//                    System.out.println("Save triggered from focus gained!");
                    String taxes = labelTax1.getText() + "|" + numTax1Field.getText() + "|"
                                   + labelTax2.getText() + "|" + numTax2Field.getText() + "|"
                                   + labelTax3.getText() + "|" + numTax3Field.getText();
                    fOps.setValue("$taxes", taxes);
                    fOps.saveToFile();
                    taxBoxActivated = false;
                    cards.show(p32, "MAIN");
                }
            }

            if(fe.getSource() instanceof NumberField)
            {
                ((NumberField)fe.getSource()).selectAll();
            }
            else if(fe.getSource() instanceof JTextField)
            {
                ((JTextField)fe.getSource()).selectAll();
            }
        }
    }
    
    /**
     * Only default constructor of the class responsible for creation of the main GUI.
     */
    public IndianGold()
    {
        super(NAME_STRING_FULL);
        fOps=new FileOperations(new File("units.dat"),NAME_STRING_MEDIUM);
        weightList=new Vector<String>(); //fOps.getCheckedUnitNames();
        mgValue=new Vector<String>(); //fOps.getCheckedUnitValues();
        shAdapter=new ShowHideAdapter();
        fAdapter = new NumberFieldFocusAdapter();
        aAdapter = new PrivateActionAdapter();
        currency = Currency.getInstance(fOps.getValue("$currency", "USD"));
        JTextField[] fields = new JTextField[11];
        labelWt = new JLabel("Wt.");
        weightField=new NumberField(13, false);
        weightField.addActionListener(aAdapter);
        weightField.addFocusListener(fAdapter);
        weightUnitCombo1=new JComboBox(weightList);
        weightUnitCombo1.addActionListener(aAdapter);
        int requiredTFheight = weightUnitCombo1.getPreferredSize().height;
        fields[0]=weightField;
        weightUnitCombo1.setPreferredSize(new Dimension(weightField.getPreferredSize().width+15, requiredTFheight));

        JPanel p11=new JPanel(new FlowLayout(FlowLayout.LEFT));
        p11.add(labelWt);
        p11.add(weightField);
        p11.add(weightUnitCombo1);
        Icon aboutIcon = getResizedIcon(UIManager.getIcon("OptionPane.informationIcon"),16,16);
        Icon settingsIcon = getResizedIcon(UIManager.getIcon("FileView.computerIcon"),16,16);
        Icon taxIcon = getResizedIcon(UIManager.getIcon("FileView.fileIcon"),16,16);
        Icon rateIcon = getResizedIcon(UIManager.getIcon("FileChooser.detailsViewIcon"),16,16);
        abtButton=new JButton("About", aboutIcon);
        abtButton.setHorizontalTextPosition(SwingConstants.CENTER);
        abtButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        abtButton.addActionListener(aAdapter);
        setButton=new JButton("Settings", settingsIcon);
        setButton.setHorizontalTextPosition(SwingConstants.CENTER);
        setButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        setButton.addActionListener(aAdapter);
        taxButton=new JButton("Taxes", taxIcon);
        taxButton.setHorizontalTextPosition(SwingConstants.CENTER);
        taxButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        taxButton.addActionListener(aAdapter);
        rateBarButton=new JButton("Rate Bar", rateIcon);
        rateBarButton.setHorizontalTextPosition(SwingConstants.CENTER);
        rateBarButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        rateBarButton.addActionListener(aAdapter);
        ok1=new JButton("OK");
        ok1.addActionListener(aAdapter);
        ok2=new JButton("OK");
        ok2.addActionListener(aAdapter);

        labelRate1=new JLabel("Rate : "+currency.getSymbol());
        rateField=new NumberField(7, false);
        fields[1]=rateField;
        rateField.addActionListener(aAdapter);
        rateField.addFocusListener(fAdapter);
        labelRate2=new JLabel("per");
        noOfUnitsField=new NumberField(2, false);
        fields[2]=noOfUnitsField;
        labelWeightUnit=new JLabel();
//                labelWeightUnit.setVisible(false);
        noOfUnitsField.addActionListener(aAdapter);
        noOfUnitsField.addFocusListener(fAdapter);
        weightUnitCombo2=new JComboBox(weightList);
//        weightUnitCombo2.setPreferredSize(goodDimension);
        weightUnitCombo2.addActionListener(aAdapter);
        weightUnitCombo2.setVisible(false);

        p12=new JPanel(new FlowLayout(FlowLayout.LEFT));
        p12.add(labelRate1);
        p12.add(rateField);
        p12.add(labelRate2);
        p12.add(noOfUnitsField);
        p12.add(weightUnitCombo2);
        p12.add(labelWeightUnit);

        labelMakingChrg=new JLabel("Making charge");
        labelDiscount1=new JLabel("Discount");
        labelDiscount2=new JLabel("on");
        String discOn[] = {"Price", "Making", "Total"};
        discountOnCombo3=new JComboBox(discOn);
        discountOnCombo3.addActionListener(aAdapter);
        makingChargeField=new NumberField(4, true);
        fields[3]=makingChargeField;
        makingChargeField.addActionListener(aAdapter);
        makingChargeField.addFocusListener(fAdapter);
        discountField=new NumberField(4, true);
        fields[4]=discountField;
        discountField.addActionListener(aAdapter);
        discountField.addFocusListener(fAdapter);

        JPanel p13=new JPanel();
        p13.add(labelMakingChrg);
        p13.add(makingChargeField);
        p13.add(labelDiscount1);
        p13.add(discountField);
        p13.add(labelDiscount2);
        p13.add(discountOnCombo3);

        costArea=new JTextPane();
        costArea.setPreferredSize(new Dimension(340,60));
        SimpleAttributeSet attribs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attribs , StyleConstants.ALIGN_CENTER);  
        StyleConstants.setBold(attribs , true);
        StyleConstants.setForeground(attribs , Color.red);
        costArea.setParagraphAttributes(attribs,true);  
        costArea.setText(getCostString());
        costArea.setEditable(false);
        costArea.setFocusable(false);
        costArea.setBorder(BorderFactory.createEtchedBorder());
        JPanel p14=new JPanel();
        p14.add(costArea);

        JPanel p1=new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.PAGE_AXIS));
        p1.add(p11);
        p1.add(p12);
        p1.add(p13);
        p1.add(p14);
        p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Calculators"));

        String columnNames[]=new String[]{"Value","Unit"};
        model = new DefaultTableModel(columnNames,weightList.size());
        table1=new JTable(model);
        table1.getColumnModel().getColumn(0).setPreferredWidth(150);
        table1.getColumnModel().getColumn(1).setPreferredWidth(190);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT );
        table1.getColumnModel().getColumn(0).setCellRenderer( rightRenderer );
        table1.setFocusable(false);
        spane =new JScrollPane(table1);
        displayNumRows(Integer.valueOf(fOps.getValue("$numrows", "10")));
        spane.setBorder(BorderFactory.createLoweredBevelBorder());

        JPanel p2=new JPanel();
        p2.add(spane);
        p2.setBorder(BorderFactory.createEtchedBorder());

        rp320=new JPanel();
        rp320.add(taxButton);
        rp320.add(setButton);
        rp320.add(rateBarButton);
        rp320.add(abtButton);

        rp321=new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        rp322=new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        labelTax1 = new JLabel("TAX-1 ", JLabel.RIGHT);
        labelTax1EditField = new JTextField(4);
        fields[5]=labelTax1EditField;
        numTax1Field = new NumberField(3, true);
        fields[6]=numTax1Field;
        numTax1Field.addFocusListener(fAdapter);
        numTax1fix = new JLabel("0", JLabel.CENTER);
        Font labelFont = labelTax1.getFont();
        Font textFieldFont = labelTax1EditField.getFont();
        labelTax1EditField.setFont(labelFont);
        numTax1fix.setFont(textFieldFont);
        labelTax2 = new JLabel("TAX-2 ", JLabel.RIGHT);
        labelTax2EditField = new JTextField(4);
        fields[7]=labelTax2EditField;
        labelTax2EditField.setFont(labelFont);
        numTax2Field = new NumberField(3, true);
        fields[8]=numTax2Field;
        numTax2Field.addFocusListener(fAdapter);
        numTax2fix = new JLabel("0", JLabel.CENTER);
        numTax2fix.setFont(textFieldFont);
        labelTax3 = new JLabel("TAX-3 ", JLabel.RIGHT);
        labelTax3EditField = new JTextField(4);
        fields[9]=labelTax3EditField;
        labelTax3EditField.setFont(labelFont);
        numTax3Field = new NumberField(3, true);
        fields[10]=numTax3Field;
        numTax3Field.addFocusListener(fAdapter);
        numTax3fix = new JLabel("0", JLabel.CENTER);
        numTax3fix.setFont(textFieldFont);
        cards = new CardLayout();
        p32 = new JPanel(cards);
        cAdapter = new CardAdapter();
        rp321.add(labelTax1);
        rp321.add(numTax1Field);
        rp321.add(labelTax2);
        rp321.add(numTax2Field);
        rp321.add(labelTax3);
        rp321.add(numTax3Field);
        rp321.add(ok1);
        // ###
        rp322.add(labelTax1EditField);
        rp322.add(numTax1fix);
        rp322.add(labelTax2EditField);
        rp322.add(numTax2fix);
        rp322.add(labelTax3EditField);
        rp322.add(numTax3fix);
        rp322.add(ok2);
        Component[] array1 = rp321.getComponents();
        Component[] array2 = rp322.getComponents();
        for(int i=0; i<7; i++) // As both the JPanels has 7 components: 3 labels, 3 textfields, 1 button
        {
            if(array1[i] instanceof JLabel)
            {
                ((JLabel)array1[i]).setToolTipText("Click to edit tax name");
                array1[i].addMouseListener(cAdapter);
            }
            else if(array1[i] instanceof NumberField) ((NumberField)array1[i]).setToolTipText("Enter tax rate in percentage");
            if(array2[i] instanceof JLabel)
            {
                ((JLabel)array2[i]).setToolTipText("Click to edit tax");
                array2[i].addMouseListener(cAdapter);
            }
            else if(array2[i] instanceof JTextField) ((JTextField)array2[i]).setToolTipText("Enter tax name");
        }
        p32.add(rp320, "MAIN");
        p32.add(rp321, "TRUE");
        p32.add(rp322, "FALSE");

        JPanel p3=new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.LINE_AXIS));
        p3.add(p32);
        p3.setBorder(BorderFactory.createEtchedBorder());
//        p3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Settings"));

        mainPane = new JPanel();
        mainPane.setLayout(new BorderLayout());
        mainPane.add(p1,BorderLayout.NORTH);
        mainPane.add(p2,BorderLayout.CENTER);
        mainPane.add(p3,BorderLayout.SOUTH);
        for(JTextField component : fields) { // Apply standard height on all textfields
            component.setPreferredSize(new Dimension(component.getPreferredSize().width, requiredTFheight));
        }
        ratePane = new RateBar(fOps, (fOps.getValue("$calculator", "1").equals("1")?BorderFactory.createEtchedBorder():BorderFactory.createRaisedBevelBorder()));

        init();
        resetUIData();
    }
    
    /**
     * Created separate init to avoid overridable methods setLayout, add & setIconImage in the constructor
     */
    private void init()
    {
        setLayout(new BorderLayout());
        add(mainPane, BorderLayout.CENTER);
        add(ratePane, BorderLayout.EAST);

        try
        {
            java.net.URL url1 = Thread.currentThread().getContextClassLoader().getResource("igcircle.gif");
            Image icon = ImageIO.read(url1);
            setIconImage(icon);
        }
        catch(IOException e)
        {
            System.out.println("Icon not found.");
        }
    }
    
    /**
     * Internal method for controlling number of rows to display in the conversion table.
     * @param num Specifies the number of rows to display.
     */
    private void displayNumRows(int num)
    {
        int hh=table1.getRowMargin()+table1.getRowHeight(0);
        spane.setPreferredSize(new Dimension(340,num*hh+hh));
    }

    /**
     * Resizes the given icon according to given parameters.
     * @param icon Icon to resize.
     * @param width Width of the resized icon.
     * @param height Height of the resized icon.
     * @return Returns the icon after resizing.
     */
    public final Icon getResizedIcon(Icon icon, int width, int height)
    {
        SafeIcon ico = new SafeIcon(icon);
        BufferedImage image = new BufferedImage(ico.getIconWidth(), ico.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        ico.paintIcon(new JButton(), image.getGraphics(), 0, 0);
        Image tmpImg = image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        return new ImageIcon(tmpImg);
    }

    /**
     * Gets the screen center location for the given container.
     * @param cont container to be posited at screen center.
     * @return Returns a perfect screen center location for the given container.
     */
    public static Dimension getScreenCenterLocation(Container cont)
    {
        Dimension window = cont.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int X = (screen.width / 2) - (window.width / 2); // Center horizontally.
        int Y = (screen.height / 2) - (window.height / 2); // Center vertically.
        return new Dimension(X,Y);
    }

    private class CardAdapter extends MouseAdapter
    {    
        @Override
        public void mouseClicked(MouseEvent me) {
            nowcard = !nowcard;
            if(nowcard)
            {
                labelTax1.setText(labelTax1EditField.getText());
                labelTax2.setText(labelTax2EditField.getText());
                labelTax3.setText(labelTax3EditField.getText());
                numTax1Field.setText(numTax1fix.getText());
                numTax2Field.setText(numTax2fix.getText());
                numTax3Field.setText(numTax3fix.getText());
            }
            else
            {
                labelTax1EditField.setText(labelTax1.getText());
                labelTax2EditField.setText(labelTax2.getText());
                labelTax3EditField.setText(labelTax3.getText());
                numTax1fix.setText(numTax1Field.getText());
                numTax2fix.setText(numTax2Field.getText());
                numTax3fix.setText(numTax3Field.getText());
            }
            cards.show(p32, Boolean.toString(nowcard).toUpperCase());
        }
    }
    
    private class ShowHideAdapter extends ClickCountAdapter
    {
        boolean doubleClickForShowHide, manualRefreshRates;

        public ShowHideAdapter() 
        {
            doubleClickForShowHide=fOps.getValue("$clickcondition", "1").equals("1");
            // true = Single click to fill the rate. Double click for show/hide calculator.
            // false = Double click to fill the rate. Right click for show/hide calculator.
            
            manualRefreshRates=fOps.getValue("$rateauto", "0").equals("0");
            // true = Manual refresh enabled.
        }

        @Override
        public void singleClick(MouseEvent e)
        {
            if(SwingUtilities.isRightMouseButton(e))
            {
                if(doubleClickForShowHide) // right single click for clickCondition=1, refresh the rate
                {
                    if(manualRefreshRates) ratePane.fetchRates();
                }
                else // right single click for clickCondition=2, show/hide calculator
                {
                    showMainPane(!fOps.getValue("$calculator", "0").equals("1"), true);
                }
            }
            else
            {
                if(doubleClickForShowHide) // left single click for clickCondition=1, fill the rate
                {
                    if(e.getSource() instanceof RateLabel) updateRate((RateLabel)e.getSource());
                    calculateWeights();
                    calculateCost();
                }
                else // left single click for clickCondition=2, refresh the rate
                {
                    if(manualRefreshRates) ratePane.fetchRates();
                }
            }
        }

        @Override
        public void doubleClick(MouseEvent e)
        {
            if(SwingUtilities.isRightMouseButton(e))
            {
                // Right double click has no functionality
            }
            else
            {
                if(doubleClickForShowHide) // left double click for clickCondition=1, show/hide calculator
                {
                    showMainPane(!fOps.getValue("$calculator", "0").equals("1"), true);
                }
                else // left double click for clickCondition=2, fill the rate
                {
                    if(e.getSource() instanceof RateLabel) updateRate((RateLabel)e.getSource());
                    calculateWeights();
                    calculateCost();
                }
            }
        }
        
        @Override
        public void mouseEntered(MouseEvent e)
        {
            if(e.getSource() instanceof RateLabel)
                ((RateLabel)e.getSource()).glow();
        }
        
        @Override
        public void mouseExited(MouseEvent e)
        {
            if(e.getSource() instanceof RateLabel)
                ((RateLabel)e.getSource()).dim();
        }
    }

    /**
     * Main method. Main event dispatch thread.
     * @param args No parameters.
     */
    public static void main(String args[])
    {
        IndianGold mm=new IndianGold();
        mm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mm.pack();
        Dimension loc=getScreenCenterLocation(mm);
        mm.setLocation(loc.width,loc.height);
        mm.setResizable(false);
        mm.setVisible(true);
    }
}