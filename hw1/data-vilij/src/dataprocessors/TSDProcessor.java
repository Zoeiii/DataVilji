package dataprocessors;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import settings.AppPropertyTypes;
import vilij.templates.ApplicationTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * The data files used by this data visualization applications follow a tab-separated format, where each data point is
 * named, labeled, and has a specific location in the 2-dimensional X-Y plane. This class handles the parsing and
 * processing of such data. It also handles exporting the data to a 2-D plot.
 * <p>
 * A sample file in this format has been provided in the application's <code>resources/data</code> folder.
 *
 * @author Ritwik Banerjee
 * @see XYChart
 */
public final class TSDProcessor {

    public static class InvalidDataNameException extends Exception {

        private static final String NAME_ERROR_MSG = "All data instance names must start with the @ character.";

        public InvalidDataNameException(String name) {
            super(NAME_ERROR_MSG+String.format("%nInvalid name '%s'. At line: ",name));
        }
    }
    private class DuplicateDataNameException extends Throwable {
        public DuplicateDataNameException(String name) {
            super(String.format("%nDuplicate name '%s'. At line: ", name));
        }
    }

    private Map<String, String>  dataLabels;

    public Map<String, Point2D> getDataPoints() {
        return dataPoints;
    }

    private Map<String, Point2D> dataPoints;
    private Map<Point2D, String> dataPoints1;
    private ArrayList<String> nameArray;
    private ApplicationTemplate applicationTemplate;
    private Double Xmin, Xmax;

    public TSDProcessor() {
        dataLabels = new HashMap<>();
        dataPoints = new HashMap<>();
        dataPoints1 = new HashMap<>();
        applicationTemplate = new ApplicationTemplate();
    }

    /**
     * getLoad data information while processing the string
     * @return information of data as String
     */
    public String getLoadDataInfo(){
        String dash = "-";
        String newLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NEW_LINE.name());
        Set<String> labels = new HashSet<>(dataLabels.values());
        StringBuilder info = new StringBuilder();

        info.append(newLine).append(dataPoints.size()).append(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NUMBER_INSTANCE.name())).append(labels.size())
                .append(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NUMBER_LABLES.name()))
                .append(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.ListOfLable.name()));
        for (String n :labels) {
            if(!n.equalsIgnoreCase("null"))
            info.append(newLine).append(dash).append(n);
        }
        return info.toString();
    }

    public int getNumberInstance(){
        return dataPoints.size();
    }
    /**
     * check if there is exactly two non null labels
     * @return boolean, true exactly 2 non null, otherwise false
     */
    public boolean twoNonNullLables(){
        Set<String> labels = new HashSet<>(dataLabels.values());
        int nonNull=0;
        for (String n : labels)
            if(!n.equalsIgnoreCase("null"))
                nonNull++;
        return nonNull == 2;
    }

    /**
     * Processes the data and populated two {@link Map} objects with the data.
     *
     * @param tsdString the input data provided as a single {@link String}
     * @throws Exception if the input string does not follow the <code>.tsd</code> data format
     */
    public void processString(String tsdString) throws Exception {
        nameArray = new ArrayList<>();
        AtomicInteger i = new AtomicInteger(0);
        ArrayList<String> err = new ArrayList<>(), err2 = new ArrayList<>();
        AtomicBoolean hadAnError   = new AtomicBoolean(false);
        StringBuilder errorMessage = new StringBuilder(), errorMessage2 = new StringBuilder();

        Stream.of(tsdString.split("\n"))
              .map(line -> Arrays.asList(line.split("\t")))
              .forEach(list -> {
                  try {
                      i.getAndIncrement();
                      String   name  = checkedname(list.get(0));
                      String   label = list.get(1);
                      String[] pair  = list.get(2).split(",");
                      Point2D  point = new Point2D(Double.parseDouble(pair[0]), Double.parseDouble(pair[1]));
                      dataLabels.put(name, label);
                      dataPoints.put(name, point);
                      dataPoints1.put(point,name);
                  } catch (Exception e) {
                      err.add(i.toString()); //invalid name
                      errorMessage.setLength(0);
                      errorMessage.append(e.getMessage());
                      hadAnError.set(true);
                  } catch (DuplicateDataNameException e) {
                      errorMessage2.setLength(0);
                      errorMessage2.append(e.getMessage());
                      err2.add(i.toString()); //same name
                  }
              });
        if (err.size() > 0 && err2.size() > 0) {
            if(Integer.parseInt(err.get(0)) < Integer.parseInt(err2.get(0))) //if same name appear before invalid name
                throw new Exception(errorMessage + err.get(0));
            else
                throw new Exception(errorMessage2+err2.get(0));

        }
        else if(err.size() > 0){
            throw new Exception(errorMessage + err.get(0));
        }
        else if(err2.size() > 0)
            throw new Exception(errorMessage2 + err2.get(0));

    }

    /**
     * Exports the data to the specified 2-D chart.
     *
     * @param chart the specified chart
     */
    void toChartData(XYChart<Number, Number> chart) {
        Set<String> labels = new HashSet<>(dataLabels.values());

        for (String label : labels) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(label);
            dataLabels.entrySet().stream().filter(entry -> entry.getValue().equals(label)).forEach(entry -> {
                Point2D point = dataPoints.get(entry.getKey());
                series.getData().add(new XYChart.Data<>(point.getX(), point.getY()));
            });
            chart.getData().add(series);
        }
        showTooltip(chart);
    }

    private void setxMaxMin(){
        Set<String> labels = new HashSet<>(dataLabels.values());
        ArrayList<Double> x = new ArrayList<>();
        for (String label : labels) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(label);
            dataLabels.entrySet().stream().filter(entry -> entry.getValue().equals(label)).forEach(entry -> {
                Point2D point = dataPoints.get(entry.getKey());
                x.add(point.getX());
            });
        }
        if(x.stream().mapToDouble(Double::doubleValue).average().isPresent()) {
            Xmin = x.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
            Xmax = x.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
        }
    }

    public Double getXmax() {
        setxMaxMin();
        return Xmax;
    }

    public Double getXmin() {
        setxMaxMin();
        return Xmin;
    }

    void clear() {
        dataPoints1.clear();
        dataPoints.clear();
        dataLabels.clear();
    }

    /**
     * Check if the name is valid
     * @param name  data name start with@
     * @return  name
     * @throws InvalidDataNameException
     *          throw exception when the data is not valid, not start with @
     * @throws DuplicateDataNameException
     *          throw exception when there is duplicated name
     */
    private String checkedname(String name) throws InvalidDataNameException, DuplicateDataNameException {
        if (!name.startsWith("@"))
            throw new InvalidDataNameException(name);
        for(String n: nameArray){
            if(n.equals(name))
                throw new DuplicateDataNameException(name);
        }
        nameArray.add(name);
        return name;
    }

    /**
     * Add tooltips to points
     * @param chart the chart that's displaying
     */
    public void showTooltip(XYChart<Number, Number> chart) {
        Point2D point;
        String x = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.X.name()),
                y = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.Y.name()),
                newLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NEW_LINE.name());
        for (XYChart.Series<Number, Number> s : chart.getData()) {
            for (XYChart.Data<Number, Number> d : s.getData()) {
                point = new Point2D(d.getXValue().doubleValue(), d.getYValue().doubleValue());
                String name = dataPoints1.get(point);
                Tooltip.install(d.getNode(), new Tooltip(name + newLine + x + d.getXValue() + newLine+y + d.getYValue()));
                d.getNode().setCursor(Cursor.HAND);
            }
        }
    }



}
