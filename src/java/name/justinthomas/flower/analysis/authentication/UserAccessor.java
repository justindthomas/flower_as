/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.authentication;

import name.justinthomas.flower.analysis.authentication.User;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 *
 * @author justin
 */
public class UserAccessor {
    public UserAccessor(EntityStore store) throws DatabaseException {
        userById = store.getPrimaryIndex(String.class, User.class);
    }

    public PrimaryIndex<String,User> userById;
}
