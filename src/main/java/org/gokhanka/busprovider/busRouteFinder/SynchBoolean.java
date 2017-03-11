package org.gokhanka.busprovider.busRouteFinder;

public class SynchBoolean {

    private Object               mutex = new Object();
    private boolean              found = Utility.FALSE;

    public boolean isFound() {
        boolean result = Utility.FALSE;
        synchronized (mutex) {
            result = this.found;
        }
        return result;
    }

    public void setFound() {
        if (!this.found) {
            synchronized (mutex) {
                this.found = Utility.TRUE;
            }
        }
    }
}
