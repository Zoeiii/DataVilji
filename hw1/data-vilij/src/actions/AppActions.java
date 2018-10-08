package actions;

import dataprocessors.AppData;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.components.ActionComponent;
import vilij.components.ConfirmationDialog;
import vilij.components.Dialog;
import vilij.components.ErrorDialog;
import vilij.propertymanager.PropertyManager;
import vilij.settings.PropertyTypes;
import vilij.templates.ApplicationTemplate;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Collectors;

import static vilij.settings.PropertyTypes.SAVE_WORK_TITLE;
import static vilij.templates.UITemplate.SEPARATOR;

/**
 * This is the concrete implementation of the action handlers required by the application.
 *
 * @author Ritwik Banerjee
 */
public final class AppActions implements ActionComponent {

    /** The application to which this class of actions belongs. */
    private final ApplicationTemplate applicationTemplate;
    private final PropertyManager manager;

    /** Path to the data file currently active. */
    Path dataFilePath;

    /** The boolean property marking whether or not there are any unsaved changes. */
    final SimpleBooleanProperty isUnsaved;
    private Boolean isValid;
    private LinkedList<String> allLines;

    public AppActions(ApplicationTemplate applicationTemplate) {
        this.applicationTemplate = applicationTemplate;
        this.manager = applicationTemplate.manager;
        this.isUnsaved = new SimpleBooleanProperty(false);
    }

    public Path getDataFilePath() { return dataFilePath; }

    public void setIsUnsavedProperty(boolean property) { isUnsaved.set(property); }

    @Override
    public void handleNewRequest() {
        try {
            if (!isUnsaved.get() || promptToSave()) {
                applicationTemplate.getDataComponent().clear();
                applicationTemplate.getUIComponent().clear();
                isUnsaved.set(false);
                dataFilePath = null;
            }
        } catch (IOException e) { errorHandlingHelper(); }
    }

    @Override
    public void handleSaveRequest() {
        if (isUnsaved.getValue()) {
            try {
                selectFile();
            } catch (IOException ignored) { }
        }
        else{
            save();
        }
        ((AppUI)applicationTemplate.getUIComponent()).setSaveButtonAction(isUnsaved.getValue());
    }

    /**
     * The chart should display all 2,000 points.
     * If you delete a line from the first ten, the data should move up and show the eleventh line.
     * Similarly, if you delete all ten lines, then the 11 - 20 lines should become visible in the text area.
     * Clicking display after doing this should display the remaining 1990 lines.
     * The chart display always reflects the entire data, not just what is visible in the text area.
     */
    @Override
    public void handleLoadRequest() {
        String newLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NEW_LINE.name());
        FileChooser load = new FileChooser();
        //set extension for the file
        String description = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_FILE_EXT_DESC.name());
        String extension = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_FILE_EXT.name());
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(String.format("%s (*%s)", description, extension),
                String.format("*%s", extension));

        load.getExtensionFilters().add(extFilter);
        String dataDirPath = SEPARATOR + applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_RESOURCE_PATH.name());
        URL dataDirURL = getClass().getResource(dataDirPath);

        //initialize file directory
        load.setInitialDirectory(new File(dataDirURL.getFile()));
        File file = load.showOpenDialog(applicationTemplate.getUIComponent().getPrimaryWindow());
        //get data from the file, and check if the data is valid, if not does not load and no change occurs.
        //continue to display line after user delete lines*, still all the data should be displayed.
        if(file!=null) {
            dataFilePath = file.toPath();
            applicationTemplate.getDataComponent().loadData(dataFilePath);
            StringBuilder displayed = new StringBuilder();
            allLines = ((AppData) applicationTemplate.getDataComponent()).getData();
            String data = allLines.stream().collect(Collectors.joining(newLine));

            if (((AppData) applicationTemplate.getDataComponent()).loadData(data)){
                applicationTemplate.getUIComponent().clear(); //clear textArea and chart if there's any data before loading.
                //get the first 10 lines from the data load.
                for (int i = 0; i < 10; i++) {
                    if(allLines.size()!=0) {
                        displayed.append(allLines.getFirst()).append(newLine);
                        allLines.removeFirst();
                    }
                    else    break;
                }
                ((AppUI) applicationTemplate.getUIComponent()).setTextArea(displayed.toString());
                ((AppUI) applicationTemplate.getUIComponent()).setCheckBox(true);
                ((AppUI)applicationTemplate.getUIComponent()).setDataInfo(dataInfo());
            }
        }

    }
    public String dataInfo(){
        StringBuilder info = new StringBuilder();
        String newLine = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.NEW_LINE.name());
        info.append(((AppData) applicationTemplate.getDataComponent()).getDataInfo()).append(newLine);
        if(dataFilePath!=null)
            info.append(manager.getPropertyValue(AppPropertyTypes.LOAD_FROM.name())).append(dataFilePath).append(newLine);
        return info.toString();
    }

    @Override
    public void handleExitRequest() {
        //use reflection to check if any thread is running
        Thread obj = ((AppUI) applicationTemplate.getUIComponent()).currentRunningAlgo();
        if(obj==null) {
            try {
                if (!isUnsaved.get() || promptToSave())
                    System.exit(0);
            } catch (IOException e) {
                errorHandlingHelper();
            }
        }
        else{
            try {
                if ((!isUnsaved.get() || promptToSave()) && (!obj.isAlive() || contiRuningDialog()))
                    System.exit(0);
            } catch (IOException e) {
                errorHandlingHelper();
            }
        }
    }

    @Override
    public void handlePrintRequest() {
        // TODO: NOT A PART OF HW 1
    }

    public void handleScreenshotRequest() throws IOException{
        final WritableImage screenShot = ((AppUI) applicationTemplate.getUIComponent()).take();

        FileChooser fileChooser = new FileChooser();
        String dataDirPath = SEPARATOR + applicationTemplate.manager.getPropertyValue(AppPropertyTypes.DATA_RESOURCE_PATH.name());
        URL dataDirURL = getClass().getResource(dataDirPath);

        if (dataDirURL == null)
            throw new FileNotFoundException(applicationTemplate.manager.getPropertyValue(AppPropertyTypes.RESOURCE_SUBDIR_NOT_FOUND.name()));

        fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
        fileChooser.setTitle(applicationTemplate.manager.getPropertyValue(SAVE_WORK_TITLE.name()));
        String description = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.IMAGE_FILE_EXT_DESC.name());
        String extension = applicationTemplate.manager.getPropertyValue(AppPropertyTypes.IMAGE_FILE_EXT.name());
        ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*%s)", description, extension),
                String.format("*%s", extension));

        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(applicationTemplate.getUIComponent().getPrimaryWindow());

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(screenShot, null), applicationTemplate.manager.getPropertyValue(AppPropertyTypes.IMAGE_FILE_EXT.name()), file);
        } catch (IllegalArgumentException | IOException ignored) { }

    }

    /**
     * This helper method verifies that the user really wants to save their unsaved work, which they might not want to
     * do. The user will be presented with three options:
     * <ol>
     * <li><code>yes</code>, indicating that the user wants to save the work and continue with the action,</li>
     * <li><code>no</code>, indicating that the user wants to continue with the action without saving the work, and</li>
     * <li><code>cancel</code>, to indicate that the user does not want to continue with the action, but also does not
     * want to save the work at this point.</li>
     * </ol>
     *
     * @return <code>false</code> if the user presses the <i>cancel</i>, and <code>true</code> otherwise.
     */
    private boolean promptToSave() throws IOException {
        PropertyManager    manager = applicationTemplate.manager;
        ConfirmationDialog dialog  = ConfirmationDialog.getDialog();
        dialog.show(manager.getPropertyValue(AppPropertyTypes.SAVE_UNSAVED_WORK_TITLE.name()),
                    manager.getPropertyValue(AppPropertyTypes.SAVE_UNSAVED_WORK.name()));

        if (dialog.getSelectedOption() == null) return false; // if user closes dialog using the window's close button

        if (dialog.getSelectedOption().equals(ConfirmationDialog.Option.YES)) if(!selectFile()) return false;

        return !dialog.getSelectedOption().equals(ConfirmationDialog.Option.CANCEL);
    }

    public boolean selectFile() throws IOException {
        PropertyManager    manager = applicationTemplate.manager;
        setIsValid();
        if(isValid) {
            if (dataFilePath == null) {
                FileChooser fileChooser = new FileChooser();
                String dataDirPath = SEPARATOR + manager.getPropertyValue(AppPropertyTypes.DATA_RESOURCE_PATH.name());
                URL dataDirURL = getClass().getResource(dataDirPath);

                if (dataDirURL == null)
                    throw new FileNotFoundException(manager.getPropertyValue(AppPropertyTypes.RESOURCE_SUBDIR_NOT_FOUND.name()));

                fileChooser.setInitialDirectory(new File(dataDirURL.getFile()));
                fileChooser.setTitle(manager.getPropertyValue(SAVE_WORK_TITLE.name()));

                String description = manager.getPropertyValue(AppPropertyTypes.DATA_FILE_EXT_DESC.name());
                String extension = manager.getPropertyValue(AppPropertyTypes.DATA_FILE_EXT.name());
                ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*%s)", description, extension),
                        String.format("*%s", extension));

                fileChooser.getExtensionFilters().add(extFilter);
                File selected = fileChooser.showSaveDialog(applicationTemplate.getUIComponent().getPrimaryWindow());
                if (selected != null) {
                    dataFilePath = selected.toPath();
                    save();
                } else return false; // if user presses escape after initially selecting 'yes'
            } else
                save();
            return true;
        }
        else return false;
    }
    private void save() {
        applicationTemplate.getDataComponent().saveData(dataFilePath);
        isUnsaved.set(false);
    }

    private void errorHandlingHelper() {
        ErrorDialog     dialog   = (ErrorDialog) applicationTemplate.getDialog(Dialog.DialogType.ERROR);
        String          errTitle = manager.getPropertyValue(PropertyTypes.SAVE_ERROR_TITLE.name());
        String          errMsg   = manager.getPropertyValue(PropertyTypes.SAVE_ERROR_MSG.name());
        String          errInput = manager.getPropertyValue(AppPropertyTypes.SPECIFIED_FILE.name());
        dialog.show(errTitle, errMsg + errInput);
    }

    /**
     * This helper method verifies that the user really wants to exit while still runing the alog
     * The user will be presented with three options:
     * <ol>
     * <li><code>yes</code>, indicating that the user wants to leave,</li>
     * <li><code>no</code>, indicating that the user wants to continue</li>
     * <li><code>cancel</code>, to indicate that the user does not want to go back to the application</li>
     * </ol>
     *
     * @return <code>false</code> if the user presses the <i>cancel</i>, and <code>true</code> otherwise.
     */
    private boolean contiRuningDialog() {
        ConfirmationDialog dialog = (ConfirmationDialog) applicationTemplate.getDialog(Dialog.DialogType.CONFIRMATION);
        String errTitle = manager.getPropertyValue(AppPropertyTypes.EXIT_WHILE_RUNNING.name());
        String errMsg = manager.getPropertyValue(AppPropertyTypes.EXIT_WHILE_RUNNING_WARNING.name());
        dialog.show(errTitle, errMsg);
        if (dialog.getSelectedOption() == null) return false; // if user closes dialog using the window's close button

        if (dialog.getSelectedOption().equals(ConfirmationDialog.Option.YES))
            return true;
        else if(dialog.getSelectedOption().equals(ConfirmationDialog.Option.NO))
            return false;
        return !dialog.getSelectedOption().equals(ConfirmationDialog.Option.CANCEL);

    }

    private void setIsValid(){
        AppData dataComponent = (AppData) applicationTemplate.getDataComponent();
        isValid = dataComponent.dataValidity(((AppUI) applicationTemplate.getUIComponent()).getCurrentText());
    }

    public String getFirstLines() {
        String first = allLines.getFirst()+'\n';
        allLines.removeFirst();
        return first;
    }

    public LinkedList<String> getAllLines() {
        return allLines;
    }

}
