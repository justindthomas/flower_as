/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.analysis.authentication.DirectoryDomain;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 *
 * @author justin
 */
public class CustomerConfigurationAccessor {
    public CustomerConfigurationAccessor(EntityStore store) throws DatabaseException {
        managedNetworkByAddress = store.getPrimaryIndex(String.class, ManagedNetwork.class);
        directoryDomainByName = store.getPrimaryIndex(String.class, DirectoryDomain.class);
    }

    public PrimaryIndex<String, ManagedNetwork> managedNetworkByAddress;
    public PrimaryIndex<String, DirectoryDomain> directoryDomainByName;
}
