/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author justin
 */


@Singleton
@Startup
public class ThreadManager {

    private Map<String, ArrayList<TimedThread>> threads;
    private ScheduledThreadPoolExecutor pool;

    @PostConstruct
    protected void init() {
       threads = new HashMap<String, ArrayList<TimedThread>>();

       //System.out.println("Starting thread watcher...");
       pool = new ScheduledThreadPoolExecutor(1);
       ThreadWatcher threadWatcher = new ThreadWatcher();
       pool.scheduleWithFixedDelay(threadWatcher, 1, 3, TimeUnit.MINUTES);
    }

    @Lock(LockType.WRITE)
    public void start(String user, TimedThread thread) {
        //System.out.println("Getting threads for user: " + user);
        ArrayList<TimedThread> threadList = threads.get(user);
        
        if (threadList == null) {
            //System.out.println("Beginning tracking of threads for: " + user);
            threads.put(user, threadList = new ArrayList<TimedThread>());
        }

        threadList.add(thread);
        threads.put(user, threadList);

        //System.out.println("Starting analysis thread: " + thread.getName());
        thread.start();
        //System.out.println("Number of threads for " + user + ": " + threads.get(user).size());
    }

    @Lock(LockType.WRITE)
    public Map<String, ArrayList<TimedThread>> getThreads() {
        return threads;
    }

    public void closeEnvironment(String threadName) {
        for(ArrayList<TimedThread> threadList : threads.values()) {
            Boolean found = false;
            for(TimedThread thread : threadList) {
                if(thread.getName().equals(threadName)) {
                    found = true;
                }

                if(found) {
                    break;
                }
            }
        }
    }
}
