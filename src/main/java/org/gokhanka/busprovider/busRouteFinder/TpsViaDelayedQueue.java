package org.gokhanka.busprovider.busRouteFinder;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
/**
 * A TPS counter implementation using DelayedObjects in DelayQueue in JDK
 * @author gokhanka
 *
 */
public class TpsViaDelayedQueue {

    private static final Logger logger   = LogManager.getLogger();
    private Object              callLock = null;

    private int                 maxTransactionCount;

    public int getMaxTransactionCount() {
        return maxTransactionCount;
    }

    private long                      delayPeriod = 1000;

    private DelayQueue<DelayedObject> permitsQueue;
    /**
     * Constructor :
     * maximum Transaction  that will be allowed for the defined period of Time
     * @param maxTransactionCount
     * @param period
     */
    public TpsViaDelayedQueue(int maxTransactionCount, long period) {
        if (maxTransactionCount <= 0) {
            throw new IllegalArgumentException("maxTransactionCount must be greater than 0");
        }
        this.maxTransactionCount = maxTransactionCount;
        if (period > 1000)
            this.delayPeriod = period;
        callLock = new Object();
        permitsQueue = new DelayQueue<DelayedObject>();
        for (int i = 0; i < this.maxTransactionCount; i++) {
            permitsQueue.add(new DelayedObject(this.delayPeriod));
        }
        try {
            Thread.sleep(delayPeriod);
        } catch (Exception e) {

        }
    }

    @Override
    public String toString() {
        return "ThrottlabeInfo [maxConcurrentCallCount=" + this.maxTransactionCount + " currentTPS="
                + getCurrentCallCount() + "]";
    }
    /**
     * to see if there is space for new transaction in the queue
     * Blocking method so use it responsibly
     * @return
     */
    public boolean isTpsAvailable() {
        boolean result = Utility.FALSE;
        synchronized (callLock) {
            try {
                DelayedObject del = permitsQueue.poll();
                if (del != null) {
                    result = Utility.TRUE;
                    permitsQueue.add(new DelayedObject(this.delayPeriod));
                }
            } catch (Exception e) {
                logger.fatal("Houston we have a problem! : ", e);
                result = Utility.FALSE;
            }
        }
        return result;
    }
    /**
     * more safe way to id there is space for new transaction. Method waits for time given in 
     * millis and then return if there is no space at the time of request
     * @param millis
     * @return
     */
    public boolean isTpsAvailable(long millis) {
        boolean result = Utility.FALSE;
        synchronized (callLock) {
            try {
                DelayedObject del = permitsQueue.poll(millis, TimeUnit.MILLISECONDS);
                if (del != null) {
                    result = Utility.TRUE;
                    permitsQueue.add(new DelayedObject(this.delayPeriod));
                }
            } catch (Exception e) {
                logger.fatal("Houston we have a problem! : ", e);
                result = Utility.FALSE;
            }
        }
        return result;
    }
    /**
     * Wait till there is space for new transaction...
     * @return
     */
    public boolean getTpsWhenAvailable() {
        boolean result = Utility.FALSE;
        synchronized (callLock) {
            try {
                DelayedObject del = permitsQueue.take();
                if (del != null) {
                    result = Utility.TRUE;
                    permitsQueue.add(new DelayedObject(this.delayPeriod));
                }
            } catch (Exception e) {
                logger.fatal("Houston we have a problem! : ", e);
                result = Utility.FALSE;
            }
        }
        return result;
    }

    public int getCurrentCallCount() {
        int rsult = 0;
        synchronized (callLock) {
            Object[] obj = this.permitsQueue.toArray();
            for (int i = 0; i < obj.length; i++) {
                if (((DelayedObject) obj[i]).getDelay(TimeUnit.MILLISECONDS) <= delayPeriod) {
                    rsult++;
                }
            }
        }
        return rsult;
    }
}
