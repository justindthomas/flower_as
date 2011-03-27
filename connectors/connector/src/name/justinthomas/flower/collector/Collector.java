package name.justinthomas.flower.collector;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author justin
 */
public class Collector {
    private static Listener listener;
    private static ScheduledThreadPoolExecutor executor;

    public void init() {
        listener = new Listener();
        listener.listen();

        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleWithFixedDelay(new QueueManager(), 30, 30, TimeUnit.SECONDS);
    }
}
