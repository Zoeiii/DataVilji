package ui;

import actions.AppActions;
import algo.Algorithm;
import algo.DataSet;
import dataprocessors.AppData;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import settings.AppPropertyTypes;
import vilij.components.Dialog;
import vilij.components.ErrorDialog;
import vilij.propertymanager.PropertyManager;
import vilij.templates.ApplicationTemplate;
import vilij.templates.UITemplate;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static vilij.settings.PropertyTypes.GUI_RESOURCE_PATH;
import static vilij.settings.PropertyTypes.ICONS_RESOURCE_PATH;

/**
 * This is the application's user interface implementation.
 *
 * @author Ritwik Banerjee
 */
public final class AppUI extends UITemplate {

    /** The application to which this class of actions belongs. */
    ApplicationTemplate applicationTemplate;

    private Button                       scrnshotButton; // toolbar button to take a screenshot of the data
    private LineChart<Number, Number>    chart;          // the chart where data will be displayed
    private Button                       displayButton; // workspace button to display data on the chart
    private Button                       clustering, classfication, runButton,confu,confu2,startOverButton;
    private VBox                         algoTypes;
    private TextArea                     textArea,       // text area for new data input
                                        iter, updateInter,numberlables;
    private boolean                      hasNewText;    // whether or not the text area has any new data since last display
    private Label                        dataInfo;       //lable to display data information when loaded.
    private CheckBox                     checkBox,con;       // enable and disable textarea read only mode
    private VBox                         leftPanel,algoVbox;
    private RadioButton                  algoSets, algoSet2;
    private ToggleGroup                  group;
    private int[]                        classInput = new int[2],clusterInput = new int[3];
    private HashMap<int[],Boolean>       pairMap; //store the value of configuration (the user input).
    private Thread                       currentAlgo; //last activated algo
    private boolean                      classFlag, clusterFlag, RClusterFlag, kClusterFlag; //used to indicate algo type seclected.
    private Class<?>                     algoClass; //where we don't know which algo is being called at compile time
    private Algorithm                    algo;


    public AppUI(){}
    public AppUI(Stage primaryStage, ApplicationTemplate applicationTemplate) {
        super(primaryStage, applicationTemplate);
        this.applicationTemplate = applicationTemplate;
    }

    @Override
    protected void setResourcePaths(ApplicationTemplate applicationTemplate) {
        super.setResourcePaths(applicationTemplate);
    }

    @Override
    protected void setToolBar(ApplicationTemplate applicationTemplate) {
        super.setToolBar(applicationTemplate);
        PropertyManager manager = applicationTemplate.manager;
        String iconsPath = SEPARATOR + String.join(SEPARATOR,
                                                   manager.getPropertyValue(GUI_RESOURCE_PATH.name()),
                                                   manager.getPropertyValue(ICONS_RESOURCE_PATH.name()));
        String scrnshoticonPath = String.join(SEPARATOR,
                                              iconsPath,
                                              manager.getPropertyValue(AppPropertyTypes.SCREENSHOT_ICON.name()));
        scrnshotButton = setToolbarButton(scrnshoticonPath,
                                          manager.getPropertyValue(AppPropertyTypes.SCREENSHOT_TOOLTIP.name()),
                                          true);
        toolBar.getItems().add(scrnshotButton);
    }

    @Override
    protected void setToolbarHandlers(ApplicationTemplate applicationTemplate) {
        applicationTemplate.setActionComponent(new AppActions(applicationTemplate));
        newButton.setOnAction(e -> {applicationTemplate.getActionComponent().handleNewRequest();
                                    leftPanel.setVisible(true);});
        saveButton.setOnAction(e -> applicationTemplate.getActionComponent().handleSaveRequest());
        loadButton.setOnAction(e -> {applicationTemplate.getActionComponent().handleLoadRequest();
                                    leftPanel.setVisible(true); saveButton.setDisable(true);
            ((AppActions) applicationTemplate.getActionComponent()).setIsUnsavedProperty(false);});
        exitButton.setOnAction(e -> applicationTemplate.getActionComponent().handleExitRequest());
        printButton.setOnAction(e -> applicationTemplate.getActionComponent().handlePrintRequest());
        scrnshotButton.setOnAction(e -> {
            try {
                ((AppActions)applicationTemplate.getActionComponent()).handleScreenshotRequest();
            } catch (IOException ignored) { }
        });
    }

    @Override
    public void initialize() {
        layout();
        setWorkspaceActions();
    }

    @Override
    public void clear() {
        textArea.clear();
        chart.getData().clear();
        dataInfo.setText(""); //clear the label text
    }

    public String getCurrentText() { return textArea.getText(); }
    public void setTextArea(String string) { textArea.setText(string);}
    public void setDataInfo(String string){ dataInfo.setText(string);}
    public LineChart<Number, Number> getChart(){ return chart; }
    public Button getScrnshotButton() { return scrnshotButton; }
    public Button getRunButton() { return runButton; }
    public Button getStartOverButton() { return startOverButton; }
    public Button getConfu(){return confu;}
    public Button getConfu2(){return confu2;}

    private void layout() {
        PropertyManager manager = applicationTemplate.manager;
        NumberAxis      xAxis   = new NumberAxis();
        NumberAxis      yAxis   = new NumberAxis();
        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false); //let the original graph stop moving, and line not jumping around
        chart.setTitle(manager.getPropertyValue(AppPropertyTypes.CHART_TITLE.name()));
        pairMap = new HashMap<>();//used to store configuration
        leftPanel = new VBox(8);
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setPadding(new Insets(10));

        VBox.setVgrow(leftPanel, Priority.ALWAYS);
        leftPanel.setMaxSize(windowWidth * 0.29, windowHeight * 0.3);
        leftPanel.setMinSize(windowWidth * 0.29, windowHeight * 0.3);

        Text   leftPanelTitle = new Text(manager.getPropertyValue(AppPropertyTypes.LEFT_PANE_TITLE.name()));
        String fontname       = manager.getPropertyValue(AppPropertyTypes.LEFT_PANE_TITLEFONT.name());
        Double fontsize       = Double.parseDouble(manager.getPropertyValue(AppPropertyTypes.LEFT_PANE_TITLESIZE.name()));
        leftPanelTitle.setFont(Font.font(fontname, fontsize));

        textArea = new TextArea();
        textArea.setMinHeight(windowHeight*0.15);
        dataInfo = new Label();
        dataInfo.setWrapText(true);
        dataInfo.setMinSize(windowWidth * 0.29,windowHeight * 0.3);

        HBox processButtonsBox = new HBox(30);
        displayButton = new Button(manager.getPropertyValue(AppPropertyTypes.DISPLAY_BUTTON_TEXT.name()));
        checkBox = new CheckBox();
        checkBox.setText(manager.getPropertyValue(AppPropertyTypes.CHECK_BOX_TEXT.name()));

        algoTypes = new VBox();
        algoVbox = new VBox();
        Text algo = new Text(manager.getPropertyValue(AppPropertyTypes.ALGO_TYPES.name()));
        clustering = new Button(manager.getPropertyValue(AppPropertyTypes.CLUSTERING.name()));
        classfication = new Button(manager.getPropertyValue(AppPropertyTypes.CLASSIFICATION.name()));
        algoTypes.getChildren().addAll(algo,classfication,clustering);
        algoTypes.setVisible(false);

        HBox.setHgrow(processButtonsBox, Priority.ALWAYS);
        processButtonsBox.getChildren().addAll(displayButton, checkBox);

        leftPanel.getChildren().addAll(leftPanelTitle, textArea, processButtonsBox, dataInfo,algoTypes);
        leftPanel.setMaxSize(windowWidth * 0.5, windowHeight * 0.8);
        leftPanel.setMinSize(windowWidth * 0.5, windowHeight * 0.8);
        leftPanel.setVisible(false);

        StackPane rightPanel = new StackPane(chart);
        rightPanel.setMaxSize(windowWidth * 0.5, windowHeight * 0.8);
        rightPanel.setMinSize(windowWidth * 0.5, windowHeight * 0.8);
        StackPane.setAlignment(rightPanel, Pos.CENTER);

        primaryScene.getStylesheets().add(manager.getPropertyValue(AppPropertyTypes.CSS_RESOURCE_PATH.name()));

        workspace = new HBox(leftPanel, rightPanel);
        HBox.setHgrow(workspace, Priority.ALWAYS);

        appPane.getChildren().add(workspace);
        VBox.setVgrow(appPane, Priority.ALWAYS);

        //enable textArea depending on the newValue(box is selected or not)
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            AppData data = (AppData) applicationTemplate.getDataComponent();
            if(newValue.equals(true)) {
                classfication.setDisable(!data.twoNonNull());
                //cluster set disable when only one instance, no need cluster
                if(data.getNumberInstance()<=1)
                    clustering.setDisable(true);
                else
                    clustering.setDisable(false);
                algoTypes.setVisible(data.dataValidity(getCurrentText()));
            }
            else
                algoTypes.setVisible(newValue);
            setDataInfo(((AppActions)applicationTemplate.getActionComponent()).dataInfo());
            textArea.setDisable(newValue); algoVbox.getChildren().clear();});
    }

    private void setWorkspaceActions() {
        setTextAreaActions();
        setButtonActions();
    }

    private void setTextAreaActions() {
        AppActions appActions = ((AppActions)applicationTemplate.getActionComponent());
        newButton.setDisable(false);
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {

            try {
                if (!newValue.equals(oldValue)) {
                    if (!newValue.isEmpty()) {
                        ((AppActions) applicationTemplate.getActionComponent()).setIsUnsavedProperty(true);
                        if (newValue.charAt(newValue.length() - 1) == '\n')
                            hasNewText = true;
                        newButton.setDisable(false);
                        saveButton.setDisable(false);
                        if(appActions.getAllLines()!=null)
                            while(countLine()<10 && appActions.getAllLines().size()>0) textArea.appendText(appActions.getFirstLines());
                    } else {
                        hasNewText = true;
                        newButton.setDisable(true);
                        saveButton.setDisable(true);
                        checkBox.setSelected(false);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println(newValue);
            }
        }
        );
    }

    //disable the save button when data is saved.
    public void setSaveButtonAction(boolean saved){
        saveButton.setDisable(!saved);
    }
    //call this method to initalize the checkBox as checked.
    public void setCheckBox(Boolean status){
        checkBox.setSelected(status);
    }

    //save the chart as image as pass to other method.
    public WritableImage take() {
        return chart.snapshot(new SnapshotParameters(), null);
    }

    private void setButtonActions() {
        displayButton.setOnAction(event -> {
            if (hasNewText) {
                displayData();
            }
        });
        //enable and disable scrnshotButton base on the chart
        chart.getData().addListener((ListChangeListener<XYChart.Series<Number, Number>>) c -> {
            if(chart.getData().isEmpty()) {
                 scrnshotButton.setDisable(true);
            }
  //          else if (!chart.getData().isEmpty())
    //            scrnshotButton.setDisable(false);
        });

        classfication.setOnAction(event -> {
            classFlag=true; clusterFlag=false;
            setAlgoSets();
        });
        clustering.setOnAction(event ->{
            classFlag=false; clusterFlag=true;
            setAlgoSets();
        });
    }

    /**
     * creating a Vbox for types of algo when clicked on either classification or clustering.
     *          including Radio Buttons
     * use flag (global variable) to indicate it's classification or clustering
     */
    private void setAlgoSets(){
        HBox radioBu = new HBox();
        confu = new Button();
        algoSets = new RadioButton();
        group = new ToggleGroup();
        PropertyManager manager = applicationTemplate.manager;
        String iconsPath = SEPARATOR + String.join(SEPARATOR,
                manager.getPropertyValue(GUI_RESOURCE_PATH.name()),
                manager.getPropertyValue(ICONS_RESOURCE_PATH.name()));
        String settingPath = String.join(SEPARATOR, iconsPath,
                manager.getPropertyValue(AppPropertyTypes.SETTING_ICON.name()));
        String runPath = String.join(SEPARATOR,
                iconsPath,
                manager.getPropertyValue(AppPropertyTypes.RUN_ICON.name()));
        Image imageS = new Image(settingPath), imageRun = new Image(runPath);
        ImageView[] setting =new ImageView[2];
        //
        for(int i=0; i< setting.length; i++){
            setting[i] = new ImageView(imageS);
            setting[i].setFitHeight(20);
            setting[i].setFitWidth(20);
        }
        ImageView run =new ImageView(imageRun);
        run.setFitHeight(40);
        run.setFitWidth(40);

        runButton = new Button();
        startOverButton = new Button(manager.getPropertyValue(AppPropertyTypes.START_OVER_BUTTON.name()));
        algoTypes.setVisible(false);
        Text title = new Text(manager.getPropertyValue(AppPropertyTypes.ALGO_TYPES.name()));
        algoSets.setToggleGroup(group);

        confu2 = new Button();
        confu.setGraphic(setting[0]);confu2.setGraphic(setting[1]);
        radioBu.getChildren().addAll(algoSets, confu);
        algoSets.setGraphicTextGap(20);
        if(classFlag && !clusterFlag) {
            algoSets.setText(manager.getPropertyValue(AppPropertyTypes.R_CLASS.name()));
            algoVbox.getChildren().addAll(title, radioBu,runButton,startOverButton);
        }
        else {
            algoSets.setText(manager.getPropertyValue(AppPropertyTypes.R_CLUST.name()));
            algoSet2 = new RadioButton(manager.getPropertyValue(AppPropertyTypes.K_CLUST.name()));
            algoSet2.setToggleGroup(group);
            HBox radioBu2 = new HBox();
            radioBu2.getChildren().addAll(algoSet2,confu2);
            algoVbox.getChildren().addAll(title, radioBu, radioBu2,runButton,startOverButton);
        }

        runButton.setGraphic(run);
        runButton.setDisable(true);
        algoVbox.setSpacing(5);
        leftPanel.getChildren().remove(algoVbox);
        leftPanel.getChildren().add(algoVbox);
        startOverButton.setVisible(false);
        startOverButton.setOnAction(e->{
            runButton.setDisable(false); startOverButton.setVisible(false);
        });
        setAlgoAction();
    }

    private void setAlgoAction(){
        confu.setOnAction(event ->{
            runConfiguration();
            if(algoSets.isSelected()) runButton.setDisable(false);
        });
        confu2.setOnAction(event ->{
            runConfiguration();
            if(algoSet2.isSelected()) runButton.setDisable(false);
        });
        group.selectedToggleProperty().addListener((observable, oldValue, newValue) ->
        {
            String name = ((RadioButton) newValue).getText();
            final String one = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.K_CLUST.name()),
                    two = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.R_CLUST.name());
            if(name.equals(one)) {
                kClusterFlag = true;RClusterFlag = false;classFlag = false;
            }
            else if(name.equals(two)) {
                kClusterFlag = false;RClusterFlag = true;classFlag = false;
            }
            //The run button is active only if an algorithm has been chosen,
            // and that chosen algorithm already has a runtime configuration.(clicked)
            if(newValue.isSelected()) {
                if(pairMap.containsKey(clusterInput) && clusterFlag)
                    runButton.setDisable(false);
                else if (pairMap.containsKey(classInput) &&classFlag)
                    runButton.setDisable(false);
            }
        });
        runButton.setOnAction(event -> callToAlgo());
    }
    /**
     *Layout to set up the runConfiguration
     * precondition: From the primary window, the user has successfully selected an algorithm to run.
     * flag(boolean): check the type of algo selected.
     * If any configuration is invalid, the software shall handle it gracefully by designing robust methods
     * in the back-end. The values decided in such cases must be reflected in the GUI (e.g., if the user chooses
     * the update interval to be -5, it is reset to 1; and this reset is shown in the GUI).
     */
    private void runConfiguration(){
        PropertyManager manager = applicationTemplate.manager;
        Stage runConfiguration = new Stage();
        runConfiguration.initModality(Modality.WINDOW_MODAL); //prevent user from owner stage before the finish the new stage.
        runConfiguration.initOwner(primaryStage); //set the owner stage

        runConfiguration.setTitle(manager.getPropertyValue(AppPropertyTypes.ALGO_RUN_CON_TITLE.name()));
        Text i = new Text(manager.getPropertyValue(AppPropertyTypes.MAX_ITERATION.name())),
                j = new Text(manager.getPropertyValue(AppPropertyTypes.UPDATE_INTERVAL.name())),
                l = new Text(manager.getPropertyValue(AppPropertyTypes.NUMBER_LABLES_TA.name()));
        iter = new TextArea();updateInter= new TextArea(); numberlables = new TextArea();
        iter.setMaxSize(50,5);
        updateInter.setMaxSize(50,5);
        numberlables.setMaxSize(50,5);
        con = new CheckBox(manager.getPropertyValue(AppPropertyTypes.RUN_OR_NOT.name()));
        //close the window by pressing finish button
        Button finish = new Button(manager.getPropertyValue(AppPropertyTypes.FINISH.name()));
        HBox a = new HBox(i,iter),b = new HBox(j,updateInter),
                c = new HBox(l,numberlables), d = new HBox(con,finish);

        VBox infoPane = new VBox(5);
        infoPane.setPadding(new Insets(10, 20, 20, 20));
        infoPane.setAlignment(Pos.CENTER);
        infoPane.setSpacing(10);
        infoPane.getChildren().clear();
        if(classFlag && !clusterFlag){
            infoPane.getChildren().addAll(a,b,d);
            if (pairMap.containsKey(classInput)) {
                iter.setText(String.valueOf(classInput[0]));
                updateInter.setText(String.valueOf(classInput[1]));
                con.setSelected(pairMap.get(classInput));
            }
        }
        else {
            infoPane.getChildren().addAll(a, b, c, d);
            if(pairMap.containsKey(clusterInput)) {
                iter.setText(String.valueOf(clusterInput[0]));
                updateInter.setText(String.valueOf(clusterInput[1]));
                numberlables.setText(String.valueOf(clusterInput[2]));
                con.setSelected(pairMap.get(clusterInput));
            }
        }
        //close the window by pressing finish button
        finish.setOnAction(e-> runConfiguration.close());
        Scene layout = new Scene(infoPane);
        runConfiguration.setScene(layout);
        runConfiguration.showAndWait();
        //if input is not corrected, keep updating.
        while(!checkInputCon()){
            runConfiguration.showAndWait();
        }
    }
    private boolean checkInputCon(){
        PropertyManager manager = applicationTemplate.manager;
        AppData data = (AppData) applicationTemplate.getDataComponent();
        //check if the input is valid or not after pressing finishButton
        int iterInt,updateInt, lablesInt;
        final String five = "5", one = "1";
        try {
            iterInt = Integer.parseInt(iter.getText());
            updateInt = Integer.parseInt(updateInter.getText());
            if (iterInt < 0) {
                inputDialog(manager.getPropertyValue(AppPropertyTypes.RE_INPUT.name()),
                        manager.getPropertyValue(AppPropertyTypes.INVALID_ITER.name()));
                iter.setText(five);
                return false;
            }
            if (updateInt < 0) {
                inputDialog(manager.getPropertyValue(AppPropertyTypes.RE_INPUT.name()),
                        manager.getPropertyValue(AppPropertyTypes.INVALID_INTERV.name()));
                updateInter.setText(String.valueOf(iterInt));
                return false;
            }
            if(clusterFlag && !classFlag) {
                lablesInt = Integer.parseInt(numberlables.getText());
                if (lablesInt < 0) {
                    inputDialog(manager.getPropertyValue(AppPropertyTypes.RE_INPUT.name()), "");
                    numberlables.setText(one);
                    return false;
                }
                else if(lablesInt>data.getNumberInstance()) {
                    inputDialog(manager.getPropertyValue(AppPropertyTypes.RE_INPUT.name()),
                            manager.getPropertyValue(AppPropertyTypes.TOO_MANY_LABELS.name()));
                    return false;
                }
                storeInput(con.isSelected(),iterInt,updateInt,lablesInt);
            }
            else
                storeInput(con.isSelected(),iterInt,updateInt);
        } catch (NumberFormatException e) {
            inputDialog(manager.getPropertyValue(AppPropertyTypes.NOT_INT.name()), "");
            return false;
            //runConfiguration.showAndWait();
        }
        return true;
    }
    public boolean checkInputConTest(String a, String b, String c) throws NumberFormatException{
        //check if the input is valid or not after pressing finishButton
        int[] data = new int[3];
        int iterInt,updateInt, lablesInt;
        iterInt = Integer.parseInt(a);
        updateInt = Integer.parseInt(b);
        if (iterInt < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (updateInt < 0) {
            throw new IndexOutOfBoundsException();
        }
        if(c!=null) {
            lablesInt = Integer.parseInt(c);
            if (lablesInt < 0) {
                throw new IndexOutOfBoundsException();
            } else if (lablesInt > iterInt) {
                throw new IndexOutOfBoundsException();
            }
        }
        return true;
    }

    private void storeInput(boolean t,int a, int b, int c){
        clusterInput[0]=a;
        clusterInput[1]=b;
        clusterInput[2]=c;
        pairMap.put(clusterInput,t);
    }
    private void storeInput(boolean t, int a, int b){
        classInput[0]=a;
        classInput[1]=b;
        pairMap.put(classInput,t);
    }

    /**
     * initalize Class<?> algoClass and Algorithm algo.
     * use reflection to load the correct algorithm class by using algoFlags
     *
     */
    private void setAlgoClass() {
        AppActions actionComponent = (AppActions) applicationTemplate.getActionComponent();
        boolean toContinue = false;
        int[] arugment = null;
        DataSet dataSet;

        try {
            dataSet = DataSet.fromTSDFile(actionComponent.getDataFilePath());
        } catch (IOException|NullPointerException e) {
            DataSet finalDataSet = new DataSet();
            Arrays.stream(textArea.getText().split("\n")).forEach(line -> {
                try {
                    finalDataSet.addInstance(line);
                } catch (DataSet.InvalidDataNameException e1) {
                    e1.printStackTrace();
                }
            });
            dataSet = finalDataSet;
        }

        if (classFlag && !clusterFlag) {
            try {
                algoClass = Class.forName(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.RandomClassifier.name()));
                toContinue = pairMap.get(classInput);
                arugment = classInput;
            } catch (ClassNotFoundException ignored) {}
        }
        else if (!classFlag && clusterFlag) {
            toContinue = pairMap.get(clusterInput);
            arugment = clusterInput;
            if(kClusterFlag && ! RClusterFlag) {
                try {
                    algoClass = Class.forName(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.KMeansClusterer.name()));
                } catch (ClassNotFoundException ignored) { }
            }
            else if(RClusterFlag && !kClusterFlag){
                try {
                    algoClass = Class.forName(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.RandomClusterer.name()));
                } catch (ClassNotFoundException ignored) {}
            }
        }
        if (algoClass != null) {
            if (currentAlgo == null || !currentAlgo.isAlive()) {
                try {
                    Constructor constructor = algoClass.getConstructors()[0];
                    algo = (Algorithm) constructor.newInstance(dataSet, arugment, toContinue, this);
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException ignored) {}
            }
        }
    }

    /**
     * Call to run the actual algo by creating a thread and run the algo as a thread
     * but the program only display one line??? how to display
     */
    private void callToAlgo(){
        if (currentAlgo == null || !currentAlgo.isAlive()) {
            setAlgoClass();
            currentAlgo = new Thread(algo);
            currentAlgo.start();
            if (algo.tocontinue())
                runButton.setDisable(true);
            else
                runButton.setDisable(false);
        } else if (currentAlgo.isAlive()) {
                //if thread is still running, meaning there are still update left.
                //keep updating
            runButton.setDisable(false);
            currentAlgo.interrupt();
        }
    }

    public void displayData(){
        try {
            AppData dataComponent = (AppData) applicationTemplate.getDataComponent();
            AppActions appActions = (AppActions)applicationTemplate.getActionComponent();
            chart.getData().clear();
            applicationTemplate.getDataComponent().clear();
            chart.getData().add(new XYChart.Series<>()); //line holder
            String b="";
            if(appActions.getAllLines()!=null){
                b = appActions.getAllLines().stream().collect(Collectors.joining("\n"));
            }
            String a = textArea.getText()+b;
            dataComponent.loadData(a);
            dataComponent.displayData();
        } catch (Exception ignored) { }
    }

    public void displayClassData(){
        AppData dataComponent = (AppData) applicationTemplate.getDataComponent();
        AppActions appActions = (AppActions)applicationTemplate.getActionComponent();
        String b="";
        if(appActions.getAllLines()!=null){
            b = appActions.getAllLines().stream().collect(Collectors.joining("\n"));
        }
        String a = textArea.getText()+b;
        dataComponent.loadData(a);
        dataComponent.displayData();
    }
    private void inputDialog(String s,String message){
        ErrorDialog dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
        PropertyManager manager  = applicationTemplate.manager;
        String          errTitle = manager.getPropertyValue(AppPropertyTypes.FORMAT_ERROR.name());
        String errorMess = s+manager.getPropertyValue(AppPropertyTypes.NEW_LINE.name())+message;
        dialog.show(errTitle,errorMess);
    }

    private int countLine(){
        int i=0;
        for(String ignored : textArea.getText().split("\n"))
            i++;
        return i;
    }

    /**
     * get the last activated thread. To handle exit request
     * @return last activated thread.
     */
    public Thread currentRunningAlgo(){
        if(currentAlgo==null) return null;
        else return currentAlgo;
    }
    public double getXMax(){
        return ((AppData) applicationTemplate.getDataComponent()).getxMax(); }
    public double getXMin(){
        return ((AppData) applicationTemplate.getDataComponent()).getxMin(); }

}
