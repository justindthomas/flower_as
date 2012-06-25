package name.justinthomas.flower.analysis.persistence;

import java.util.LinkedList;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.*;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

/**
 *
 * @author justin
 */
public class FlowReceiver {

    private Customer customer;
    private static GlobalConfigurationManager globalConfigurationManager;
    private EntityManager em;
    private UserTransaction utx;

    public FlowReceiver(EntityManager em, UserTransaction utx, Customer customer) {
        this.customer = customer;
        this.em = em;
        this.utx = utx;

        if (globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                System.err.println("Error retrieving GlobalConfigurationManager in FlowReceiver: " + e.getMessage());
            }
        }
    }

    public LinkedList<Long> addFlows(String sender, LinkedList<Flow> flows) {
        return this.addFlows(sender, flows, null);
    }

    private void addFlow(PersistentFlow flow) {
        try {
            utx.begin();
            em.persist(flow);
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
        }
    }

    public LinkedList<Long> addFlows(String sender, LinkedList<Flow> flows, HttpServletRequest request) {
        LinkedList<Long> ids = new LinkedList();

        try {
            for (Flow flow : flows) {
                if ((flow.protocol == 6) || (flow.protocol == 17)) {
                    globalConfigurationManager.addFrequency(customer, flow.protocol, flow.ports);
                }

                PersistentFlow pflow = flow.toHashTableFlow();
                this.addFlow(pflow);

                ids.add(pflow.getId());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return ids;
    }
}
