package ui;

import org.junit.Test;


public class AppUITest {

    //test case for Classification Run configuration values
    @Test
    public void testClassCon() {
        AppUI appUI = new AppUI();
        String iter = "5", updateInt= "2";
        appUI.checkInputConTest(iter,updateInt,null);
    }
    //test case for non-integer value
    @Test(expected = NumberFormatException.class)
    public void testClassConBoundary() {
        AppUI appUI = new AppUI();
        String iter = "e", updateInt= "2";
        appUI.checkInputConTest(iter,updateInt,null);
    }

    //test case for negative iteration
    @Test(expected = IndexOutOfBoundsException.class)
    public void testClassConNegativeIter() {
        AppUI appUI = new AppUI();
        String iter = "-2", updateInt= "2";
        appUI.checkInputConTest(iter,updateInt,null);
    }

    //test case for negative update intervals
    @Test(expected = IndexOutOfBoundsException.class)
    public void testClassConNegativeUpdateInter() {
        AppUI appUI = new AppUI();
        String iter = "2", updateInt= "-1";
       appUI.checkInputConTest(iter,updateInt,null);
    }


    //test case for Cluster Run configuration values
    @Test
    public void testClusterCon() {
        AppUI appUI = new AppUI();
        String iter = "5", updateInt= "2", numberLabels = "3";
        appUI.checkInputConTest(iter,updateInt,numberLabels);
    }

    //test case for non-integer value
    @Test(expected = NumberFormatException.class)
    public void testClusterConBoundary() {
        AppUI appUI = new AppUI();
        String iter = "e", updateInt= "2",numberLabels = "3";
        appUI.checkInputConTest(iter,updateInt,numberLabels);
    }
    //test case for negative iteration
    @Test(expected = IndexOutOfBoundsException.class)
    public void testClusterConNegativeValue() {
        AppUI appUI = new AppUI();
        String iter = "-2", updateInt= "2",numberLabels = "3";
        appUI.checkInputConTest(iter,updateInt,numberLabels);
    }
    //test case for negative update interval
    @Test(expected = IndexOutOfBoundsException.class)
    public void testClusterConNegativeUpdateInter() {
        AppUI appUI = new AppUI();
        String iter = "4", updateInt= "-2",numberLabels = "3";
        appUI.checkInputConTest(iter,updateInt,numberLabels);
    }

    //test case for negative number of Label need
    @Test(expected = IndexOutOfBoundsException.class)
    public void testClusterConNegativeLabels() {
        AppUI appUI = new AppUI();
        String iter = "4", updateInt= "2",numberLabels="-2";
        appUI.checkInputConTest(iter,updateInt,numberLabels);
    }
}