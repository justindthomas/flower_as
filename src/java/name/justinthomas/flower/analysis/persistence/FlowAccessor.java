/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 *
 * @author justin
 */
public class FlowAccessor {

    public FlowAccessor(EntityStore store) throws DatabaseException {
        flowsBySecond = store.getPrimaryIndex(Long.class, PersistentSecond.class);
    }

    public PrimaryIndex<Long,PersistentSecond> flowsBySecond;
}
