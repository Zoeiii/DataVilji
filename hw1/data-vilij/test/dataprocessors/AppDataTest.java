package dataprocessors;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

public class AppDataTest {


    //Saving data from the text-area in the UI to a .tsd file
    @Test
    public void saveDataToTsdFile() {
        String  data = "@Instance6\tlabel2\t11,9.1";
        File f = new File("saving data to a .tsd file.tsd");
        AppData appData = new AppData();
        appData.saveDataTest(f.toPath(),data);
    }

    ////test case for null file path
    @Test(expected = NullPointerException.class)
    public void saveDataToNoneExistingFile() {
        Path p = null;
        String  data = "@Instance6\tlabel2\t11,9.1";
        AppData appData = new AppData();
        appData.saveDataTest(p,data);
    }
}