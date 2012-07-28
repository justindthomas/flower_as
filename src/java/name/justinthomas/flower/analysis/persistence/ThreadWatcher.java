package name.justinthomas.flower.analysis.persistence;

import java.util.ArrayList;
import java.util.Map.Entry;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author justin
 */
public class ThreadWatcher implements Runnable {

    @Override
    public void run() {
        //System.out.println("Checking in on threads...");

        ThreadManager threadManager = null;

        //System.out.println("Getting ThreadManager EJB...");
        try {
            Context context = new InitialContext();
            threadManager = (ThreadManager) context.lookup("java:global/Analysis/ThreadManager");
        } catch (NamingException ne) {
            ne.printStackTrace();
        }

        //System.out.println("Iterating over all threads");
        for(Entry<String, ArrayList<TimedThread>> threadList : threadManager.getThreads().entrySet()) {
            //System.out.println("Evaluating threads for user: " + threadList.getKey());

            ArrayList<TimedThread> remove = new ArrayList<TimedThread>();
            for(TimedThread thread : threadList.getValue()) {
                //System.out.println("thread: " + tt.getName() + ", delta: " + tt.delta());

                if (!thread.isAlive()) {
                    remove.add(thread);
                } else {
                    if (thread.isExpired()) {
                        if (!thread.isInterrupted()) {
                            System.err.println("Interrupting expired thread: " + thread.getName());
                            thread.interrupt();
                        }
                    }
                }
            }

            //System.out.println("Performing thread removals...");
            int i = 0;
            for(TimedThread thread : remove) {
                i++;
                threadManager.getThreads().get(threadList.getKey()).remove(thread);
            }
            //System.out.println("Completed " + i + " removals for " + remove.size() + " threads");
        }
        //System.out.println("Completed iteration");
    }
}

