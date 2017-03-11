package org.gokhanka.busprovider.busRouteFinder;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
/**
 * Delay interface is implemented in order to use this class
 * in TPS counting with delayed queue
 * @author gokhanka
 *
 */
class DelayedObject implements Delayed {

    long creationTime;
    long duration;

    public DelayedObject(long dur) {
        this.creationTime = System.currentTimeMillis();
        this.duration = dur;
    }

    @Override
    public int compareTo(Delayed o) {
        DelayedObject delayedObject = (DelayedObject) o;
        if (creationTime > delayedObject.creationTime) {
            return 1;
        } else if (creationTime == delayedObject.creationTime) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long timeElapsed = System.currentTimeMillis() - creationTime;
        long timeRemaining = duration - timeElapsed;
        return unit.convert(timeRemaining, TimeUnit.MILLISECONDS);
    }

}