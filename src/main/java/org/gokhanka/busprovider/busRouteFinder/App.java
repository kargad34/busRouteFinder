package org.gokhanka.busprovider.busRouteFinder;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import spark.Spark;

import static spark.Spark.*;
/**
 * Entrance of the project hosting main method
 * @author gokhanka
 *
 */
public class App implements Runnable {

    static final Logger logger                       = LogManager.getLogger();
    RoutesWithStations  rs                           = null;
    TpsViaDelayedQueue  requestCountPerSecController = new TpsViaDelayedQueue(Utility.MAX_ALLOWED_REQUEST,
                                                                              Utility.MAX_ALLOWED_REQUEST_DURATION);
    Thread              threadNew                    = null;
    AtomicBoolean       halted                       = new AtomicBoolean(Utility.TRUE);
    boolean             fileReadSuccess              = Utility.TRUE;

    /**
     * Constructor For the APP class
     * APP class is used to host the features provided by busRouteFinder
     * It starts and stops HTTP server for REST API http://localhost:8088/api/direct?dep_sid={}&arr_sid={}
     * @param fileName : the data file in which there are routes and including stations
     */
    public App(String fileName) {
        LoggerContext context = (LoggerContext) LogManager.getContext(Utility.FALSE);
        File file = new File("src/main/resources/log4j2.xml");
        context.setConfigLocation(file.toURI());
        org.apache.log4j.BasicConfigurator.configure();
        rs = new RoutesWithStations();
        fileReadSuccess = rs.readFile(fileName);
        threadPool(Utility.HTTP_THEAD_MAX, Utility.HTTP_THEAD_MIN, Utility.HTTP_IDLE_TIME);
        port(Utility.HTTP_PORT);

    }
    /**
     * Used to start the HTTP Service
     */
    public void start() {
        if (this.threadNew == null) {
            threadNew = new Thread(this);
            threadNew.setDaemon(Utility.TRUE);
            threadNew.start();
        } else {
            if (halted.compareAndSet(Utility.TRUE, Utility.FALSE)) {
                initHttp();
                logger.info("HTTP Service Started");
            } else {
                logger.warn("HTTP Already Started");
            }
        }
    }

    /**
     *used to exit from the application
     */
    public void stop() {
        Utility.quit();
    }
    /**
     * Used to halt the HTTP Service, but it can be started again with start()
     */
    public void block() {
        if (halted.compareAndSet(Utility.FALSE, Utility.TRUE)) {
            Spark.stop();
            logger.warn("HTPP Stopped");
        } else {
            logger.warn("HTTP Already Stopped");
        }
    }

    public static void main(String[] args) {
        String fileName = Utility.retriveFile(args);
        if (fileName == null) {
            Utility.quit();
        }
        App app = new App(fileName);
        boolean started = app.runCommand(args[0].trim());
        if (started) {
            Scanner scanner = new Scanner(System.in);
            String command = "";
            while (!command.equalsIgnoreCase(Utility.STOP)) {
                System.out.print("Enter command (start|stop|block): ");
                command = scanner.next();
                if (app.runCommand(command) != Utility.TRUE)
                    break;
            }
            scanner.close();
            app.stop();
        }
    }

    /**
     * 
     * @param command : command to be executed: start or block
     * in order to start the HTTP REST Service or stop it
     * @return if the command is block or start true, otherwise false
     */
    public boolean runCommand(String command) {
        boolean result = Utility.TRUE;
        if (command.equalsIgnoreCase(Utility.BLOCK)) {
            block();
            System.out.println("HTTP blocked");
        } else if (command.equalsIgnoreCase(Utility.START)) {
            start();
            System.out.println("HTTP started");
        } else {
            logger.warn("Unknown or stop command: {}", command);
            result = Utility.FALSE;
        }
        return result;
    }

    public RoutesWithStations getRs() {
        return rs;
    }

    @Override
    public void run() {
        if (logger.isDebugEnabled())
            logger.debug("Hello World! I am on port {}", Utility.HTTP_PORT);
        if (isFileReadSuccess() && halted.compareAndSet(Utility.TRUE, Utility.FALSE)) {
            initHttp();
            logger.info("HTTP Service Started");
        } else {
            if (isFileReadSuccess()) {
                logger.warn("file read error");
                Utility.quit();
            }
        }
    }
    /**
     * To start the REST API and the related HTTP Service this method is used
     * Sparkjava is used to build the REST Service
     */
    private void initHttp() {
        init();
        before((req, res) -> {
            res.type("application/json");
        });
        get("/api/direct", (req, res) -> {
            String dep = req.queryParams("dep_sid").trim();
            String arr = req.queryParams("arr_sid").trim();
            if (Utility.validateInput(arr, dep)) {
                return executeRequest(dep, arr);
            } else {
                return rs.wrongIpnput(dep, arr);
            }
        }, Utility.json());
    }

    public boolean isFileReadSuccess() {
        return fileReadSuccess;
    }

    private Output executeRequest(String dep, String arr) {
        Output result = null;
        int dep_sid = -1;
        int arr_sid = -1;
        try {
            dep_sid = Integer.parseInt(dep);
            arr_sid = Integer.parseInt(arr);
            if (arr_sid == dep_sid)
                result = rs.arrAndDepSame(dep_sid, arr_sid);
            if (requestCountPerSecController.isTpsAvailable()) {
                result = rs.isThereDirectRoute(dep_sid, arr_sid);
            } else {
                result = rs.tpsExceeded(dep_sid, arr_sid);
            }
        } catch (Exception e) {
            result = rs.wrongIpnput(dep, arr);
        }
        return result;
    }

}
