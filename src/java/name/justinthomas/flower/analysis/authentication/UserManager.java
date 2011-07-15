package name.justinthomas.flower.analysis.authentication;

import name.justinthomas.flower.global.GlobalConfigurationManager;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.ForwardCursor;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimeZone;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author justin
 */
public class UserManager {

    private static final Logger log = Logger.getLogger(UserManager.class.getName());
    private static FileAppender fileAppender;
    private Customer customer;
    private static Integer DEBUG = 1;
    private Environment environment;
    private static GlobalConfigurationManager globalConfigurationManager;

    public UserManager(Customer customer) {
        this.customer = customer;

        if (globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                log.error(e.getMessage());
            }
        }

        if (fileAppender == null) {
            try {
                fileAppender = new FileAppender(new SimpleLayout(), globalConfigurationManager.getBaseDirectory() + "/authentication.log");
                log.addAppender(fileAppender);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void createFirstUser() {
        Security.addProvider(new BouncyCastleProvider());
        MessageDigest hash = null;
        StringBuilder builder = new StringBuilder();
        
        try {
            Random r = new Random();
            
            for (int i = 0; i < 8; i++) {
                int c;
                c = r.nextInt(126);
                while (c < 33) {
                    c = r.nextInt(126);
                }
                builder.append((char) c);
            }

            hash = MessageDigest.getInstance("SHA-256", "BC");
            hash.update(builder.toString().getBytes());
        } catch (NoSuchAlgorithmException nsae) {
            log.error(nsae.getMessage());
        } catch (NoSuchProviderException nspe) {
            log.error(nspe.getMessage());
        }

        try {
            log.debug("Creating first user: flower, " + builder.toString());
            updateUser(new User("flower", new String(hash.digest()), "Administrator", true, "PST"));
        } catch (Exception e) {
            log.error(e.getMessage());
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
            log.error(e.getMessage());
        } finally {
            closeEnvironment();
        }

        return deleted;
    }

    public User getUser(String userName) {
        setupEnvironment();
        User user = null;

        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(false);
            storeConfig.setReadOnly(true);
            EntityStore entityStore = new EntityStore(environment, "User", storeConfig);
            UserAccessor accessor = new UserAccessor(entityStore);

            user = accessor.userById.get(userName);

            entityStore.close();
        } catch (Throwable t) {
            log.error(t.getMessage());
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
            log.error(nsae.getMessage());
        } catch (NoSuchProviderException nspe) {
            log.error(nspe.getMessage());
        }

        try {
            updateUser(new User(user, new String(hash.digest()), fullName, administrator, timeZone));
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    private void updateUser(User user) {
        log.debug("Updating User: " + user.username);

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
            log.error(e.getMessage());
        } finally {
            closeEnvironment();
        }
    }

    public ArrayList<User> getUsers() {
        setupEnvironment();
        ArrayList<User> users = new ArrayList<User>();

        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(false);
            storeConfig.setReadOnly(true);
            EntityStore entityStore = new EntityStore(environment, "User", storeConfig);
            UserAccessor accessor = new UserAccessor(entityStore);

            ForwardCursor<User> cursor = accessor.userById.entities();

            for (User user : cursor) {
                users.add(user);
            }

            cursor.close();
            entityStore.close();
        } catch (DatabaseException e) {
            log.error(e.getMessage());
        } finally {
            closeEnvironment();
        }

        return users;
    }

    private void setupEnvironment() {
        File environmentHome = new File(globalConfigurationManager.getBaseDirectory() + "/customers/" + customer.getDirectory() + "/users");

        if (!environmentHome.exists()) {
            if (environmentHome.mkdirs()) {
                log.debug("Created user directory: " + environmentHome);
                createFirstUser();
            } else {
                log.error("User directory '" + environmentHome + "' does not exist and could not be created (permissions?)");
            }
        }

        EnvironmentConfig environmentConfig = new EnvironmentConfig();
        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);
        environment = new Environment(environmentHome, environmentConfig);
    }

    private void closeEnvironment() {
        if (environment != null) {
            try {
                environment.close();
            } catch (DatabaseException e) {
                log.error(e.getMessage());
            }
        }
    }
}
