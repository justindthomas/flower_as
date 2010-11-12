package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 *
 * @author justin
 */
public class AlertAccessor {
    public AlertAccessor(EntityStore store) throws DatabaseException {
        alertById = store.getPrimaryIndex(Long.class, PersistentAlert.class);
    }

    public PrimaryIndex<Long,PersistentAlert> alertById;

}
