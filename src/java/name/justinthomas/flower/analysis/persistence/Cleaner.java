package name.justinthomas.flower.analysis.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.Customer;
import name.justinthomas.flower.manager.services.CustomerAdministration;
import name.justinthomas.flower.manager.services.CustomerAdministrationService;

/**
 *
 * @author justin
 */
@Singleton
@Startup
public class Cleaner implements Runnable {

    @EJB
    GlobalConfigurationManager globalConfigurationManager;
    private static Cleaner instance;
    private static ScheduledThreadPoolExecutor executor;

    @PostConstruct
    protected void setup() {
        System.out.println("Setting Cleaner to run in 3 minutes and every 4 hours.");

        instance = new Cleaner();
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(instance, 3, 240, TimeUnit.MINUTES);
    }
    private static Integer DEBUG = 1;

    private void clean() {
        Collection<String> customers;

        synchronized (customers = globalConfigurationManager.getCachedStatisticsMap().keySet()) {
            System.out.println("Cleaning statistics...");
            Customer customer = null;



            for (String customerID : customers) {
                try {
                    CustomerAdministrationService admin = new CustomerAdministrationService(new URL(globalConfigurationManager.getManager() + "/CustomerAdministrationService?wsdl"));

                    CustomerAdministration port = admin.getCustomerAdministrationPort();
                    customer = port.getCustomer(null, null, customerID);
                } catch (MalformedURLException e) {
                    System.err.println("Could not access Customer Administration service at: " + globalConfigurationManager.getManager());
                    break;
                }
                
                StatisticsManager statisticsManager = new StatisticsManager(customer);
                ArrayList<Long> flowIDs = statisticsManager.cleanStatisticalIntervals();
                System.out.println("Cleaning flows...");
                FlowManager flowManager = new FlowManager();
                flowManager.cleanFlows(flowIDs);
            }
        }



        if (DEBUG >= 1) {
            System.out.println("Cleaning completed.");
        }
    }

    @Override
    public void run() {
        clean();
    }
}
