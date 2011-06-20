/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.accounting;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import name.justinthomas.flower.analysis.services.Utility;
import name.justinthomas.flower.manager.services.CustomerAdministration.Accounting;

/**
 *
 * @author justin
 */
@Singleton
@Startup
public class AccountingManager {
    // <account ID, flows received>

    private static Queue<Accounting> queue = new ConcurrentLinkedQueue();
    private static ScheduledThreadPoolExecutor executor;

    public AccountingManager() {
        executor = new ScheduledThreadPoolExecutor(3);
        executor.scheduleAtFixedRate(new Charge(), 60, 60, TimeUnit.SECONDS);
    }

    public void addFlows(String account, String sender, Integer count) {
        Accounting accounting = new Accounting();
        accounting.setCustomer(account);
        accounting.setSender(sender);
        accounting.setCount(count);
        queue.add(accounting);
    }

    class Charge implements Runnable {

        @Override
        public void run() {
            List<Accounting> accountings = new ArrayList();

            while (!queue.isEmpty() && (accountings.size() < 100)) {
                accountings.add(AccountingManager.queue.remove());
            }
            
            Utility.chargeCustomers(accountings);
        }
    }
}
