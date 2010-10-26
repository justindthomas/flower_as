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
public class ConfigurationAccessor {
    public ConfigurationAccessor(EntityStore store) throws DatabaseException {
        configurationById = store.getPrimaryIndex(Long.class, PersistentConfiguration.class);
        defaultConfiguration = store.getSecondaryIndex(configurationById, Boolean.class, "selected");
    }

    public PrimaryIndex<Long,PersistentConfiguration> configurationById;
    public SecondaryIndex<Boolean, Long, PersistentConfiguration> defaultConfiguration;
}
