/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

/**
 *
 * @author justin
 */
@Entity
public class PersistentFrequency {
    @PrimaryKey
    String protocol_port = null;
    @SecondaryKey(relate=Relationship.MANY_TO_ONE)
    public Integer frequency = 0;

    public PersistentFrequency() {
        
    }

    public PersistentFrequency(String protocol_port, Integer frequency) {
        this.protocol_port = protocol_port;
        this.frequency = frequency;
    }

    public PersistentFrequency increment() {
        frequency++;
        return this;
    }
}
