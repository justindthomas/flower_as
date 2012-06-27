package name.justinthomas.flower.analysis.authentication;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.List;
import java.util.Random;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.*;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author justin
 */
@PersistenceContext(name = "persistence/Analysis", unitName = "AnalysisPU")
public final class UserManager {

    private static final Logger log = Logger.getLogger(UserManager.class.getName());
    private static FileAppender fileAppender;
    private Customer customer;
    private GlobalConfigurationManager globalConfigurationManager;
    private EntityManager em;

    public static EntityManager getEntityManager() {
        try {
            return (EntityManager) InitialContext.doLookup("java:comp/env/persistence/Analysis");
        } catch (NamingException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    public UserManager(Customer customer) {
        this.customer = customer;
        this.em = UserManager.getEntityManager();

        if (this.globalConfigurationManager == null) {
            try {
                this.globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
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
        
        if(this.getUsers().isEmpty()) {
            this.createFirstUser();
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
            updateUser(new FlowerUser(customer.getAccount(), "flower", new String(hash.digest()), "Administrator", true, "PST"));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public Boolean deleteUser(String userName) {
        FlowerUser user = this.getUser(userName);
        Boolean deleted;
        
        try {
            em.remove(user);
            deleted = true;
        } catch (Exception e) {
            e.printStackTrace();
            deleted = false;
        }
        
        return deleted;
    }

    public FlowerUser getUser(String userName) {
        System.out.println("Retrieving user: " + userName + " from storage.");
        FlowerUser user = null;

        List<FlowerUser> users = (List<FlowerUser>) em.createQuery(
                "SELECT u FROM FlowerUser u WHERE u.username LIKE :username").setParameter("username", userName).getResultList();

        if (users.size() > 1) {
            System.err.println("UserManager: too many results");
        } else if (!users.isEmpty()) {
            user = users.get(0);
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
            updateUser(new FlowerUser(customer.getAccount(), user, new String(hash.digest()), fullName, administrator, timeZone));
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    private void updateUser(FlowerUser user) {
        System.out.println("Persisting " + user.getUsername() + " to long-term storage.");

        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            if (this.getUser(user.getUsername()) != null) {
                FlowerUser stored = this.getUser(user.getUsername());
                user.setId(stored.getId());
                utx.begin();
                em.merge(stored);
                utx.commit();
            } else {
                utx.begin();
                em.persist(user);
                utx.commit();
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } catch (NotSupportedException nse) {
            nse.printStackTrace();
        } catch (RollbackException rbe) {
            rbe.printStackTrace();
        } catch (HeuristicMixedException hme) {
            hme.printStackTrace();
        } catch (HeuristicRollbackException hrbe) {
            hrbe.printStackTrace();
        } catch (SystemException se) {
            se.printStackTrace();
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
    }

    public List<FlowerUser> getUsers() {
        System.out.println("Retrieving users from storage.");

        List<FlowerUser> users = (List<FlowerUser>) em.createQuery("SELECT u FROM FlowerUser u").getResultList();

        return users;
    }
}
