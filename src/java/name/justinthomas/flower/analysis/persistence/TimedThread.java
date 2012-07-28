package name.justinthomas.flower.analysis.persistence;

import java.util.Date;

/**
 *
 * @author justin
 */
public class TimedThread extends Thread {

    private long start;
    private static final long TIMEOUT = 600000;

    public TimedThread(Runnable runnable) {
        super(runnable);
        start = new Date().getTime();
    }

    public long delta() {
        return new Date().getTime() - start;
    }

    public Boolean isExpired() {
        if(delta() > TIMEOUT) return true;
        else return false;
    }
}
