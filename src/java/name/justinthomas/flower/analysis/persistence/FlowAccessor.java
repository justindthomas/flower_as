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
        flowById = store.getPrimaryIndex(Long.class, PersistentFlow.class);
    }

    public PrimaryIndex<Long,PersistentFlow> flowById;
}
