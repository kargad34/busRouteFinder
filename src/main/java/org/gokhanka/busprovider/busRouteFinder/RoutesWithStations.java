package org.gokhanka.busprovider.busRouteFinder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Worker class for the project
 * It is holding the input data, providing build up of it via file
 * and providing the main business function to find out if there is bus route
 * between given two stations
 * 
 * Note: the data routesWithStations can be considered to be an independent class
 * as an enhancement
 * Note: validation over file is done not in detail, unique stationId, routeId and similar
 * controls can be added as validation methods during file parsing as an enhancement
 * @author gokhanka
 *
 */
public class RoutesWithStations {

    private static final Logger logger             = LogManager.getLogger();
    int                         workerCount        = Utility.ONE;
    private int[][]             routesWithStations = null;
    private AtomicInteger       routeCounter       = new AtomicInteger(0);

    public RoutesWithStations() {
        super();
    }

    /**
     * to read the file and retrieve the data in it
     * @param fileName
     * @return
     */
    public boolean readFile(String fileName) {
        long startTime = System.currentTimeMillis();
        boolean goon = Utility.TRUE;
        // just to read the first line and take the count
        try (BufferedReader buffer = new BufferedReader(new FileReader(fileName))) {
            routesWithStations = new int[Integer.parseInt(buffer.readLine().trim())][];
            int count = (int) Files.lines(Paths.get(fileName)).count();
            if ((count - Utility.ONE) != routesWithStations.length) {
                logger.error("File has not expected lines of routes expected {} exists {}",
                             routesWithStations.length,
                             (count - 1));
                goon = Utility.FALSE;
            }
            logger.info("file has {} routes", routesWithStations.length);
        } catch (IOException ex) {
            logger.error("file {} could not be read", fileName);
            goon = Utility.FALSE;
        }
        if (goon) {
            try (Stream<String> stream = Files.lines(Paths.get(fileName)).parallel()) {
                readStations(stream);
            } catch (IOException e) {
                logger.error("can  not open file", e);
                goon = Utility.FALSE;
            }
        }

        logger.info("Elapsed Time to read the file {}ms", (System.currentTimeMillis() - startTime));
        return goon;
    }

    /**
     * to calculate the worker thread count for the requests
     * @return
     */
    private int getWorkerCount() {
        int result = Utility.ONE;
        if (routeCounter.get() > Utility.COUNT_OF_ROUTES_PER_THEAD) {
            if (routeCounter.get() % Utility.COUNT_OF_ROUTES_PER_THEAD == 0) {
                result = (routeCounter.get() / Utility.COUNT_OF_ROUTES_PER_THEAD);
            } else {
                result = ((routeCounter.get() / Utility.COUNT_OF_ROUTES_PER_THEAD) + 1);
            }
        }
        return result;
    }

    /**
     * to find out if there is bus route between two stations
     * here for each request a fixed size(workerCount) of thread pool is assigned with
     * the task that is finding if there is one bus route.
     * Finding just one route is assumed to be enough!
     * @param srcStation
     * @param dstStation
     * @return
     */
    public Output isThereDirectRoute(int srcStation, int dstStation) {
        long startTime = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("Request Accepted");
        }
        SynchBoolean result = findIfThereisOneRoute(srcStation, dstStation);
        boolean res = result.isFound();
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= Utility.RESPONSE_TIMEOUT)
            logger.error("Took more then expected {}ms", elapsedTime);
        logger.info("route exist in {} - > {} {} found in {}ms",
                    srcStation,
                    dstStation,
                    res,
                    (elapsedTime));
        return new Output(srcStation, dstStation, res);
    }

    /**
     * search is done here
     * the array holding the station lists of routes is iterated by worker threads
     * each thread is given a specific part on the array thus can work in parallel
     * each thread is checking other via an synchronized boolean of any of the thread
     * already find the result(true)
     * @param srcStation
     * @param dstStation
     * @return
     */
    private SynchBoolean findIfThereisOneRoute(int srcStation, int dstStation) {
        SynchBoolean result = new SynchBoolean();
        CountDownLatch cl = new CountDownLatch(workerCount);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        int start = 0;
        int end = 0;
        for (int j = 0; j < workerCount; j++) {
            start = j * Utility.COUNT_OF_ROUTES_PER_THEAD;
            end = start + Utility.COUNT_OF_ROUTES_PER_THEAD;
            if (end > routeCounter.get())
                end = routeCounter.get();
            if (logger.isDebugEnabled()) {
                logger.debug("start {} end {}", start, end);
            }
            Runnable worker = new WorkerThreadForSearch("worker-" + ":" + j,
                                                        result,
                                                        routesWithStations,
                                                        srcStation,
                                                        dstStation,
                                                        cl,
                                                        start,
                                                        end);
            executor.execute(worker);
        }
        try {
            cl.await(Utility.RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
            executor.shutdownNow();
        } catch (InterruptedException e) {
            logger.error("interrupted while waiting for results", e);
        }
        return result;
    }

    public Output tpsExceeded(int srcStation, int dstStation) {
        logger.error("TPS Exceeded returning FALSE");
        return new Output(srcStation, dstStation, Utility.FALSE);
    }

    public Output arrAndDepSame(int srcStation, int dstStation) {
        logger.error("Input Error same stationId for arrival and departure");
        return new Output(srcStation, dstStation, Utility.FALSE);
    }

    public Output wrongIpnput(String srcStation, String dstStation) {
        logger.error("Worng Input:They are expected to be int but received as; dep {} arr {} ",
                     srcStation,
                     dstStation);
        return new Output(Utility.NEGATIVE_ONE, Utility.NEGATIVE_ONE, Utility.FALSE);
    }

    /**
     * reading stations from file
     * first line is read before coming here thus skipping it
     * try to read file in parallel
     * @param stream
     */
    private void readStations(Stream<String> stream) {
        if (stream.isParallel()) {
            if (logger.isDebugEnabled()) {
                logger.debug(" file will be read in pararllel");
            }
            ForkJoinPool forkJoinPool = new ForkJoinPool(Utility.FILE_READER_THREAD_COUNT);
            try {
                forkJoinPool.submit(() -> {
                    stream.forEach(consumer);
                }).join();
            } catch (Exception e) {
                logger.error("Error while processing file", e);
            }
            forkJoinPool.shutdown();
        } else {
            stream.forEach(consumer);
        }
        workerCount = getWorkerCount();
        logger.info("file loaded with {} routes, {} thread/s will be used for each search req.",
                    routeCounter.get(),
                    workerCount);
    }

    Consumer<String> consumer = (String line) -> {
        if (line != null) {
            String[] temp = line.trim().split(Utility.DELIMETER);
            if (temp != null) {
                if (temp.length == Utility.ONE) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("ignoring first LINE {} ", line);
                    }
                } else {
                    // at the moment I do not need to keep routeId due to no requirement
                    // if required routeIndx should be mapped to routeId
                    int routeIndx = routeCounter.getAndIncrement();
                    if (logger.isTraceEnabled()) {
                        logger.trace("will process route {} with line {}", routeIndx, line.trim());
                    }
                    routesWithStations[routeIndx] = (Arrays.stream(temp,
                                                                   Utility.ONE,
                                                                   temp.length).mapToInt((String s) -> Integer.parseInt(s)).toArray());
                    // I assumed that all lines are bi-directional 
                    // thus I sorted the stationId list in the route line
                    Arrays.sort(routesWithStations[routeIndx]);
                }
            } else {
                logger.warn("Line can not be splitted");
            }
        } else {
            logger.warn("Empty Line");
        }
    };
}

/*
 * JSON Output that is returned to REST API
 */
class Output {

    int     dep_sid          = Utility.NEGATIVE_ONE;
    int     arr_sid          = Utility.NEGATIVE_ONE;
    boolean direct_bus_route = Utility.FALSE;

    public Output(int src, int dst, boolean rs) {
        this.dep_sid = src;
        this.arr_sid = dst;
        this.direct_bus_route = rs;
    }

    public int getDep_sid() {
        return dep_sid;
    }

    public int getArr_sid() {
        return arr_sid;
    }

    public boolean isDirect_bus_route() {
        return direct_bus_route;
    }

}
