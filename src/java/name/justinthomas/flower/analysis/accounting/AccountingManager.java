/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.accounting;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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
        queue.add(new Accounting(sender, count));
    }
    
    class Charge implements Runnable {
        
        @Override
        public void run() {
             for(Accounting accounting : queue) {
                 AccountingManager.queue.remove(accounting);
             }
        }
    }
    
    static class Accounting {
        private String sender;
        private Date date;
        private Integer count;
        
        public Accounting(String sender, Integer count) {
            date = new Date();
            this.sender = sender;
            this.count = count;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Accounting other = (Accounting) obj;
            if ((this.sender == null) ? (other.sender != null) : !this.sender.equals(other.sender)) {
                return false;
            }
            if (this.date != other.date && (this.date == null || !this.date.equals(other.date))) {
                return false;
            }
            if (this.count != other.count && (this.count == null || !this.count.equals(other.count))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + (this.sender != null ? this.sender.hashCode() : 0);
            hash = 97 * hash + (this.date != null ? this.date.hashCode() : 0);
            hash = 97 * hash + (this.count != null ? this.count.hashCode() : 0);
            return hash;
        }
    }
}
