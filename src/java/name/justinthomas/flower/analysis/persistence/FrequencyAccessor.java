/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

/**
 *
 * @author justin
 */
public class FrequencyAccessor {

    public FrequencyAccessor(EntityStore store) throws DatabaseException {
        frequencyByPort = store.getPrimaryIndex(String.class, PersistentFrequency.class);
        frequencyByFrequency = store.getSecondaryIndex(frequencyByPort, Integer.class, "frequency");
    }

    public PrimaryIndex<String, PersistentFrequency> frequencyByPort;
    public SecondaryIndex<Integer, String, PersistentFrequency> frequencyByFrequency;
}
