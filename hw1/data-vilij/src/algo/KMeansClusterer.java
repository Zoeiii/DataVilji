package algo;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.chart.XYChart;
import ui.AppUI;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class KMeansClusterer extends Clusterer{

    private DataSet       dataset;
    private List<Point2D> centroids;
    private AppUI appUI;

    private final int           maxIterations;
    private final int           updateInterval;
    private final int           numberOfClusters;
    private final AtomicBoolean tocontinue;
    private AtomicBoolean       toBeContinue = new AtomicBoolean();


    public KMeansClusterer(DataSet dataset,int[] arug, boolean toBbContinue,AppUI appUI) {
        super(arug[2]);
        this.dataset = dataset;
        this.maxIterations = arug[0];
        this.updateInterval = arug[1];
        this.numberOfClusters = getNumberOfClusters();
        this.toBeContinue.set(toBbContinue);
        this.appUI = appUI;
        this.tocontinue = new AtomicBoolean(false);
    }

    @Override
    public int getMaxIterations() { return maxIterations; }

    @Override
    public int getUpdateInterval() { return updateInterval; }

    @Override
    public boolean tocontinue() { return toBeContinue.get(); }

    @Override
    public void run() {
        appUI.getStartOverButton().setVisible(false);
        initializeCentroids();
        int iteration = 0;
        while (iteration++ < maxIterations & tocontinue.get()) {
            assignLabels();
            recomputeCentroids();
            appUI.getRunButton().setDisable(true);
            appUI.getScrnshotButton().setDisable(true);
            appUI.getConfu().setDisable(true);
            appUI.getConfu2().setDisable(true);
            try {
                toChartData(dataset);
            } catch (InterruptedException e) {
                synchronized (this){this.notify();}
            }
        }
        appUI.getScrnshotButton().setDisable(false);
        appUI.getRunButton().setDisable(true);
        appUI.getConfu().setDisable(false);
        appUI.getConfu2().setDisable(false);
        appUI.getStartOverButton().setVisible(true);
    }
    private void initializeCentroids() {
        Set<String>  chosen        = new HashSet<>();
        List<String> instanceNames = new ArrayList<>(dataset.getLabels().keySet());
        Random       r             = new Random();
        //create labels by random number according to the # of labels needed
        while (chosen.size() < numberOfClusters) {
            //produce random integer with a range of size of instanceNames
            int i = r.nextInt(instanceNames.size());
            while (chosen.contains(instanceNames.get(i)))
                i = (++i % instanceNames.size());
            chosen.add(instanceNames.get(i));
        }
        centroids = chosen.stream().map(name -> dataset.getLocations().get(name)).collect(Collectors.toList());
        tocontinue.set(true);
    }
    private void assignLabels() {
        dataset.getLocations().forEach((instanceName, location) -> {
            double minDistance      = Double.MAX_VALUE;
            int    minDistanceIndex = -1;
            for (int i = 0; i < centroids.size(); i++) {
                double distance = computeDistance(centroids.get(i), location);
                if (distance < minDistance) {
                    minDistance = distance;
                    minDistanceIndex = i;
                }
            }
            dataset.getLabels().put(instanceName, Integer.toString(minDistanceIndex));
        });
    }

    private void recomputeCentroids() {
        tocontinue.set(false);
        IntStream.range(0, numberOfClusters).forEach(i -> {
            AtomicInteger clusterSize = new AtomicInteger();
            Point2D sum = dataset.getLabels()
                    .entrySet()
                    .stream()
                    .filter(entry -> i == Integer.parseInt(entry.getValue()))
                    .map(entry -> dataset.getLocations().get(entry.getKey()))
                    .reduce(new Point2D(0, 0), (p, q) -> {
                        clusterSize.incrementAndGet();
                        return new Point2D(p.getX() + q.getX(), p.getY() + q.getY());
                    });
            Point2D newCentroid = new Point2D(sum.getX() / clusterSize.get(), sum.getY() / clusterSize.get());
            if (!newCentroid.equals(centroids.get(i))) {
                centroids.set(i, newCentroid);
                tocontinue.set(true);
            }
        });
    }

    private static double computeDistance(Point2D p, Point2D q) {
        return Math.sqrt(Math.pow(p.getX() - q.getX(), 2) + Math.pow(p.getY() - q.getY(), 2));
    }

    public void toChartData(DataSet dataset) throws InterruptedException {
        Platform.runLater(()-> {
            appUI.getChart().getData().clear();
            appUI.getChart().getData().add(new XYChart.Series<>());//place holder for the line
        });
        Set<String> labels = new HashSet<>(dataset.getLabels().values()); //should be 0/1/2/3 ect
        for (String label : labels) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(label);
            //if dataset.labels.getKey() or name equals to location.name, added to series
            dataset.getLocations().forEach((name, location) ->
                    //get all the data name with specific label
                    dataset.getLabels().entrySet().stream().filter(entry ->entry.getValue().equals(label)).forEach(entry->{
                        if(name.equals(entry.getKey()))
                            series.getData().add(new XYChart.Data<>(location.getX(), location.getY()));
                    })
            );
            Platform.runLater(()->appUI.getChart().getData().add(series));
        }
        Thread.sleep(500);
        if(!toBeContinue.get()){
            appUI.getScrnshotButton().setDisable(false);
            appUI.getRunButton().setDisable(false);
            synchronized (this){this.wait();}
        }
    }
}