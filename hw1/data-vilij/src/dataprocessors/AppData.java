package dataprocessors;

import javafx.scene.chart.XYChart;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.components.DataComponent;
import vilij.components.Dialog;
import vilij.components.ErrorDialog;
import vilij.propertymanager.PropertyManager;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * This is the concrete application-specific implementation of the data component defined by the Vilij framework.
 *
 * @author Ritwik Banerjee
 * @see DataComponent
 */
public class AppData implements DataComponent {

    private TSDProcessor        processor;
    private ApplicationTemplate applicationTemplate;
    private LinkedList<String> data;

    public AppData(){ }
    public AppData(ApplicationTemplate applicationTemplate) {
        this.processor = new TSDProcessor();
        this.applicationTemplate = applicationTemplate;
    }

    @Override
    public void loadData(Path dataFilePath) {
        data = new LinkedList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(Files.newBufferedReader(dataFilePath));

            String line;
            //text stores first 10 line.
            while ((line = bufferedReader.readLine()) != null)  data.add(line);
            //only display first 10 line, if there is more, show error dialog indicating total line.
            if(data.size()>10){
                ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
                PropertyManager manager  = applicationTemplate.manager;
                String          errTitle = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
                String          errMes   = manager.getPropertyValue(AppPropertyTypes.LOAD_TOO_MANY.name());
                String          errMes2  = manager.getPropertyValue(AppPropertyTypes.SHOW_10_LINE_ONLY.name());
                dialog.show(errTitle, errMes+data.size()+errMes2);
            }
        } catch (IOException ignored) { }
    }

    public Boolean loadData(String dataString) {
        try {
            clear();
            processor.processString(dataString);
        } catch (Exception e) {
            ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
            PropertyManager manager  = applicationTemplate.manager;
            String          errTitle = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_TITLE.name());
            String          errMsg   = manager.getPropertyValue(PropertyTypes.LOAD_ERROR_MSG.name());
            String          errInput = manager.getPropertyValue(AppPropertyTypes.TEXT_AREA.name());
            dialog.show(errTitle, errMsg + errInput+e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void saveData(Path dataFilePath) {
        // NOTE: completing this method was not a part of HW 1. You may have implemented file saving from the
        // confirmation dialog elsewhere in a different way.
        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(dataFilePath))) {
            writer.write(((AppUI) applicationTemplate.getUIComponent()).getCurrentText());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    public void saveDataTest(Path dataFilePath, String s){
        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(dataFilePath))) {
            writer.write(s);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void clear() {
        processor.clear();
    }

    public void displayData() {
        processor.toChartData(((AppUI) applicationTemplate.getUIComponent()).getChart());
    }

    public boolean dataValidity(String dataString) {
        try {
            processor.processString(dataString);
        } catch (Exception e) {
            ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
            PropertyManager manager  = applicationTemplate.manager;
            String          errTitle = manager.getPropertyValue(AppPropertyTypes.FORMAT_ERROR.name());
            dialog.show(errTitle, e.getMessage());
            return false;
        }
        return true;
    }

    public String getDataInfo() {
        return processor.getLoadDataInfo();
    }
    public int getNumberInstance() {
        return processor.getNumberInstance();
    }

    public boolean twoNonNull(){
        clear();
        dataValidity(((AppUI)applicationTemplate.getUIComponent()).getCurrentText());
        return processor.twoNonNullLables();
    }
    public int moreThanOneInstance(){
        dataValidity(((AppUI)applicationTemplate.getUIComponent()).getCurrentText());
        return processor.getNumberInstance();
    }
    public double getxMin(){return processor.getXmin();}
    public double getxMax(){return processor.getXmax();}
    public LinkedList<String> getData() {
        return data;
    }
    public void showTooltip(XYChart<Number, Number> chart){
        processor.showTooltip(chart);
    }
}
