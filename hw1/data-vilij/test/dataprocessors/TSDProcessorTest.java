package dataprocessors;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point2D;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;

public class TSDProcessorTest {
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    //Parsing a single line of data in the TSD format to create an instance object.
    // This must include tests for suitable boundary values.
    @Test
    public void tdsProcessing() throws Exception {
        String firstLine = "@Instance2\tlabel2\t1.5,2.2";
        Point2D data = new Point2D(1.5,2.2);
        final Point2D[] testData = new Point2D[1];
        TSDProcessor tsdProcessor = new TSDProcessor();
        tsdProcessor.processString(firstLine);
        tsdProcessor.getDataPoints().values().stream().forEach(point2D -> testData[0] =point2D);
        Assert.assertEquals(tsdProcessor.getNumberInstance(),1);
        Assert.assertEquals(data, testData[0]);
    }

    //test case for 0 values
    @Test
    public void tdsProcessing2() throws Exception {
        String firstLine = "@Instance2\tlabel2\t0,0";
        Point2D data = new Point2D(0,0);
        final Point2D[] testData = new Point2D[1];
        TSDProcessor tsdProcessor = new TSDProcessor();
        tsdProcessor.processString(firstLine);
        tsdProcessor.getDataPoints().values().stream().forEach(point2D -> testData[0] =point2D);
        Assert.assertEquals(tsdProcessor.getNumberInstance(),1);
        Assert.assertEquals(data, testData[0]);
    }

    //test case for negative value
    @Test
    public void tdsProcessing3() throws Exception {
        String firstLine = "@Instance2\tlabel2\t-5,5";
        Point2D data = new Point2D(-5,5);
        final Point2D[] testData = new Point2D[1];
        TSDProcessor tsdProcessor = new TSDProcessor();
        tsdProcessor.processString(firstLine);
        tsdProcessor.getDataPoints().values().stream().forEach(point2D -> testData[0] =point2D);
        Assert.assertEquals(tsdProcessor.getNumberInstance(),1);
        Assert.assertEquals(data, testData[0]);
    }

    public static class JavaFXThreadingRule implements TestRule {

        /**
         * Flag for setting up the JavaFX, we only need to do this once for all tests.
         */
        private static boolean jfxIsSetup;

        @Override
        public Statement apply(Statement statement, Description description) {

            return new OnJFXThreadStatement(statement);
        }

        private static class OnJFXThreadStatement extends Statement {

            private final Statement statement;

            public OnJFXThreadStatement(Statement aStatement) {
                statement = aStatement;
            }

            private Throwable rethrownException = null;

            @Override
            public void evaluate() throws Throwable {

                if (!jfxIsSetup) {
                    setupJavaFX();

                    jfxIsSetup = true;
                }

                final CountDownLatch countDownLatch = new CountDownLatch(1);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            statement.evaluate();
                        } catch (Throwable e) {
                            rethrownException = e;
                        }
                        countDownLatch.countDown();
                    }
                });

                countDownLatch.await();

                // if an exception was thrown by the statement during evaluation,
                // then re-throw it to fail the test
                if (rethrownException != null) {
                    throw rethrownException;
                }
            }

            protected void setupJavaFX() throws InterruptedException {

                long timeMillis = System.currentTimeMillis();

                final CountDownLatch latch = new CountDownLatch(1);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // initializes JavaFX environment
                        new JFXPanel();

                        latch.countDown();
                    }
                });

                System.out.println("javafx initialising...");
                latch.await();
                System.out.println("javafx is initialised in " + (System.currentTimeMillis() - timeMillis) + "ms");
            }

        }

    }
}