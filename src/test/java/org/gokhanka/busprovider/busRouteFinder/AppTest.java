package org.gokhanka.busprovider.busRouteFinder;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for  App
 */
public class AppTest extends TestCase {


    public AppTest(String testName) {
        super(testName);
        
    }

    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    @org.junit.Test
    public void testApp1() {
        String testInput = new String("src/main/resources/unitTestData.txt");
        App app = new App(testInput);
        assertTrue(app.isFileReadSuccess());
        assertTrue(app.getRs().isThereDirectRoute(11, 125).isDirect_bus_route());
    }

    @org.junit.Test
    public void testApp2() {
        String testInput = new String("src/main/resources/unitTestData.txt");
        App app = new App(testInput);
        assertTrue(app.isFileReadSuccess());
        assertFalse(app.getRs().isThereDirectRoute(55, 244).isDirect_bus_route());
    }
    
    @org.junit.Test
    public void testApp3() {
        String testInput = new String("src/main/resources/unitTestData2.txt");
        App app = new App(testInput);
        assertFalse(app.isFileReadSuccess());
    }
}
