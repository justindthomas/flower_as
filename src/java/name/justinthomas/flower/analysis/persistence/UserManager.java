package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.ForwardCursor;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.TimeZone;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author justin
 */
public class UserManager {

    private static Integer DEBUG = 1;
    private Environment environment;
    private static ConfigurationManager configurationManager;

    private void createFirstUser() {
        Security.addProvider(new BouncyCastleProvider());
        MessageDigest hash = null;

        try {
            hash = MessageDigest.getInstance("SHA-256", "BC");
            hash.update("flower".getBytes());
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (NoSuchProviderException nspe) {
            nspe.printStackTrace();
        }

        try {
            System.out.println("Creating first user (flower)");
            TimeZone PST = TimeZone.getTimeZone("PST");
            updateUser(new PersistentUser("flower", new String(hash.digest()), "Administrator", true, "PST"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean deleteUser(String userName) {
        setupEnvironment();
        Boolean deleted = false;
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "User", storeConfig);
            UserAccessor accessor = new UserAccessor(entityStore);

            deleted = accessor.userById.delete(userName);

            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in UserManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return deleted;
    }

    public PersistentUser getUser(String userName) {
        setupEnvironment();
        PersistentUser user = null;

        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "User", storeConfig);
            UserAccessor accessor = new UserAccessor(entityStore);

            user = accessor.userById.get(userName);

            entityStore.close();
        } catch (Throwable t) {
            System.err.println("Exception in UserManager: " + t.getMessage());
        } finally {
            closeEnvironment();
        }

        return user;
    }

    public Boolean updateUser(String user, String password, String fullName, Boolean administrator, String timeZone) {

        Security.addProvider(new BouncyCastleProvider());
        MessageDigest hash = null;

        try {
            hash = MessageDigest.getInstance("SHA-256", "BC");
            hash.update(password.getBytes());
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (NoSuchProviderException nspe) {
            nspe.printStackTrace();
        }

        try {
            updateUser(new PersistentUser(user, new String(hash.digest()), fullName, administrator, timeZone));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void updateUser(PersistentUser user) {
        System.out.println("Updating User: " + user.username);

        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "User", storeConfig);
            UserAccessor accessor = new UserAccessor(entityStore);

            accessor.userById.put(user);
            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in UserManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }
    }

    public ArrayList<PersistentUser> getUsers() {
        setupEnvironment();
        ArrayList<PersistentUser> users = new ArrayList<PersistentUser>();

        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "User", storeConfig);
            UserAccessor accessor = new UserAccessor(entityStore);

            ForwardCursor<PersistentUser> cursor = accessor.userById.entities();

            for(PersistentUser user : cursor) {
                users.add(user);
            }

            cursor.close();
            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in UserManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return users;
    }

    private void setupEnvironment() {
        if(configurationManager == null) {
            try {
                configurationManager = (ConfigurationManager) InitialContext.doLookup("java:global/Analysis/ConfigurationManager");
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }
        
        File environmentHome = new File(configurationManager.getBaseDirectory() + "/" + configurationManager.getUserDirectory());

        if (!environmentHome.exists()) {
            if (environmentHome.mkdirs()) {
                System.out.println("Created user directory: " + environmentHome);
                createFirstUser();
            } else {
                System.err.println("User directory '" + environmentHome + "' does not exist and could not be created (permissions?)");
            }
        }

        EnvironmentConfig environmentConfig = new EnvironmentConfig();
        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);
        //environmentConfig.setConfigParam(EnvironmentConfig.CLEANER_EXPUNGE, "false");
        environment = new Environment(environmentHome, environmentConfig);
    }

    private void closeEnvironment() {
        if (environment != null) {
            try {
                environment.close();
            } catch (DatabaseException e) {
                System.err.println("Error closing environment: " + e.toString());
            }
        }
    }
}
