/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;

/**
 *
 * @author justin
 */
@Singleton
@Startup
@DependsOn("ConfigurationManager")
public class Cleaner implements Runnable {

    private static Cleaner instance;
    private static ScheduledThreadPoolExecutor executor;

    @PostConstruct
    public void setup() {
        System.out.println("Setting Cleaner to run in 3 minutes and every 4 hours.");

        instance = new Cleaner();
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(instance, 3, 240, TimeUnit.MINUTES);
    }

    private static Integer DEBUG = 1;

    private void clean() {
        if (DEBUG >= 1) {
            System.out.println("Cleaning databases...");
        }

        System.out.println("Cleaning flows...");
        FlowManager flowManager = FlowManager.getFlowManager();
        flowManager.cleanFlows();

        System.out.println("Cleaning statistics...");
        StatisticsManager statisticsManager = new StatisticsManager();
        statisticsManager.cleanStatisticalIntervals();

        if (DEBUG >= 1) {
            System.out.println("Cleaning completed.");
        }
    }

    @Override
    public void run() {
        clean();
    }
}
