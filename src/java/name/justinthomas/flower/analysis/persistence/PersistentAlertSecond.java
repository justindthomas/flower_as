/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.LinkedList;

/**
 *
 * @author justin
 */
@Entity
public class PersistentAlertSecond {
    @PrimaryKey
    public long second;

    public LinkedList<Long> alerts = new LinkedList();

    public PersistentAlertSecond() {
    }

    public LinkedList<Long> getAlerts() {
        return alerts;
    }

    public void setAlerts(LinkedList<Long> alerts) {
        this.alerts = alerts;
    }

    public long getSecond() {
        return second;
    }

    public void setSecond(long second) {
        this.second = second;
    }
}
