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
@Entity(version = 102242010)
public class PersistentSecond {

    @PrimaryKey()
    Long second;
    LinkedList<PersistentFlow> flows = new LinkedList();

    protected PersistentSecond() {
    }

    public PersistentSecond(Long second) {
        this.second = second;
    }

    public void addFlow(PersistentFlow flow) {
        flows.add(flow);
    }

    public LinkedList<PersistentFlow> getFlows() {
        return flows;
    }
}
