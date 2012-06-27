/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.authentication;

import java.io.IOException;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.*;
import name.justinthomas.flower.analysis.persistence.ManagedNetworkManager;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author justin
 */
@PersistenceContext(name = "persistence/Analysis", unitName = "AnalysisPU")
public class DirectoryDomainManager {

    private static final Logger log = Logger.getLogger(DirectoryDomainManager.class.getName());
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

    public DirectoryDomainManager(Customer customer) { 
        this.customer = customer;
        this.globalConfigurationManager = DirectoryDomainManager.getGlobalConfigurationManager();
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
    
    public Boolean removeGroup(String domain, String group) {
        DirectoryDomain directoryDomain = this.getDirectoryDomain(domain);
        directoryDomain.getGroups().remove(group);
        this.updateDirectoryDomain(directoryDomain);
        return true;
    }
    
    public Boolean updateDirectoryDomain(DirectoryDomain directoryDomain) {
        log.debug("Updating DirectoryDomain: " + directoryDomain.toString());
        
        if(directoryDomain.getId() == null) {
            directoryDomain = this.getDirectoryDomain(directoryDomain.getDomain());
        }
        
        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            utx.begin();
            em.merge(directoryDomain);
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
    public Boolean addDirectoryDomain(String domain, String group, Boolean privileged) {
        DirectoryDomain directoryDomain = new DirectoryDomain(domain, group, privileged);
        directoryDomain.setAccountId(customer.getAccount());
        
        System.out.println("Adding DirectoryDomain: " + directoryDomain.toString());
        
        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            utx.begin();
            em.persist(directoryDomain);
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

    public DirectoryDomain getDirectoryDomain(String domain) {
        System.out.println("Getting directory domain " + domain + " for: " + customer.getAccount());
        
        List directoryDomains = (List) em.createQuery(
                "SELECT s FROM DirectoryDomain s WHERE s.accountId LIKE :accountid AND s.domain LIKE :domain")
                    .setParameter("accountid", customer.getAccount())
                    .setParameter("domain", domain)
                    .getResultList();
        
        if(directoryDomains.size() > 1) {
            log.error("DirectoryDomainManager: too many results");
        } else if(!directoryDomains.isEmpty()) {
            return (DirectoryDomain) directoryDomains.get(0);
        }
        
        return null;
    }

    public List<DirectoryDomain> getDirectoryDomains() {
        System.out.println("Getting directory domains for: " + customer.getAccount());
 
        return (List) em.createQuery(
                "SELECT s FROM DirectoryDomain s WHERE s.accountId LIKE :accountid")
                    .setParameter("accountid", customer.getAccount())
                    .getResultList();
    }
}
