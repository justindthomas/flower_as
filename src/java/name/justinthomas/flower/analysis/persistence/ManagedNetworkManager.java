/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.global.GlobalConfigurationManager;
import java.io.IOException;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.*;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author justin
 */
@PersistenceContext(name = "persistence/Analysis", unitName = "AnalysisPU")
public class ManagedNetworkManager {

    private static final Logger log = Logger.getLogger(ManagedNetworkManager.class.getName());
    private static FileAppender fileAppender;
    private Customer customer;
    private GlobalConfigurationManager globalConfigurationManager;
    private EntityManager em;

    private static GlobalConfigurationManager getGlobalConfigurationManager() {
        try {
            return (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
        } catch (NamingException e) {
            log.error(e.getMessage());
        }

        return null;
    }
    
    public static EntityManager getEntityManager() {
        try {
            return (EntityManager) InitialContext.doLookup("java:comp/env/persistence/Analysis");
        } catch (NamingException e) {
            log.error(e.getMessage());
        }

        return null;
    }
    
    public ManagedNetworkManager(Customer customer) {
        this.customer = customer;
        this.globalConfigurationManager = ManagedNetworkManager.getGlobalConfigurationManager();
        this.em = ManagedNetworkManager.getEntityManager();
        
        if (fileAppender == null) {
            try {
                String pattern = "%d{dd MMM yyyy HH:mm:ss.SSS} - %p - %m %n";
                PatternLayout layout = new PatternLayout(pattern);
                fileAppender = new FileAppender(layout, globalConfigurationManager.getBaseDirectory() + "/statistics.log");
                log.addAppender(fileAppender);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
    
    public List<ManagedNetwork> getManagedNetworks() {
        System.out.println("Getting managed networks for: " + customer.getAccount());
 
        return (List) em.createQuery(
                "SELECT s FROM ManagedNetwork s WHERE s.accountId LIKE :accountid")
                    .setParameter("accountid", customer.getAccount())
                    .getResultList();
    }

    public ManagedNetwork getManagedNetwork(String address) {
        System.out.println("Getting managed network " + address + " for: " + customer.getAccount());
        
        List managedNetworks = (List) em.createQuery(
                "SELECT s FROM ManagedNetwork s WHERE s.accountId LIKE :accountid AND s.address LIKE :address")
                    .setParameter("accountid", customer.getAccount())
                    .setParameter("address", address)
                    .getResultList();
        
        if(managedNetworks.size() > 1) {
            log.error("ManagedNetworkManager: too many results");
        } else if(!managedNetworks.isEmpty()) {
            return (ManagedNetwork) managedNetworks.get(0);
        }
        
        return null;
    }
    
    public Boolean addManagedNetwork(ManagedNetwork network) {
        network.setAccountId(customer.getAccount());
        System.out.println("Adding ManagedNetwork: " + network.toString());
        
        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            utx.begin();
            em.persist(network);
            utx.commit();
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
        
        return true;
    }

    public Boolean deleteManagedNetwork(String address) {
        ManagedNetwork managedNetwork = this.getManagedNetwork(address);
        em.remove(managedNetwork);
        return true;
    }
}
