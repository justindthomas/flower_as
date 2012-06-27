package name.justinthomas.flower.analysis.persistence;

import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.*;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

@PersistenceContext(name = "persistence/Analysis", unitName = "AnalysisPU")
public class AlertManager {

    private Customer customer;
    private GlobalConfigurationManager globalConfigurationManager;
    private EntityManager em;
    
    public AlertManager(Customer customer) {
        this.customer = customer;
        
        try {
            this.globalConfigurationManager = InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
        } catch (NamingException e) {
            e.printStackTrace();
        }
        
        try {
            this.em = (EntityManager) InitialContext.doLookup("java:comp/env/persistence/Analysis");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void addAlert(SnortAlert alert) {
        System.out.println("Adding alert: " + alert.toString());
        
        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            utx.begin();
            em.persist(alert);
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
    }
    
    public <T> T getAlert(Class<T> type, Long id) {
        return em.find(type, id);
    }

    public void addAlert(ModSecurityAlert alert) {
        System.out.println("Adding alert: " + alert.toString());
        
        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            utx.begin();
            em.persist(alert);
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
    }

    public Boolean deleteAlert(Class type, Long alertId) {
        System.out.println("Deleting alert: " + alertId);
        
        Object alert = em.find(type, alertId);
        alert = type.cast(alert);
        
        try {
            Context context = new InitialContext();
            UserTransaction utx = (UserTransaction) context.lookup("java:comp/UserTransaction");

            utx.begin();
            em.remove(alert);
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

    public List getAlerts(Class type, Constraints constraints) {
        System.out.println("Getting alerts from: " + constraints.startTime + " to: " + constraints.endTime);

        Long start = constraints.startTime.getTime() / 1000;
        Long end = constraints.endTime.getTime() / 1000;
 
        return (List) em.createQuery(
                "SELECT s FROM " + type.getName() + " s WHERE s.accountId LIKE :accountid AND s.date >= :start AND s.date <= :end")
                    .setParameter("accountid", customer.getAccount())
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getResultList();
    }
}
