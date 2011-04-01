package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

/**
 *
 * @author justin
 */
public class AlertAccessor {
    public AlertAccessor(EntityStore store) throws DatabaseException {
        snortAlertById = store.getPrimaryIndex(Long.class, SnortAlert.class);
        modSecurityAlertById = store.getPrimaryIndex(Long.class, ModSecurityAlert.class);
        snortAlertsByDate = store.getSecondaryIndex(snortAlertById, Long.class, "date");
        modSecurityAlertsByDate = store.getSecondaryIndex(modSecurityAlertById, Long.class, "date");
    }

    public PrimaryIndex<Long,SnortAlert> snortAlertById;
    public PrimaryIndex<Long,ModSecurityAlert> modSecurityAlertById;
    public SecondaryIndex<Long, Long, SnortAlert> snortAlertsByDate;
    public SecondaryIndex<Long, Long, ModSecurityAlert> modSecurityAlertsByDate;
}
