/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.analysis.persistence.PersistentUser;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 *
 * @author justin
 */
public class UserAccessor {
    public UserAccessor(EntityStore store) throws DatabaseException {
        userById = store.getPrimaryIndex(String.class, PersistentUser.class);
    }

    public PrimaryIndex<String,PersistentUser> userById;
}
