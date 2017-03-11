package org.gokhanka.busprovider.busRouteFinder;

import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorkerThreadForSearch implements Runnable {

    private static final Logger logger                  = LogManager.getLogger();
    private int[][]             routesWithStations      = null;
    private SynchBoolean        needToContinueSearching = null;
    private int                 src                     = 0;
    private int                 dst                     = 0;
    private CountDownLatch      cl                      = null;
    private int                 startingPoint           = 0;
    private int                 endingPoint             = 0;
    private String              name                    = null;

    public WorkerThreadForSearch(String name, SynchBoolean needToContinueSearching, int[][] routes,
                                 int src, int dst, CountDownLatch cl, int iterationStartIdx,
                                 int iterationStopIdx) {
        this.needToContinueSearching = needToContinueSearching;
        this.routesWithStations = routes;
        this.src = src;
        this.dst = dst;
        this.cl = cl;
        this.startingPoint = iterationStartIdx;
        this.endingPoint = iterationStopIdx;
        this.name = name;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(name);
        try {
            if (logger.isTraceEnabled())
                logger.trace("Start.");
            if (!needToContinueSearching.isFound()) {
                processJob();
            }
            if (logger.isTraceEnabled())
                logger.trace("End.");
        } finally {
            try {
                this.cl.countDown();
            } catch (Exception e) {
            }
        }
    }

    private void processJob() {
        boolean myResult = Utility.FALSE;
        for (int routeIndx = this.startingPoint; routeIndx < this.endingPoint; routeIndx++) {
            if (needToContinueSearching.isFound()) {
                break;
            } else {
                if (logger.isTraceEnabled())
                    logger.trace("will evalutae routeIndex {}", routeIndx);
                if (Utility.ifBothExist(routesWithStations[routeIndx], src, dst)) {
                    myResult = Utility.TRUE;
                    needToContinueSearching.setFound();
                    break;
                }
            }
        }
        if (logger.isDebugEnabled())
            logger.debug("route exist in {} - > {} {}", src, dst, myResult);
    }

    @Override
    public String toString() {
        return Thread.currentThread().getName();
    }
}
